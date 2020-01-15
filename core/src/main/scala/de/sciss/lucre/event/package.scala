/*
 *  package.scala
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

package de.sciss.lucre

import java.text.SimpleDateFormat
import java.util.{Date, Locale}

import de.sciss.lucre.stm.Base

import scala.annotation.elidable
import scala.annotation.elidable.CONFIG
import scala.collection.immutable.{IndexedSeq => Vec}

package object event {

  private[event] type Children[S <: Base[S]] = Vec[(Byte, Event[S, Any])]

  private[lucre] def NoChildren[S <: Base[S]]: Children[S] = Vector.empty

  private lazy val logHeader = new SimpleDateFormat("[d MMM yyyy, HH:mm''ss.SSS] 'Lucre' - 'evt' ", Locale.US)

  var showLog = false

  @elidable(CONFIG) private[event] def logEvent(what: => String): Unit =
    if (showLog) println(logHeader.format(new Date()) + what)
}