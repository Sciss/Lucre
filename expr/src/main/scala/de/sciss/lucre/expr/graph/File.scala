/*
 *  File.scala
 *  (Lucre)
 *
 *  Copyright (c) 2009-2020 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.lucre.expr.graph

import java.io.{File => _File}

import de.sciss.lucre.expr.impl.IActionImpl
import de.sciss.lucre.expr.{Context, IAction, IExpr}
import de.sciss.lucre.stm.Sys

object File {
  private final class MkDirExpanded[S <: Sys[S]](f: IExpr[S, _File])
    extends IActionImpl[S] {

    def executeAction()(implicit tx: S#Tx): Unit = {
      val fv = f.value
      tx.afterCommit(fv.mkdirs()) // plural! we want to ensure parent directories are created, too
    }
  }

  final case class MkDir(f: Ex[_File]) extends Act {
    override def productPrefix: String = s"File$$MkDir"  // serialization

    type Repr[S <: Sys[S]] = IAction[S]

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] =
      new MkDirExpanded(f.expand[S])
  }

  private final class DeleteExpanded[S <: Sys[S]](f: IExpr[S, _File])
    extends IActionImpl[S] {

    def executeAction()(implicit tx: S#Tx): Unit = {
      val fv = f.value
      tx.afterCommit(fv.delete())
    }
  }

  final case class Delete(f: Ex[_File]) extends Act {
    override def productPrefix: String = s"File$$Delete"  // serialization

    type Repr[S <: Sys[S]] = IAction[S]

    protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] =
      new DeleteExpanded(f.expand[S])
  }
}
