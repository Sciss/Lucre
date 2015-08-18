/*
 *  Node.scala
 *  (LucreEvent)
 *
 *  Copyright (c) 2011-2015 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.lucre.event

import de.sciss.lucre.{event, stm}
import de.sciss.lucre.stm.{NoSys, Sys}
import de.sciss.serial
import de.sciss.serial.{DataInput, DataOutput, Serializer}

import scala.annotation.switch
import scala.collection.immutable.{IndexedSeq => Vec}

object Targets {
  private implicit def childrenSerializer[S <: Sys[S]]: serial.Serializer[S#Tx, S#Acc, Children[S]] =
    anyChildrenSer.asInstanceOf[ChildrenSer[S]]

  private val anyChildrenSer = new ChildrenSer[NoSys]

  private final class ChildrenSer[S <: Sys[S]] extends serial.Serializer[S#Tx, S#Acc, Children[S]] {
    def write(v: Children[S], out: DataOutput): Unit = {
      out./* PACKED */ writeInt(v.size)
      v.foreach { tup =>
        out.writeByte(tup._1)
        tup._2.writeSelector(out) // same as Selector.serializer.write(tup._2)
      }
    }

    def read(in: DataInput, access: S#Acc)(implicit tx: S#Tx): Children[S] = {
      val sz = in./* PACKED */ readInt()
      if (sz == 0) Vector.empty else Vector.fill(sz) {
        val slot      = in.readByte()
        val selector  = Selector.read(in, access)
        (slot, selector)
      }
    }
  }

  def apply[S <: Sys[S]](implicit tx: S#Tx): Targets[S] = {
    val id        = tx.newID()
    val children  = tx.newEventVar[Children[S]](id)
    new Impl(0, id, children)
  }

  def read[S <: Sys[S]](in: DataInput, access: S#Acc)(implicit tx: S#Tx): Targets[S] = {
    (in.readByte(): @switch) match {
      case 0      => readIdentified(in, access)
      case cookie => sys.error(s"Unexpected cookie $cookie")
    }
  }

  private[event] def readIdentified[S <: Sys[S]](in: DataInput, access: S#Acc)(implicit tx: S#Tx): Targets[S] = {
    val id = tx.readID(in, access)
    val children = tx.readEventVar[Children[S]](id, in)
    new Impl[S](0, id, children)
  }

  private final class Impl[S <: Sys[S]](cookie: Int, val id: S#ID, childrenVar: event.Var[S, Children[S]])
    extends Targets[S] {

    def write(out: DataOutput): Unit = {
      out        .writeByte(cookie)
      id         .write(out)
      childrenVar.write(out)
    }

    def dispose()(implicit tx: S#Tx): Unit = {
      if (children.nonEmpty) throw new IllegalStateException("Disposing a event reactor which is still being observed")
      id         .dispose()
      childrenVar.dispose()
    }

    private[event] def children(implicit tx: S#Tx): Children[S] = childrenVar.getOrElse(NoChildren)

    override def toString = s"Targets$id"

    private[event] def add(slot: Int, sel: Selector[S])(implicit tx: S#Tx): Boolean = {
      log(s"$this.add($slot, $sel)")
      val tup = (slot.toByte, sel)
      val old = childrenVar.get // .getFresh
      log(s"$this - old children = $old")
      old match {
        case Some(seq) =>
          childrenVar() = seq :+ tup
          !seq.exists(_._1 == slot)
        case _ =>
          childrenVar() = Vector(tup)
          true
      }
    }

    private[event] def remove(slot: Int, sel: Selector[S])(implicit tx: S#Tx): Boolean = {
      log(s"$this.remove($slot, $sel)")
      val tup = (slot, sel)
      val xs = childrenVar.getOrElse(NoChildren)
      log(s"$this - old children = $xs")
      val i = xs.indexOf(tup)
      if (i >= 0) {
        val xs1 = xs.patch(i, Vector.empty, 1) // XXX crappy way of removing a single element
        childrenVar() = xs1
        !xs1.exists(_._1 == slot)
      } else {
        log(s"$this - selector not found")
        false
      }
    }

    private[event] def observers(implicit tx: S#Tx): Vec[ObserverKey[S]] =
      children.flatMap(_._2.toObserverKey)

    def isEmpty (implicit tx: S#Tx): Boolean = children.isEmpty   // XXX TODO this is expensive
    def nonEmpty(implicit tx: S#Tx): Boolean = children.nonEmpty  // XXX TODO this is expensive

    private[event] def _targets: Targets[S] = this
  }
}

/** An abstract trait unifying invariant and mutating targets. This object is responsible
  * for keeping track of the dependents of an event source which is defined as the outer
  * object, sharing the same `id` as its targets. As a `Reactor`, it has a method to
  * `propagate` a fired event.
  */
sealed trait Targets[S <: Sys[S]] extends Reactor[S] {
  private[event] def children(implicit tx: S#Tx): Children[S]

  /** Adds a dependant to this node target.
    *
    * @param slot the slot for this node to be pushing to the dependant
    * @param sel  the target selector to which an event at slot `slot` will be pushed
    *
    * @return  `true` if this was the first dependant registered with the given slot, `false` otherwise
    */
  private[event] def add(slot: Int, sel: Selector[S])(implicit tx: S#Tx): Boolean

  def isEmpty (implicit tx: S#Tx): Boolean
  def nonEmpty(implicit tx: S#Tx): Boolean

  /** Removes a dependant from this node target.
    *
    * @param slot the slot for this node which is currently pushing to the dependant
    * @param sel  the target selector which was registered with the slot
    *
    * @return  `true` if this was the last dependant unregistered with the given slot, `false` otherwise
    */
  private[event] def remove(slot: Int, sel: Selector[S])(implicit tx: S#Tx): Boolean

  private[event] def observers(implicit tx: S#Tx): Vec[ObserverKey[S]]
}

object Node {
  def read[S <: Sys[S]](in: DataInput, access: S#Acc)(implicit tx: S#Tx): Node[S] = ???

  implicit def serializer[S <: Sys[S]]: Serializer[S#Tx, S#Acc, Node[S]] = ???
}

/** An `Event.Node` is most similar to EScala's `EventNode` class. It represents an observable
  * object and can also act as an observer itself. It adds the `Reactor` functionality in the
  * form of a proxy, forwarding to internally stored `Targets`. It also provides a final
  * implementation of the `Writable` and `Disposable` traits, asking sub classes to provide
  * methods `writeData` and `disposeData`. That way it is ensured that the sealed `Reactor` trait
  * is written first as the `Targets` stub, providing a means for partial deserialization during
  * the push phase of event propagation.
  *
  * This trait also implements `equals` and `hashCode` in terms of the `id` inherited from the
  * targets.
  */
trait Node[S <: Sys[S]] extends Reactor[S] {
  override def toString = s"Node$id"

  protected def targets: Targets[S]
  protected def writeData(out: DataOutput): Unit
  protected def disposeData()(implicit tx: S#Tx): Unit

  final private[event] def _targets: Targets[S] = targets

  final private[event] def children(implicit tx: S#Tx) = targets.children

  final def id: S#ID = targets.id

  private[event] def select(slot: Int): Event[S, Any]

  final def write(out: DataOutput): Unit = {
    targets.write(out)
    writeData(out)
  }

  final def dispose()(implicit tx: S#Tx): Unit = {
    disposeData() // call this first, as it may release events
    targets.dispose()
  }
}

/** The `Reactor` trait encompasses the possible targets (dependents) of an event. It defines
  * the `propagate` method which is used in the push-phase (first phase) of propagation. A `Reactor` is
  * either a persisted event `Node` or a registered `ObserverKey` which is resolved through the transaction
  * as pointing to a live view.
  */
sealed trait Reactor[S <: Sys[S]] extends stm.Mutable[S#ID, S#Tx] {
  private[event] def _targets: Targets[S]

  override def equals(that: Any): Boolean = that match {
    case value: Reactor[_] => id == value.id
    case _ => super.equals(that)
  }

  override def hashCode = id.hashCode()
}