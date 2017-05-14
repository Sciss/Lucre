/*
 *  Generator.scala
 *  (Lucre)
 *
 *  Copyright (c) 2009-2017 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.lucre.event
package impl

import de.sciss.lucre.stm.Sys

trait Generator[S <: Sys[S], A] extends Event[S, A] {
  final def fire(update: A)(implicit tx: S#Tx): Unit = {
    logEvent(s"$this fire $update")
    Push(this, update)
  }
}