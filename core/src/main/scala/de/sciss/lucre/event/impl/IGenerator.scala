/*
 *  IGenerator.scala
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

package de.sciss.lucre.event
package impl

import de.sciss.lucre.stm.Base

trait IGenerator[S <: Base[S], A] extends IEventImpl[S, A] {
  final def fire(update: A)(implicit tx: S#Tx): Unit = {
    logEvent(s"$this fire $update")
    IPush(this, update)(tx, targets)
  }
}