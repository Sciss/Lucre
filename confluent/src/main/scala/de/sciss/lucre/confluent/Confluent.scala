/*
 *  Confluent.scala
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

package de.sciss.lucre.confluent

import de.sciss.lucre.confluent.impl.{ConfluentImpl => Impl}
import de.sciss.lucre.{confluent, stm}
import de.sciss.lucre.stm.DataStore

object Confluent {
  def apply(storeFactory: DataStore.Factory): Confluent = Impl(storeFactory)
}

trait Confluent extends Sys[Confluent] {
  final protected type S = Confluent
  final type D  = stm.Durable
  final type I  = stm.InMemory
  final type Tx = confluent.Txn[S]
}