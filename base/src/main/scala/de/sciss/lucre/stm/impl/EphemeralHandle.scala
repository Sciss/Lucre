/*
 *  EphemeralHandle.scala
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

package de.sciss.lucre.stm
package impl

final class EphemeralHandle[Tx, A](value: A) extends Source[Tx, A] {
  override def toString = s"handle: $value"

  def apply()(implicit tx: Tx): A = value
}