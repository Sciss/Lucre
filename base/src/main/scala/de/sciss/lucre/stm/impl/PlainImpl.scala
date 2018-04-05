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
  
  private abstract class AbstractVar {
    final def dispose()(implicit tx: Plain): Unit = ()

    final def write(out: DataOutput): Unit = opNotSupported("Plain.Var.write")
  }

  private final class VarImpl[A](private[this] var value: A)
    extends AbstractVar with Var[Plain, A] {

    def apply()(implicit tx: Plain): A = value

    def update(v: A)(implicit tx: Plain): Unit = value = v
  }

  private final class BooleanVarImpl(private[this] var value: Boolean)
    extends AbstractVar with Var[Plain, Boolean] {

    def apply()(implicit tx: Plain): Boolean = value

    def update(v: Boolean)(implicit tx: Plain): Unit = value = v
  }

  private final class IntVarImpl(private[this] var value: Int)
    extends AbstractVar with Var[Plain, Int] {

    def apply()(implicit tx: Plain): Int = value

    def update(v: Int)(implicit tx: Plain): Unit = value = v
  }

  private final class LongVarImpl(private[this] var value: Long) 
    extends AbstractVar with Var[Plain, Long] {
    
    def apply()(implicit tx: Plain): Long = value

    def update(v: Long)(implicit tx: Plain): Unit = value = v
  }

  private final class SysImpl extends Plain {
    type S = Plain
    
    override def toString = "Plain"

    // ---- Base ----

    def close(): Unit = ()

    // ---- Cursor ----

    def step[A](fun: Tx => A): A = fun(this)

    def position(implicit tx: S#Tx): Acc = ()

    // ---- Executor ----

    val system: S = this

    def newId(): Id = new IdImpl

    def readId(in: DataInput, acc: Acc): Id = opNotSupported("readId")

    def newVar[A](id: Id, init: A)(implicit serializer: Serializer[Tx, Acc, A]): Var[A] =
      new VarImpl(init)

    def newBooleanVar (id: Id, init: Boolean ): Var[Boolean] = new BooleanVarImpl (init)
    def newIntVar     (id: Id, init: Int     ): Var[Int]     = new IntVarImpl     (init)
    def newLongVar    (id: Id, init: Long    ): Var[Long]    = new LongVarImpl    (init)

    def newVarArray[A](size: Int): Array[Var[A]] = new Array[S#Var[A]](size)

    def newInMemoryIdMap[A]: IdentifierMap[Id, Tx, A] = ???

    def readVar[A](id: Id, in: DataInput)(implicit serializer: Serializer[Tx, Acc, A]): Var[A] =
      opNotSupported("readVar")

    def readBooleanVar(id: Id, in: DataInput): Var[Boolean]  = opNotSupported("readBooleanVar")
    def readIntVar    (id: Id, in: DataInput): Var[Int]      = opNotSupported("readIntVar")
    def readLongVar   (id: Id, in: DataInput): Var[Long]     = opNotSupported("readLongVar")

    def newHandle[A](value: A)(implicit serializer: Serializer[Tx, Acc, A]): Source[Tx, A] =
      new EphemeralHandle(value)
  }
}