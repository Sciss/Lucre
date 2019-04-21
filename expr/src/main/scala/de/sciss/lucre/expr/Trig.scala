/*
 *  Trig.scala
 *  (Lucre)
 *
 *  Copyright (c) 2009-2019 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.lucre.expr

import de.sciss.lucre.expr.Ex.Context
import de.sciss.lucre.stm.Sys

object Trig {
  final val Some: Option[Unit] = scala.Some(())

  trait Lazy extends Trig {
    // this acts now as a fast unique reference
    @transient final private[this] lazy val ref = new AnyRef

    final def expand[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): ITrigger[S] =
      ctx.visit(ref, mkTrig)

    protected def mkTrig[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): ITrigger[S]
  }
}
trait Trig extends Product {
  def expand[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): ITrigger[S]
}