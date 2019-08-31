/*
 *  ArtifactLocation.scala
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

package de.sciss.lucre.artifact

import java.io.File

import de.sciss.lucre.artifact
import de.sciss.lucre.event.Targets
import de.sciss.lucre.expr
import de.sciss.lucre.expr.Expr
import de.sciss.lucre.stm.Sys
import de.sciss.serial.{ImmutableSerializer, Serializer}

object ArtifactLocation extends expr.impl.ExprTypeImpl[File, ArtifactLocation] {
  import artifact.{ArtifactLocation => Repr}

  final val typeId = 0x10003

  def tmp[S <: Sys[S]]()(implicit tx: S#Tx): Const[S] = {
    val dir   = File.createTempFile("artifacts", "tmp")
    dir.delete()
    dir.mkdir()
    dir.deleteOnExit()
    newConst(dir)
  }
  
  implicit def valueSerializer: ImmutableSerializer[File] = Serializer.File

  def tryParse(value: Any): Option[File] = value match {
    case loc: File  => Some(loc)
    case _          => None
  }

  protected def mkConst[S <: Sys[S]](id: S#Id, value: A)(implicit tx: S#Tx): Const[S] =
    new _Const[S](id, value)

  protected def mkVar[S <: Sys[S]](targets: Targets[S], vr: S#Var[_Ex[S]], connect: Boolean)
                                  (implicit tx: S#Tx): Var[S] = {
    val res = new _Var[S](targets, vr)
    if (connect) res.connect()
    res
  }

  private[this] final class _Const[S <: Sys[S]](val id: S#Id, val constValue: A)
    extends ConstImpl[S] with Repr[S]

  private[this] final class _Var[S <: Sys[S]](val targets: Targets[S], val ref: S#Var[_Ex[S]])
    extends VarImpl[S] with Repr[S]
}
/** An artifact location is a directory on an external storage. */
trait ArtifactLocation[S <: Sys[S]] extends Expr[S, File] {
  /** Alias for `value` */
  def directory(implicit tx: S#Tx): File = value
}