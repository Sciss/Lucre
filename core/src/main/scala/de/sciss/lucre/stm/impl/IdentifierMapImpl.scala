/*
 *  IdentifierMapImpl.scala
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

package de.sciss.lucre.stm.impl

import de.sciss.lucre.stm.{IdentifierMap, TxnLike}

import scala.concurrent.stm.TMap

object IdentifierMapImpl {
  def newInMemoryIntMap[Id, Tx <: TxnLike, A](implicit intView: Id => Int): IdentifierMap[Id, Tx, A] =
    new InMemoryInt[Id, Tx, A](intView)

  private final class InMemoryInt[Id, Tx <: TxnLike, A](intView: Id => Int)
    extends IdentifierMap[Id, Tx, A] {

    private[this] val peer = TMap.empty[Int, A]

    def get(id: Id)(implicit tx: Tx): Option[A] = peer.get(intView(id))(tx.peer)

    def getOrElse(id: Id, default: => A)(implicit tx: Tx): A = get(id).getOrElse(default)

    def put(id: Id, value: A)(implicit tx: Tx): Unit =
      peer.put(intView(id), value)(tx.peer)

    def contains(id: Id)(implicit tx: Tx): Boolean = peer.contains(intView(id))(tx.peer)

    def remove(id: Id)(implicit tx: Tx): Unit =
      peer.remove(intView(id))(tx.peer)

    override def toString = s"IdentifierMap@${hashCode.toHexString}"

    // def write(out: DataOutput): Unit = ()

    def dispose()(implicit tx: Tx): Unit = ()
  }
}