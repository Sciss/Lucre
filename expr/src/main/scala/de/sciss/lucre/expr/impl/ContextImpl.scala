/*
 *  ContextMixin.scala
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

package de.sciss.lucre.expr.impl

import de.sciss.lucre.event.ITargets
import de.sciss.lucre.expr.graph.{Control, It}
import de.sciss.lucre.expr.{Context, Graph}
import de.sciss.lucre.stm
import de.sciss.lucre.stm.TxnLike.peer
import de.sciss.lucre.stm.{Cursor, Disposable, Obj, Sys, UndoManager, Workspace}

import scala.annotation.tailrec
import scala.concurrent.stm.{Ref, TMap, TxnLocal}

trait ContextMixin[S <: Sys[S]] extends Context[S] {
  // ---- abstract ----

  protected def selfH: Option[stm.Source[S#Tx, Obj[S]]]

  // ---- impl ----

  final val targets: ITargets[S] = ITargets[S]

  private type SourceMap  = Map [AnyRef, Disposable[S#Tx]]
  private type Properties = TMap[AnyRef, Map[String, Any]]

  private[this] val globalMap : Ref[SourceMap]  = Ref(Map.empty)
  private[this] val properties: Properties      = TMap.empty

  def initGraph(g: Graph)(implicit tx: S#Tx): Unit = {
    properties.clear()
    g.controls.foreach { conf =>
      properties.put(conf.control.token, conf.properties)
    }
  }

  private[this] val terminals = TxnLocal(List.empty[Nested])
  private[this] val markers   = TxnLocal(Set .empty[Int   ])

  private final class Nested(val ref: AnyRef, val level: Int) {
    var sourceMap: SourceMap = Map.empty
  }

  def nested[A](it: It.Expanded[S, _])(body: => A)(implicit tx: S#Tx): (A, Disposable[S#Tx]) = {
    val tOld    = terminals()
    val n       = new Nested(it.ref, level = tOld.size)
    terminals() = tOld :+ n // n :: tOld
    val res     = body
    terminals() = tOld
    markers.transform(_ - n.level)

    val disposableIt = n.sourceMap.values
    val disposable: Disposable[S#Tx] =
      if (disposableIt.isEmpty) Disposable.empty
      else disposableIt.toList match {
        case single :: Nil  => single
        case more           => Disposable.seq(more: _*)
      }

    (res, disposable)
  }

  def dispose()(implicit tx: S#Tx): Unit = {
    require (terminals().isEmpty, "Must not call dispose in a nested operation")
    val m = globalMap.swap(Map.empty)
    m.foreach(_._2.dispose())
  }

  final def visit[U <: Disposable[S#Tx]](ref: AnyRef, init: => U)(implicit tx: S#Tx): U = {
    val t = terminals()
    if (t.nonEmpty) t.find(_.ref == ref) match {
      case Some(n)  => markers.transform(_ + n.level)
      case None     =>
    }

    globalMap().get(ref) match {
      case Some(res) => res.asInstanceOf[U]  // not so pretty...
      case None =>
        @tailrec
        def loop(rem: List[Nested]): U =
          rem match {
            case head :: tail =>
              head.sourceMap.get(ref) match {
                case Some(res)  => res.asInstanceOf[U]
                case None       => loop(tail)
              }

            case Nil =>
              val oldMarkers  = markers.swap(Set.empty)
              val exp         = init
              val newMarkers  = markers()
              if (newMarkers.isEmpty) {
                globalMap.transform(_ + (ref -> exp))
                markers()     = oldMarkers
              } else {
                val m         = newMarkers.max
                val n         = terminals().apply(m)
                n.sourceMap  += (ref -> exp)
                markers()     = oldMarkers union newMarkers
              }
              exp
          }

        loop(t)
     }
  }

  def selfOption(implicit tx: S#Tx): Option[Obj[S]] = selfH.map(_.apply())

  def getProperty[A](c: Control, key: String)(implicit tx: S#Tx): Option[A] = {
    val m0: Map[String, Any] = properties.get(c.token).orNull
    if (m0 == null) None else {
      m0.get(key).asInstanceOf[Option[A]]
    }
  }
}

final class ContextImpl[S <: Sys[S]](protected val selfH: Option[stm.Source[S#Tx, Obj[S]]],
                                     val attr: Context.Attr[S])
                                    (implicit val workspace: Workspace[S], val cursor: Cursor[S],
                                     val undoManager: UndoManager[S])
  extends ContextMixin[S]