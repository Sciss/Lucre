/*
 *  IAction.scala
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

package de.sciss.lucre.expr

import de.sciss.lucre.stm.{Base, Disposable, Form}

object IAction {
  trait Option[S <: Base[S]] extends IAction[S] {
    def isDefined(implicit tx: S#Tx): Boolean

    def executeIfDefined()(implicit tx: S#Tx): Boolean
  }

  def empty[S <: Base[S]]: IAction[S] = new Empty

  private final class Empty[S <: Base[S]] extends IAction[S] {
    def addSource(tr: ITrigger[S])(implicit tx: S#Tx): Unit = ()

    def executeAction()(implicit tx: S#Tx): Unit = ()

    def dispose()(implicit tx: S#Tx): Unit = ()
  }
}
trait IAction[S <: Base[S]] extends Form[S] with Disposable[S#Tx] {
  /** Directly adds a trigger input to the action.
    * Actions that do not produce successive events can
    * simply rewrite this as
    *
    * {{{
    * tr.changed.react { implicit tx => _ => executeAction() }
    * }}}
    *
    * If the action produces successive events, it should
    * prevent this indirection, as triggered cannot be logically
    * combined that way.
    */
  def addSource(tr: ITrigger[S])(implicit tx: S#Tx): Unit

  def executeAction()(implicit tx: S#Tx): Unit
}
