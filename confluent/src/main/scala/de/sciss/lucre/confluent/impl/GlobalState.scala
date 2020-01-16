/*
 *  GlobalState.scala
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
package impl

import de.sciss.lucre.data.Ancestor
import de.sciss.lucre.stm
import de.sciss.serial
import de.sciss.serial.{DataInput, DataOutput, Serializer}

private[impl] object GlobalState {
  private val SER_VERSION = 0x436F6E666C6E7400L  // "Conflnt\0"

  implicit def serializer[S <: Sys[S], D <: stm.DurableLike[D]]: serial.Serializer[D#Tx, D#Acc, GlobalState[S, D]] =
    new Ser[S, D]

  private final class Ser[S <: Sys[S], D <: stm.DurableLike[D]]
    extends serial.Serializer[D#Tx, D#Acc, GlobalState[S, D]] {

    def write(v: GlobalState[S, D], out: DataOutput): Unit = {
      import v._
      out.writeLong(SER_VERSION)
      out.writeInt(durRootId) // writeInt(durRootId)
      idCnt        .write(out)
      versionLinear.write(out)
      versionRandom.write(out)
      partialTree  .write(out)
    }

    def read(in: DataInput, acc: D#Acc)(implicit tx: D#Tx): GlobalState[S, D] = {
      val serVer = in.readLong()
      if (serVer != SER_VERSION)
        throw new IllegalStateException(s"Incompatible serialized version. Found $serVer but require $SER_VERSION")
      val durRootId     = in.readInt() // readInt()
      val idCnt         = tx.readCachedIntVar(in)
      val versionLinear = tx.readCachedIntVar(in)
      val versionRandom = tx.readCachedLongVar(in)
      val partialTree   = Ancestor.readTree[D, Long](in, acc)(tx, Serializer.Long, _.toInt)
      GlobalState[S, D](durRootId = durRootId, idCnt = idCnt, versionLinear = versionLinear,
        versionRandom = versionRandom, partialTree = partialTree)
    }
  }
}

private[impl] final case class GlobalState[S <: Sys[S], D <: stm.DurableLike[D]](
    durRootId: Int, idCnt: D#Var[Int], versionLinear: D#Var[Int], versionRandom: D#Var[Long],
    partialTree: Ancestor.Tree[D, Long])
