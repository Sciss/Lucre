/*
 *  Txn.scala
 *  (Lucre)
 *
 *  Copyright (c) 2009-2017 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.lucre.stm

import java.io.Closeable

import de.sciss.lucre.event.ReactionMap
import de.sciss.serial.{DataInput, Serializer}

import scala.annotation.tailrec
import scala.concurrent.stm.Txn.ExternalDecider
import scala.concurrent.stm.{Txn => ScalaTxn, InTxnEnd, TxnLocal, TxnExecutor, InTxn}
import scala.language.higherKinds
import scala.util.control.NonFatal

object TxnLike {
  /** Implicitly extracts a Scala STM transaction from a `TxnLike` instance. */
  implicit def peer(implicit tx: TxnLike): InTxn = tx.peer

  /** Implicitly treats a Scala STM transaction as a `TxnLike` instance. */
  implicit def wrap(implicit peer: InTxn): TxnLike = new Wrapped(peer)

  private final class Wrapped(val peer: InTxn) extends TxnLike {
    override def toString: String = peer.toString

    def afterCommit (code: => Unit): Unit = ScalaTxn.afterCommit(_ => code)(peer)
  }
}
/** This is a minimal trait for any type of transactions that wrap an underlying Scala-STM transaction. */
trait TxnLike {
  /** Every transaction has a plain Scala-STM transaction as a peer. This comes handy for
    * setting up custom things like `TxnLocal`, `TMap`, or calling into the hooks of `concurrent.stm.Txn`.
    * It is also needed when re-wrapping the transaction of one system into another.
    */
  def peer: InTxn

  /** Registers a thunk to be executed after the transaction successfully committed. */
  def afterCommit(code: => Unit): Unit

  // will not be able to override this in Txn....
  // def beforeCommit(fun: TxnLike => Unit): Unit
}

object Txn {
  trait Resource extends Closeable with ExternalDecider

  private[this] final class Decider(var instances: List[Resource])
    extends ExternalDecider {

    def shouldCommit(implicit txn: InTxnEnd): Boolean = instances match {
      case single :: Nil => single.shouldCommit
      case _ => commitAll(instances, Nil)
    }

    @tailrec
    private[this] def commitAll(remain: List[Resource], done: List[Resource])(implicit txn: InTxnEnd): Boolean =
      remain match {
        case head :: tail =>
          if (head.shouldCommit) {
            commitAll(tail, head :: done)
          } else {
            if (done.nonEmpty) {
              Console.err.println(s"Resource $head failed to commit transaction.")
              done.foreach { r =>
                Console.err.println(s"Closing $r as a precaution. The system must be re-opened prior to further use.")
                try {
                  r.close()
                } catch {
                  case NonFatal(e) => e.printStackTrace()
                }
              }
            }
            false
          }

        case _ => true
      }
  }

  private[this] val decider = TxnLocal[Decider]()

  private[lucre] def addResource(resource: Resource)(implicit tx: InTxn): Unit =
    if (decider.isInitialized) {
      decider().instances ::= resource
    } else {
      val d = new Decider(resource :: Nil)
      ScalaTxn.setExternalDecider(d)
      decider() = d
    }

  private[lucre] def requireEmpty(): Unit =
    if (!allowNesting && ScalaTxn.findCurrent.isDefined)
      throw new IllegalStateException("Nested transactions are not supported by this system.")

  private[this] var allowNesting = false

  def atomic[A](fun: InTxn => A): A = {
    requireEmpty()
    TxnExecutor.defaultAtomic(fun)
  }

  /** Allows to share a transaction between two systems, necessary
    * for a cross-system `Obj.copy` operation.
    */
  def copy[S1 <: Sys[S1], S2 <: Sys[S2], A](fun: (S1#Tx, S2#Tx) => A)
                                           (implicit cursor1: Cursor[S1], cursor2: Cursor[S2]): A = {
    cursor1.step { tx1 =>
      allowNesting = true // allow only for the next cursor step
      try {
        cursor2.step { implicit tx2 =>
          allowNesting = false
          fun(tx1, tx2)
        }
      } finally {
        allowNesting = false
      }
    }
  }
}
trait Txn[S <: Sys[S]] extends TxnLike {
  /** Back link to the underlying system. */
  val system: S

  def inMemory: S#I#Tx

  def newID(): S#ID

  // ---- variables ----

  def newVar[A]    (id: S#ID, init: A)(implicit serializer: Serializer[S#Tx, S#Acc, A]): S#Var[A]
  def newBooleanVar(id: S#ID, init: Boolean): S#Var[Boolean]
  def newIntVar    (id: S#ID, init: Int    ): S#Var[Int]
  def newLongVar   (id: S#ID, init: Long   ): S#Var[Long]

  def newVarArray[A](size: Int): Array[S#Var[A]]

  /** Creates a new in-memory transactional map for storing and retrieving values based on a mutable's identifier
    * as key. If a system is confluently persistent, the `get` operation will find the most recent key that
    * matches the search key. Objects are not serialized but kept live in memory.
    *
    * ID maps can be used by observing views to look up associated view meta data even though they may be
    * presented with a more recent access path of the model peer (e.g. when a recent event is fired and observed).
    *
    * @tparam A         the value type in the map
    */
  def newInMemoryIDMap[A]: IdentifierMap[S#ID, S#Tx, A]

  def readVar[A]    (id: S#ID, in: DataInput)(implicit serializer: Serializer[S#Tx, S#Acc, A]): S#Var[A]
  def readBooleanVar(id: S#ID, in: DataInput): S#Var[Boolean]
  def readIntVar    (id: S#ID, in: DataInput): S#Var[Int]
  def readLongVar   (id: S#ID, in: DataInput): S#Var[Long]

  def readID(in: DataInput, acc: S#Acc): S#ID

  /** Creates a handle (in-memory) to refresh a stale version of an object, assuming that the future transaction is issued
    * from the same cursor that is used to create the handle, except for potentially having advanced.
    * This is a mechanism that can be used in live views to gain valid access to a referenced object
    * (e.g. self access).
    *
    * @param value         the object which will be refreshed when calling `get` on the returned handle
    * @param serializer    used to write and freshly read the object
    * @return              the handle
    */
  def newHandle[A](value: A)(implicit serializer: Serializer[S#Tx, S#Acc, A]): Source[S#Tx, A]

  // ---- completion ----

  def beforeCommit(fun: S#Tx => Unit): Unit
  def afterCommit (fun:      => Unit): Unit

  // ---- context ----

  // def newContext(): S#Context

  def use[A](context: S#Context)(fun: => A): A

  // ---- events ----

  private[lucre] def reactionMap: ReactionMap[S]

  // ---- attributes ----

  def attrMap(obj: Obj[S]): Obj.AttrMap[S]

  //  def attrPut   (obj: Obj[S], key: String, value: Obj[S]): Unit
  //  def attrGet   (obj: Obj[S], key: String): Option[Obj[S]]
  //  def attrRemove(obj: Obj[S], key: String): Unit
  //
  //  def attrIterator(obj: Obj[S]): Iterator[(String, Obj[S])]

  // def attrChanged: EventLike[S, AttrUpdate[S]]
}