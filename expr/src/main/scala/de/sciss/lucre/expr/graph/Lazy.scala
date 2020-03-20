/*
 *  Lazy.scala
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

import de.sciss.lucre.expr.Context
import de.sciss.lucre.stm.{Disposable, Sys}

trait Lazy extends Product {
  type Repr[S <: Sys[S]] <: Disposable[S#Tx]

  // this acts as a fast unique reference
  @transient final protected val ref = new AnyRef

  final def expand[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S] =
    ctx.visit(ref, mkRepr)

  protected def mkRepr[S <: Sys[S]](implicit ctx: Context[S], tx: S#Tx): Repr[S]
}