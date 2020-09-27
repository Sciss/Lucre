/*
 *  ProjectionTest.scala
 *  (Lucre 4)
 *
 *  Copyright (c) 2009-2020 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.lucre
package confluent

import de.sciss.serial.DataOutput
import de.sciss.lucre.{Txn => LTxn, Var => LVar, Sys => LSys}

// XXX TODO -- this is no longer useful, just remove
trait ProjectionTest {

  object KTx {
    implicit def downCast(implicit tx: BiTx): KTx[KTemp] = sys.error("TODO")
  }

  trait KTx[S <: KTempLike[S]] extends LTxn[S]

  trait KTempLike[S <: KTempLike[S]] extends LTxn[S] {
    type Tx <: KTx[S]
  }

  trait KTemp extends KTempLike[KTemp] {
    final type Tx = KTx[KTemp]
    final type Var[@specialized ~] = LVar[Tx, ~]
  }

  trait BiTx extends KTx[BiTemp]

  trait BiTemp extends KTempLike[BiTemp] {
    final type Tx = BiTx
  }

  def test (implicit tx: KTemp #Tx): Unit = ()

  def test2(implicit tx: BiTemp#Tx): Unit = test

  //   def txDownCastWorks[ S <: KTempLike[ S ]]( x: S#Var[ Int ])( implicit tx: BiTemp#Tx ): Unit = {
  //      x.set( 33 )( tx )
  //   }
  //
  //   def txUpCastFails[ S <: KTempLike[ S ]]( x: BiTemp#Var[ Int ])( implicit tx: S#Tx ): Unit = {
  ////      x.set( 33 )
  //   }

  def test3[T, S <: LSys /*[S]*/, Time](dynVar: LVar[Time, Int])(implicit tx: T, dynView: T => Time): Unit = {
    implicit val dtx: Time = dynView(tx)
    // dynVar.transform(_ + 33)(tx)
    dynVar() = dynVar() + 33
  }

  trait PCursor[+Tx] {
    def time: Double

    def peer: Tx
  }

  class DynamicVar[/*-*/Tx, A](tx: PCursor[Tx]) extends LVar[PCursor[Tx], A] {
    type T = PCursor[Tx]

    def apply()(implicit tx: T): A = getAt(tx.time) // (tx.peer)

    def getAt(time: Double): A = notImplemented()

    def transform(fun: A => A)(implicit tx: T): Unit = this() = fun(this())

    def update(v: A)(implicit tx: T): Unit = setAt(tx.time, v) // (tx.peer)

    def swap(v: A)(implicit tx: T): A = {
      val res = apply()
      update(v)
      res
    }

    def setAt(time: Double, v: A): Unit = notImplemented()

    def dispose()(implicit tx: T): Unit = ()

    def write(out: DataOutput): Unit = ()

    def isFresh: Boolean = notImplemented()

    private def notImplemented(): Nothing = sys.error("Not implemented")
  }
}
