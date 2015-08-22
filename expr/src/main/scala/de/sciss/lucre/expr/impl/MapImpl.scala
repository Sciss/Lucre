/*
 *  MapImpl.scala
 *  (Lucre)
 *
 *  Copyright (c) 2009-2015 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.lucre.expr
package impl

import de.sciss.lucre.data.{Iterator, Ordering, SkipList}
import de.sciss.lucre.expr.Map.Modifiable
import de.sciss.lucre.stm.{Obj, Sys}
import de.sciss.lucre.{event => evt}
import de.sciss.serial.{DataInput, DataOutput, Serializer}

import scala.collection.immutable.{IndexedSeq => Vec}

object MapImpl {
  def apply[S <: Sys[S], K, V <: Obj[S]](implicit tx: S#Tx, keyType: Type.Expr[K]): Modifiable[S, K, V] = {
    val targets = evt.Targets[S]
    new Impl[S, K, V](targets) {
      val peer = SkipList.Map.empty[S, K, Vec[Elem[K, V]]](tx, keyOrdering, keyType.valueSerializer,
        Serializer.indexedSeq(elemSerializer))
    }
  }

  def serializer[S <: Sys[S], K, V <: Obj[S]](implicit keyType: Type.Expr[K]): Serializer[S#Tx, S#Acc, Map[S, K, V]] =
    new Ser[S, K, V]

  def modSerializer[S <: Sys[S], K, V <: Obj[S]](implicit keyType: Type.Expr[K]): Serializer[S#Tx, S#Acc, Modifiable[S, K, V]] =
    new ModSer[S, K, V]

  private class Ser[S <: Sys[S], K, V <: Obj[S]](implicit keyType: Type.Expr[K])
    extends Obj.Serializer[S, Map[S, K, V]] {

    def typeID: Int = Map.typeID

    def read(in: DataInput, access: S#Acc, targets: evt.Targets[S])(implicit tx: S#Tx): Map[S, K, V] =
      MapImpl.read(in, access, targets)
  }

  private class ModSer[S <: Sys[S], K, V <: Obj[S]](implicit keyType: Type.Expr[K])
    extends Obj.Serializer[S, Modifiable[S, K, V]] {

    def typeID: Int = Map.typeID

    def read(in: DataInput, access: S#Acc, targets: evt.Targets[S])(implicit tx: S#Tx): Modifiable[S, K, V] =
      MapImpl.read(in, access, targets)
  }

  def read[S <: Sys[S], K, V <: Obj[S]](in: DataInput, access: S#Acc)
                                       (implicit tx: S#Tx, keyType: Type.Expr[K]): Map[S, K, V] =
    modRead(in, access)  // currently the same

  def modRead[S <: Sys[S], K, V <: Obj[S]](in: DataInput, access: S#Acc)
                                          (implicit tx: S#Tx, keyType: Type.Expr[K]): Modifiable[S, K, V] = {
    val targets   = evt.Targets.read(in, access)
    val keyTypeID = in.readInt()
    if (keyTypeID != keyType.typeID) sys.error(s"Type mismatch. Expected key ${keyType.typeID} but found $keyTypeID")
    read(in, access, targets)
  }

  def readIdentifiedObj[S <: Sys[S]](in: DataInput, access: S#Acc)(implicit tx: S#Tx): Obj[S] = {
    val targets   = evt.Targets.read(in, access)
    val keyTypeID = in.readInt()
    val keyType   = Obj.getType(keyTypeID).asInstanceOf[Type.Expr[_]]
    read(in, access, targets)(tx, keyType)
  }

  private def read[S <: Sys[S], K, V <: Obj[S]](in: DataInput, access: S#Acc, targets: evt.Targets[S])
                                               (implicit tx: S#Tx, keyType: Type.Expr[K]): Impl[S, K, V] =
    new Impl[S, K, V](targets) {
      val peer = SkipList.Map.read[S, K, Vec[Elem[K, V]]](in, access)(tx, keyOrdering, keyType.valueSerializer,
        Serializer.indexedSeq(elemSerializer))
    }

  private final class Elem[K, V](val key: K, val value: V)
 
  private abstract class Impl[S <: Sys[S], K, V <: Obj[S]](protected val targets: evt.Targets[S])
                                                          (implicit val keyType: Type.Expr[K])
    extends Modifiable[S, K, V] with evt.impl.SingleNode[S, Map.Update[S, K, V]] {
    map =>

    // ---- abstract ----

    protected def peer: SkipList.Map[S, K, Vec[Elem[K, V]]]
    
    // ---- implemented ----

    implicit object keyOrdering extends Ordering[S#Tx, K] {
      def compare(a: K, b: K)(implicit tx: S#Tx): Int = {
        val ah = a.hashCode() // ##
        val bh = b.hashCode() // ##
        if (ah < bh) -1 else if (ah > bh) 1 else 0
      }
    }

    object elemSerializer extends Serializer[S#Tx, S#Acc, Elem[K, V]] {
      def read(in: DataInput, access: S#Acc)(implicit tx: S#Tx): Elem[K, V] = {
        val key   = keyType.valueSerializer.read(in)
        val value = Obj.read(in, access).asInstanceOf[V]
        new Elem[K, V](key, value)
      }

      def write(elem: Elem[K, V], out: DataOutput): Unit = {
        keyType.valueSerializer.write(elem.key, out)
        elem.value.write(out)
      }
    }

    final def typeID: Int = Map.typeID

    final def contains(key: K)(implicit tx: S#Tx): Boolean    = peer.get(key).exists(vec => vec.exists(_.key == key))
    final def get     (key: K)(implicit tx: S#Tx): Option[V]  = peer.get(key).flatMap { vec =>
      vec.find(_.key == key).map(_.value)
    }

    final def iterator(implicit tx: S#Tx): Iterator[S#Tx, (K, V)] = peer.iterator.flatMap {
      case (key, vec) => vec.map(elem => key -> elem.value)
    }
    final def keysIterator  (implicit tx: S#Tx): Iterator[S#Tx, K] = peer.valuesIterator.flatMap(_.map(_.key  ))
    final def valuesIterator(implicit tx: S#Tx): Iterator[S#Tx, V] = peer.valuesIterator.flatMap(_.map(_.value))

    final def size(implicit tx: S#Tx): Int = {
      // XXX TODO: a bit ugly...
      var res = 0
      peer.valuesIterator.foreach(res += _.size)
      res
    }
    final def nonEmpty(implicit tx: S#Tx): Boolean  = peer.nonEmpty
    final def isEmpty (implicit tx: S#Tx): Boolean  = peer.isEmpty

    final def modifiableOption: Option[Modifiable[S, K, V]] = Some(this)

    final protected def writeData(out: DataOutput): Unit = {
      out.writeInt(keyType.typeID)
      peer.write(out)
    }

    final protected def disposeData()(implicit tx: S#Tx): Unit = peer.dispose()

    override def toString = s"Map$id"
    
    final protected def foreach(fun: Elem[K, V] => Unit)(implicit tx: S#Tx): Unit =
      peer.valuesIterator.foreach(_.foreach(fun))

    object changed extends Changed
      with evt.impl.Generator[S, Map.Update[S, K, V]] with evt.impl.Root[S, Map.Update[S, K, V]]

    private[this] def fireAdded(key: K, value: V)(implicit tx: S#Tx): Unit =
      changed.fire(Map.Update(map, Vec(Map.Added(key, value))))

    private[this] def fireRemoved(key: K, value: V)(implicit tx: S#Tx): Unit =
      changed.fire(Map.Update(map, Vec(Map.Removed(key, value))))

    final def +=(kv: (K, V))(implicit tx: S#Tx): this.type = {
      put(kv._1, kv._2)
      this
    }

    final def -=(key: K)(implicit tx: S#Tx): this.type = {
      remove(key)
      this
    }

    final def put(key: K, value: V)(implicit tx: S#Tx): Option[V] = {
      val elem      = new Elem[K, V](key, value)
      val oldVec    = peer.get(key).getOrElse(Vector.empty)
      val idx       = oldVec.indexWhere(_.key == key)
      val found     = idx >= 0
      val newVec    = if (found) oldVec.updated(idx, elem) else oldVec :+ elem
      peer.add(key -> newVec)

      if (found) {
        val oldElem = oldVec(idx)
        // unregisterElement(oldElem)
        fireRemoved(key, oldElem.value)
      }
      // registerElement(elem)
      fireAdded(key, value)

      if (found) Some(oldVec(idx).value) else None
    }

    final def remove(key: K)(implicit tx: S#Tx): Option[V] = {
      val oldVec  = peer.get(key).getOrElse(Vector.empty)
      val idx     = oldVec.indexWhere(_.key == key)
      if (idx < 0) return None

      val elem    = oldVec(idx)
      val value   = elem.value
      val newVec  = oldVec.patch(idx, Nil, 1)
      if (newVec.isEmpty) {
        peer.remove(key)
      } else {
        peer.add(key -> newVec)
      }

      // unregisterElement(elem)
      fireRemoved(key, value)

      Some(value)
    }
  }
}