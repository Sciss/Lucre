/*
 *  InTxnRandom.scala
 *  (Lucre)
 *
 *  Copyright (c) 2009-2018 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.lucre.stm

import de.sciss.lucre.stm.impl.RandomImpl.{BaseImpl, calcSeedUniquifier, initialScramble}

import scala.concurrent.stm.{InTxn, Ref => STMRef}

object InTxnRandom {
  def apply():           Random[InTxn] = apply(calcSeedUniquifier() ^ System.nanoTime())
  def apply(seed: Long): Random[InTxn] = new Impl(STMRef(initialScramble(seed)))

  private final class Impl(seedRef: STMRef[Long]) extends BaseImpl[InTxn] {
    protected def refSet(value: Long)(implicit tx: InTxn): Unit = seedRef() = value

    protected def refGet(implicit tx: InTxn): Long = seedRef()
  }
}