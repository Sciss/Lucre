/*
 *  GraphBuilderMixin.scala
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

package de.sciss.lucre.expr
package impl

import java.util

import scala.collection.immutable.{IndexedSeq => Vec}
import scala.collection.mutable

trait GraphBuilderMixin extends Graph.Builder {
  protected final val controls  : mutable.Builder[Control, Vec[Control]]          = Vector.newBuilder[Control]
  protected final val properties: util.IdentityHashMap[Control, Map[String, Any]] = new util.IdentityHashMap

  protected def buildControls(): Vec[Control.Configured] = {
    val vecC = controls.result()
    val configured = vecC.map { c =>
      val m0 = properties.get(c)
      val m1 = if (m0 != null) m0 else Map.empty[String, Any]
      Control.Configured(c, m1)
    }
    configured
  }

  def build(): Graph = {
    val configured = buildControls()
    Graph(configured)
  }

  def addControl(c: Control): Unit = controls += c

  def putProperty(c: Control, key: String, value: Any): Unit = {
    val m0 = properties.get(c)
    val m1 = if (m0 != null) m0 else Map.empty[String, Any]
    val m2 = m1 + (key -> value)
    properties.put(c, m2)
  }
}