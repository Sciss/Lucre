/*
 *  PlainImpl.scala
 *  (Lucre)
 *
 *  Copyright (c) 2009-2018 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.lucre.stm
package impl

import de.sciss.serial.{DataInput, DataOutput, Serializer}

object PlainImpl {
  def apply(): Plain = new SysImpl

  private def opNotSupported(name: String): Nothing = sys.error(s"Operation not supported: $name")

  private final class IdImpl extends Identifier[Plain] {
    override def toString = s"Plain.Id@${hashCode.toHexString}"

    def dispose()(implicit tx: Plain): Unit = ()

    def write(out: DataOutput): Unit = opNotSupported("Plain.Id.write")
  }
  
  private abstract class AbstractVar extends Disposable[Plain#Tx] {
    final def dispose()(implicit tx: Plain#Tx): Unit = ()

    final def write(out: DataOutput): Unit = opNotSupported("Plain.Var.write")
  }

  private final class VarImpl[A](private[this] var value: A)
    extends AbstractVar with Var[Plain, A] {

    def apply()(implicit tx: Plain): A = value

    def update(v: A)(implicit tx: Plain): Unit = value = v

    def swap(v: A)(implicit tx: Plain): A = {
      val res = value
      value = v
      res
    }
  }

  private final class BooleanVarImpl(private[this] var value: Boolean)
    extends AbstractVar with Var[Plain, Boolean] {

    def apply()(implicit tx: Plain): Boolean = value

    def update(v: Boolean)(implicit tx: Plain): Unit = value = v

    def swap(v: Boolean)(implicit tx: Plain): Boolean = {
      val res = value
      value = v
      res
    }
  }

  private final class IntVarImpl(private[this] var value: Int)
    extends AbstractVar with Var[Plain, Int] {

    def apply()(implicit tx: Plain): Int = value

    def update(v: Int)(implicit tx: Plain): Unit = value = v

    def swap(v: Int)(implicit tx: Plain): Int = {
      val res = value
      value = v
      res
    }
  }

  private final class LongVarImpl(private[this] var value: Long) 
    extends AbstractVar with Var[Plain, Long] {
    
    def apply()(implicit tx: Plain): Long = value

    def update(v: Long)(implicit tx: Plain): Unit = value = v

    def swap(v: Long)(implicit tx: Plain): Long = {
      val res = value
      value = v
      res
    }
  }

  private final class SysImpl extends Plain {
    type S = Plain
    
    override def toString = "Plain"

    // ---- Base ----

    def close(): Unit = ()

    def inMemory: I = this

    def inMemoryTx(tx: S#Tx): I#Tx = tx

    // ---- Cursor ----

    def step[A](fun: Tx => A): A = fun(this)

    def stepTag[A](systemTimeNanos: Long)(fun: S#Tx => A): A = fun(this)

    def position(implicit tx: S#Tx): Acc = ()

    // ---- Executor ----

    val system: S = this

    def cursor: Cursor[S] = this

    def newId(): Id = new IdImpl

    def readId(in: DataInput, acc: Acc): Id = opNotSupported("readId")

    def newRef[A](init: A): Ref[Tx, A] = new VarImpl(init)

    def newVar[A](id: Id, init: A)(implicit serializer: Serializer[Tx, Acc, A]): Var[A] =
      new VarImpl(init)

    def newBooleanVar (id: Id, init: Boolean ): Var[Boolean] = new BooleanVarImpl (init)
    def newIntVar     (id: Id, init: Int     ): Var[Int]     = new IntVarImpl     (init)
    def newLongVar    (id: Id, init: Long    ): Var[Long]    = new LongVarImpl    (init)

    def newVarArray[A](size: Int): Array[Var[A]] = new Array[S#Var[A]](size)

    def newInMemoryIdMap[A]: IdentifierMap[Id, Tx, A] = new PlainIdentifierMap[A]

    def newInMemoryMap[K, V]: RefMap[S, K, V] = new PlainInMemoryMap[K, V]
    def newInMemorySet[A]   : RefSet[S, A]    = new PlainInMemorySet[A]

    def readVar[A](id: Id, in: DataInput)(implicit serializer: Serializer[Tx, Acc, A]): Var[A] =
      opNotSupported("readVar")

    def readBooleanVar(id: Id, in: DataInput): Var[Boolean]  = opNotSupported("readBooleanVar")
    def readIntVar    (id: Id, in: DataInput): Var[Int]      = opNotSupported("readIntVar")
    def readLongVar   (id: Id, in: DataInput): Var[Long]     = opNotSupported("readLongVar")

    def newHandle[A](value: A)(implicit serializer: Serializer[Tx, Acc, A]): Source[Tx, A] =
      new EphemeralHandle(value)
  }
}