/*
 *  ExpandedMapExOption.scala
 *  (Lucre)
 *
 *  Copyright (c) 2009-2019 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Affero General Public License v3+
*
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.lucre.expr.graph.impl

import de.sciss.lucre.event.impl.IEventImpl
import de.sciss.lucre.event.{Caching, IEvent, IPull, IPush, ITargets}
import de.sciss.lucre.expr.graph.Ex
import de.sciss.lucre.expr.{Context, IExpr}
import de.sciss.lucre.stm.{Disposable, Sys}
import de.sciss.lucre.stm.TxnLike.peer
import de.sciss.model.Change

import scala.concurrent.stm.Ref

//final class ExpandedMapExOption[S <: Sys[S], A, B](in: IExpr[S, Option[A]], fun: Ex[B], tx0: S#Tx)
//                                                  (implicit protected val targets: ITargets[S], ctx: Context[S])
//  extends IExpr[S, Option[B]] with IEventImpl[S, Change[Option[B]]] {
//
//  in.changed.--->(this)(tx0)
//
//  def value(implicit tx: S#Tx): Option[B] = {
//    val outerV = in.value
//    valueOf(outerV)
//  }
//
//  private def valueOf(inOption: Option[A])(implicit tx: S#Tx): Option[B] =
//    if (inOption.isEmpty) None else {
//      val (out, d) = ctx.nested {
//        val funEx = fun.expand[S]
//        val vn    = funEx.value
//        vn
//      }
//
//      d.dispose()
//      Some(out)
//    }
//
//  private[lucre] def pullUpdate(pull: IPull[S])(implicit tx: S#Tx): Option[Change[Option[B]]] =
//    pull(in.changed).flatMap { inCh =>
//      val before  = valueOf(inCh.before )
//      val now     = valueOf(inCh.now    )
//      if (before == now) None else Some(Change(before, now))
//    }
//
//  def dispose()(implicit tx: S#Tx): Unit =
//    in.changed.-/->(this)
//
//  def changed: IEvent[S, Change[Option[B]]] = this
//}

final class ExpandedMapExOption[S <: Sys[S], A, B](in: IExpr[S, Option[A]], fun: Ex[B], tx0: S#Tx)
                                                  (implicit protected val targets: ITargets[S], ctx: Context[S])
  extends IExpr[S, Option[B]] with IEventImpl[S, Change[Option[B]]] with Caching {

  in.changed.--->(this)(tx0)

  private[this] val ref = Ref(valueOf(in.value(tx0))(tx0))

  def value(implicit tx: S#Tx): Option[B] = // ref().map(_._1)
    IPush.tryPull(this).fold(ref().map(_._1))(_.now)

  private def valueOf(inOption: Option[A])(implicit tx: S#Tx): Option[(B, Disposable[S#Tx])] =
    if (inOption.isEmpty) None else {
      val tup = ctx.nested {
        val funEx = fun.expand[S]
        val vn    = funEx.value
        vn
      }

      Some(tup)
    }

  private[lucre] def pullUpdate(pull: IPull[S])(implicit tx: S#Tx): Option[Change[Option[B]]] =
    pull(in.changed).flatMap { inCh =>
      val beforeTup = ref()
      beforeTup.foreach(_._2.dispose())
      val before    = beforeTup.map(_._1)
      val nowTup    = valueOf(inCh.now)
      ref() = nowTup
      val now       = nowTup.map(_._1)
      if (before == now) None else Some(Change(before, now))
    }

  def dispose()(implicit tx: S#Tx): Unit = {
    in.changed.-/->(this)
    ref.swap(None).foreach(_._2.dispose())
  }

  def changed: IEvent[S, Change[Option[B]]] = this
}
