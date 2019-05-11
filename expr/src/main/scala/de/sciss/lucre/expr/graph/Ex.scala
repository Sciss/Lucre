/*
 *  Ex.scala
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

package de.sciss.lucre.expr.graph

import de.sciss.lucre.aux.Aux
import de.sciss.lucre.expr.graph.impl.{ExpandedMapActOption, ExpandedMapExOption, ExpandedMapExSeq}
import de.sciss.lucre.expr.{Context, Graph, IExpr}
import de.sciss.lucre.stm.Sys
import de.sciss.serial.DataInput

import scala.language.higherKinds

object Ex {
  private val anyCanMapExOption   = new CanMapExOption  [Any]
  private val anyCanMapExSeq      = new CanMapExSeq     [Any]

  private lazy val _init: Unit = {
    Aux.addFactory(anyCanMapExOption)
    Aux.addFactory(anyCanMapExSeq   )
    Aux.addFactory(CanMapActOption  )
  }

  def init(): Unit = _init

  // common to all type classes
  private abstract class MapSupport extends Aux with Aux.Factory {
    def mkClosure[A, B](fun: Ex[A] => B): (It[A], Graph, B) = {
      val b     = Graph.builder
      val it    = b.allocToken[A]()
      val (c, r) = Graph.withResult {
        fun(it)
      }
      (it, c, r)
    }

    def readIdentifiedAux(in: DataInput): Aux = this
  }

  private final class CanMapExOption[B] extends MapSupport
    with CanMap[Option, Ex[B], Ex[Option[B]]] {

    final val id = 1001

    override def toString = "CanMapExOption"

    def map[A](from: Ex[Option[A]], fun: Ex[A] => Ex[B]): Ex[Option[B]] = {
      val (it, closure, res) = mkClosure(fun)
      MapExOption[A, B](in = from, it = it, closure = closure, fun = res)
    }
  }

  private final class CanMapExSeq[B] extends MapSupport
    with CanMap[Seq, Ex[B], Ex[Seq[B]]] {

    final val id = 1002

    override def toString = "CanMapExSeq"

    def map[A](from: Ex[Seq[A]], fun: Ex[A] => Ex[B]): Ex[Seq[B]] = {
      val (it, closure, res) = mkClosure(fun)
      MapExSeq[A, B](in = from, it = it, closure = closure, fun = res)
    }
  }

  private final object CanMapActOption extends MapSupport
    with CanMap[Option, Act, Ex[Option[Act]]] {

    final val id = 1003

    override def toString = "CanMapActOption"

    def map[A](from: Ex[Option[A]], fun: Ex[A] => Act): Ex[Option[Act]] = {
      val (it, closure, res) = mkClosure(fun)
      MapActOption[A](in = from, it = it, closure = closure, fun = res)
    }
  }

  final case class MapExOption[A, B] private (in: Ex[Option[A]], it: It[A], closure: Graph, fun: Ex[B])
    extends Ex[Option[B]] {

    type Repr[S <: Sys[S]] = IExpr[S, Option[B]]

    override def productPrefix: String = s"Ex$$MapExOption" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val inEx = in.expand[S]
      val itEx = it.expand[S]
      import ctx.targets
      new ExpandedMapExOption[S, A, B](inEx, itEx, fun, tx)
    }
  }

  final case class MapExSeq[A, B](in: Ex[Seq[A]], it: It[A], closure: Graph, fun: Ex[B]) extends Ex[Seq[B]] {
    type Repr[S <: Sys[S]] = IExpr[S, Seq[B]]

    override def productPrefix: String = s"Ex$$MapExSeq" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val inEx      = in      .expand[S]
      val itEx      = it      .expand[S]
      //    val closureEx = closure .expand[S]
      //    val funEx     = fun     .expand[S]
      import ctx.targets
      new ExpandedMapExSeq[S, A, B](inEx, itEx, /*closure, */ fun, tx)
    }
  }

  final case class MapActOption[A] private (in: Ex[Option[A]], it: It[A], closure: Graph, fun: Act)
    extends Ex[Option[Act]] {

    type Repr[S <: Sys[S]] = IExpr[S, Option[Act]]

    override def productPrefix: String = s"Ex$$MapActOption" // serialization

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] = {
      val inEx = in.expand[S]
      val itEx = it.expand[S]
      import ctx.targets
      new ExpandedMapActOption[S, A](inEx, itEx, fun, tx)
    }
  }

  object CanMap {
    implicit def exOption[B]: CanMap[Option, Ex[B], Ex[Option[B]]]    = anyCanMapExOption .asInstanceOf[CanMapExOption  [B]]
    implicit def exSeq   [B]: CanMap[Seq   , Ex[B], Ex[Seq   [B]]]    = anyCanMapExSeq    .asInstanceOf[CanMapExSeq     [B]]
    implicit def actOption  : CanMap[Option, Act  , Ex[Option[Act]]]  = CanMapActOption
    implicit def actSeq     : CanMap[Seq   , Act  , Ex[Seq   [Act]]]  = ???
  }
  trait CanMap[-From[_], -B, +To] extends Aux {
    def map[A](from: Ex[From[A]], fun: Ex[A] => B): To
  }

  object CanFlatMap {
    implicit def exOption [B]: CanFlatMap[Option, Ex[Option[B]], Ex[Option[B]]]  = ???
    implicit def exSeq    [B]: CanFlatMap[Seq   , Ex[Seq   [B]], Ex[Seq   [B]]]  = ???
    implicit def exSeqOpt [B]: CanFlatMap[Seq   , Ex[Option[B]], Ex[Seq   [B]]]  = ???
  }
  trait CanFlatMap[-From[_], -B, +To] {
    def flatMap[A](from: Ex[From[A]], fun: Ex[A] => B): To
  }
}
trait Ex[+A] extends Lazy {
  type Repr[S <: Sys[S]] <: IExpr[S, A]
}