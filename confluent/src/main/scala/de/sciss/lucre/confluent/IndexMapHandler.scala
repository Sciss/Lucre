/*
 *  IndexMapHandler.scala
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

package de.sciss.lucre.confluent

import de.sciss.serial.{ImmutableSerializer, DataInput}

trait IndexMapHandler[S <: Sys[S]] {
  def readIndexMap[A](in: DataInput, index: S#Acc)
                     (implicit tx: S#Tx, serializer: ImmutableSerializer[A]): IndexMap[S, A]

  def newIndexMap[A](index: S#Acc, rootTerm: Long, rootValue: A)
                    (implicit tx: S#Tx, serializer: ImmutableSerializer[A]): IndexMap[S, A]

  // true is term1 is ancestor of term2
  def isAncestor(term1: Long, term2: Long)(implicit tx: S#Tx): Boolean
}