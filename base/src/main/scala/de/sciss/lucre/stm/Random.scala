/*
 *  TxnRandom.scala
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

import de.sciss.lucre.stm
import de.sciss.lucre.stm.impl.{RandomImpl => Impl}

/** Like java's random, but within a transactional cell. */
object Random {
  def apply[S <: Base[S]](id: S#Id)(implicit tx: S#Tx): Random[S#Tx] = Impl(id)

  def apply[S <: Base[S]](id: S#Id, seed: Long)(implicit tx: S#Tx): Random[S#Tx] = Impl(id, seed)

  def wrap[Tx](peer: stm.Var[Tx, Long]): Random[Tx] = Impl.wrap(peer)
}

/** A transactional pseudo-random number generator which
  * behaves numerically like `java.util.Random`.
  */
trait Random[-Tx] {
  /** Generates a random `Boolean` value. */
  def nextBoolean  ()(implicit tx: Tx): Boolean

  /** Generates a random `Double` value, uniformly distributed
    * between `0.0` (inclusive) and `1.0` (exclusive).
    */
  def nextDouble   ()(implicit tx: Tx): Double

  /** Generates a random `Float` value, uniformly distributed
    * between `0.0f` (inclusive) and `1.0f` (exclusive).
    */
  def nextFloat    ()(implicit tx: Tx): Float

  /** Generates a random `Int` value in the range `Int.MinValue` to `Int.MaxValue`. */
  def nextInt      ()(implicit tx: Tx): Int

  /** Generates a random `Int` value in the range of 0 (inclusive) until the specified value `n` (exclusive). */
  def nextInt(n: Int)(implicit tx: Tx): Int

  /** Generates a random `Long` value in the range `Long.MinValue` to `Long.MaxValue`.
    *
    * __WARNING:__
    * Because it uses the same algorithm as `java.util.Random`, with a seed of only 48 bits,
    * this function will not return all possible long values!
    */
  def nextLong     ()(implicit tx: Tx): Long

  /** Resets the internal seed value to the given argument. */
  def setSeed(seed: Long)(implicit tx: Tx): Unit
}