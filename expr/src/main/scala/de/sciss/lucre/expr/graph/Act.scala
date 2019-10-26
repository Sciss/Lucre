/*
 *  Act.scala
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

package de.sciss.lucre.expr.graph

import de.sciss.lucre.expr.impl.IActionImpl
import de.sciss.lucre.expr.{Context, IAction, IControl}
import de.sciss.lucre.stm.Sys

import scala.language.{higherKinds, implicitConversions}

object Act {
  def apply(xs: Act*): Act = SeqImpl(xs)

  private final class ExpandedSeq[S <: Sys[S]](xs: Seq[IAction[S]]) extends IActionImpl[S] {
    def executeAction()(implicit tx: S#Tx): Unit =
      xs.foreach(_.executeAction())
  }

  private final case class SeqImpl(xs: Seq[Act]) extends Act {
    type Repr[S <: Sys[S]] = IAction[S]

    override def productPrefix = "Act" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] =
      new ExpandedSeq(xs.map(_.expand[S]))
  }

  final case class Link[A](source: Trig, sink: Act)
    extends Control {

    override def productPrefix = s"Act$$Link" // serialization

    type Repr[S <: Sys[S]] = IControl[S]

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val tr    = source.expand[S]
      val ac    = sink  .expand[S]
      ac.addSource(tr)
//      val peer  = tr.changed.react { implicit tx => _ => ac.executeAction() }
      IControl.empty // .wrap(peer)
    }
  }

//  /** Treats an expression of actions as an action, simply
//    * expanding its value every time it is called.
//    */
//  implicit def flatten(in: Ex[Act]): Act = Flatten(in)

//  trait Option[S <: Sys[S]] extends IExpr[S, _Option[Act]] {
//    def executeAction()(implicit tx: S#Tx): Boolean
//  }

  trait Option extends Act {
    type Repr[S <: Sys[S]] <: IAction.Option[S]

    def isEmpty   : Ex[Boolean]
    def isDefined : Ex[Boolean]

    def orElse(that: Act): Act = OrElse(this, that)
  }

//  implicit final class Ops (private val in: Ex[_Option[Act]]) extends AnyVal {
//    def orNop: Act = OrNop(in)
//  }

//  private final class ExpandedFlatten[S <: Sys[S]](in: IExpr[S, Act])(implicit ctx: Context[S])
//    extends IActionImpl[S] {
//
//    def executeAction()(implicit tx: S#Tx): Unit = {
//      val act = in.value
//      // XXX TODO --- nesting produces all sorts of problems
//      // why did we try to do that in the first place?
////      val (actEx, d) = ctx.nested {
////        act.expand[S]
////      }
//      val actEx = act.expand[S]
//      actEx.executeAction()
////      d.dispose()
//    }
//  }

  private final class ExpandedOrElse[S <: Sys[S]](a: IAction.Option[S], b: IAction[S])(implicit ctx: Context[S])
    extends IActionImpl[S] {

    def executeAction()(implicit tx: S#Tx): Unit = {
      val c = if (a.isDefined) a else b
      c.executeAction()
    }
  }

  final case class OrElse(a: Act.Option, b: Act) extends Act {
    override def productPrefix = s"Act$$OrElse" // serialization

    type Repr[S <: Sys[S]] = IAction[S]

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] =
      new ExpandedOrElse(a.expand[S], b.expand[S])
  }

//  final case class Flatten(in: Ex[Act]) extends Act {
//
//    override def productPrefix = s"Act$$Flatten" // serialization
//
//    type Repr[S <: Sys[S]] = IAction[S]
//
//    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] =
//      new ExpandedFlatten(in.expand[S])
//  }

  private final class ExpandedNop[S <: Sys[S]]/*(implicit ctx: Context[S])*/
    extends IActionImpl[S] {

    def executeAction()(implicit tx: S#Tx): Unit = ()
  }

  final case class Nop() extends Act {

    override def productPrefix = s"Act$$Nop" // serialization

    type Repr[S <: Sys[S]] = IAction[S]

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] =
      new ExpandedNop
  }
}
trait Act extends Lazy {
  type Repr[S <: Sys[S]] <: IAction[S]
}
