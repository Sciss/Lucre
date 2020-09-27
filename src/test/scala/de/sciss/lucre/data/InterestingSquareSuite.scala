/*
 *  InterestingSquareSuite.scala
 *  (Lucre 4)
 *
 *  Copyright (c) 2009-2020 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.lucre.data

import de.sciss.lucre.geom.{IntPoint2D => P, IntSquare => Q}
import org.scalatest.funspec.AnyFunSpec

/*

  To run only this suite

  testOnly de.sciss.lucre.data.InterestingSquareSuite

  */
class InterestingSquareSuite extends AnyFunSpec {
  val p: Q = Q(64, 64, 64)
  import p.{greatestInterestingP => gi}

  for (shift <- 0 to 5) {
    val ext = 1 << shift
    val q   = Q(ext, ext, ext)
    for (x1 <- 0 until ext; y1 <- 0 until ext; x2 <- 0 until ext; y2 <- 0 until ext) {
      assert(gi(P(x1, y1), P(x2 + ext, y2      )) === q)
      assert(gi(P(x1, y1), P(x2,       y2 + ext)) === q)
      assert(gi(P(x1, y1), P(x2 + ext, y2 + ext)) === q)

      assert(gi(P(x2 + ext, y2      ), P(x1, y1)) === q)
      assert(gi(P(x2,       y2 + ext), P(x1, y1)) === q)
      assert(gi(P(x2 + ext, y2 + ext), P(x1, y1)) === q)
    }
  }
}