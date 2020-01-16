/*
 *  IChangeEventImpl.scala
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

package de.sciss.lucre.event.impl

import de.sciss.lucre.event.{IChangeEvent, ITargets}
import de.sciss.lucre.stm.Base
import de.sciss.model.Change

trait IChangeEventImpl[S <: Base[S], +A] extends IEventImpl[S, Change[A]] with IChangeEvent[S, A] {
  protected def targets: ITargets[S]
}