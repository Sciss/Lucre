/*
 *  ExpandedMapExOption.scala
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

package de.sciss.lucre.expr.graph.impl

import de.sciss.lucre.event.{IEvent, IPull, ITargets}
import de.sciss.lucre.event.impl.IEventImpl
import de.sciss.lucre.expr.{Context, IExpr}
import de.sciss.lucre.expr.graph.{Ex, It}
import de.sciss.lucre.stm.Sys
import de.sciss.model.Change

final class ExpandedMapExOption[S <: Sys[S], A, B](in: IExpr[S, Option[A]], it: It.Expanded[S, A],
                                                   fun: Ex[B], tx0: S#Tx)
                                                  (implicit protected val targets: ITargets[S], ctx: Context[S])
  extends IExpr[S, Option[B]] with IEventImpl[S, Change[Option[B]]] {

  in.changed.--->(this)(tx0)

  def value(implicit tx: S#Tx): Option[B] = {
    val outerV = in.value
    valueOf(outerV)
  }

  private def valueOf(inOption: Option[A])(implicit tx: S#Tx): Option[B] =
    inOption.map { v0 =>
      it.setValue(v0)  // make sure we have the first value ready
      val (out, d) = ctx.nested {
        val funEx = fun.expand[S]
        val vn    = funEx.value
        vn
      }

      d.dispose()
      out
    }

  private[lucre] def pullUpdate(pull: IPull[S])(implicit tx: S#Tx): Option[Change[Option[B]]] =
    pull(in.changed).flatMap { inCh =>
      val before  = valueOf(inCh.before )
      val now     = valueOf(inCh.now    )
      if (before == now) None else Some(Change(before, now))
    }

  def dispose()(implicit tx: S#Tx): Unit =
    in.changed.-/->(this)

  def changed: IEvent[S, Change[Option[B]]] = this
}