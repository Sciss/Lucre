/*
 *  UndoManagerImpl.scala
 *  (Lucre)
 *
 *  Copyright (c) 2009-2019 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Lesser General Public License v2.1+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.lucre.stm.impl

import de.sciss.lucre.event.impl.ObservableImpl
import de.sciss.lucre.stm.TxnLike.peer
import de.sciss.lucre.stm.UndoManager.Update
import de.sciss.lucre.stm.{Sys, UndoManager, UndoableEdit}

import scala.concurrent.stm.Ref

object UndoManagerImpl {
  def apply[S <: Sys[S]](): UndoManager[S] =
    new Impl[S]

  private final class Impl[S <: Sys[S]]
    extends UndoManager[S] with ObservableImpl[S, UndoManager.Update[S]] { impl =>

    private[this] val toUndo    = Ref[List[UndoableEdit[S]]](Nil)
    private[this] val toRedo    = Ref[List[UndoableEdit[S]]](Nil)

    private[this] val _undoName = Ref(Option.empty[String])
    private[this] val _redoName = Ref(Option.empty[String])

    //    private[this] val busy        = Ref(false)
    private[this] val _blockMerge = Ref(false)

    def blockMerge()(implicit tx: S#Tx): Unit =
      _blockMerge() = true

    private[this] val Empty = new Compound[S]("", Nil, significant = false)

    // add a non-significant edit puts it into pending limbo,
    // because we do not yet want to purge the redo tree at this stage
    private[this] val pending = Ref(Empty)

    def addEdit(edit: UndoableEdit[S])(implicit tx: S#Tx): Unit = {
      if (edit.significant) {
        val undoNameOld = _undoName()
        val toUndoOld   = toUndo()
        val canUndoOld  = toUndoOld.nonEmpty
        val _pending    = pending.swap(Empty)
        val toUndoTmp   = if (_pending.isEmpty) toUndoOld else _pending :: toUndoOld

        val toUndoNew = toUndoTmp match {
          case head :: tail if !_blockMerge() =>
            head.tryMerge(edit) match {
              case Some(merged) => merged :: tail
              case None         => edit   :: toUndoTmp
            }

          case _ => edit :: toUndoTmp
        }
        toUndo()        = toUndoNew
        val undoNameNew = toUndoNew.head.name
        _undoName()     = Some(undoNameNew)
        val toRedoOld   = toRedo.swap(Nil)
        val canRedoOld  = toRedoOld.nonEmpty
        _redoName()     = None
        toRedoOld.foreach(_.dispose())

        val _fire = !canUndoOld || canRedoOld || (canUndoOld && !undoNameOld.contains(undoNameNew))
        if (_fire) fire(Update(impl, undoName = Some(undoNameNew), redoName = None)) // notifyObservers()

      } else {
        if (canUndo) pending.transform(_.merge(edit))
      }

      _blockMerge() = false
    }

    def canUndo(implicit tx: S#Tx): Boolean = toUndo().nonEmpty

    def undo()(implicit tx: S#Tx): Unit = {
      val _pending = pending.swap(Empty)
      if (_pending.nonEmpty) {
        _pending.undo()
      } else {
        toUndo() match {
          case action :: tail =>
            action.undo()
            val toRedoOld   = toRedo.getAndTransform(action :: _)
            val canRedoOld  = toRedoOld.nonEmpty
            val undoNameOld = _undoName()
            val redoNameOld = _redoName()
            toUndo() = tail
            val canUndoNew  = tail.nonEmpty

            if (action.significant) {
              _redoName()   = Some(action.name)
              _undoName()   = tail match {
                case head :: _ if head.significant  => Some(head.name)
                case _ :: pen :: _                  => Some(pen .name)
                case _                              => None
              }
            }

            val _fire = !canUndoNew || !canRedoOld || _undoName() != undoNameOld || _redoName() != redoNameOld
            blockMerge()
            if (_fire) fire(Update(impl, undoName = _undoName(), redoName = _redoName()))

          case _ =>
            throw new IllegalStateException("Nothing to undo")
        }
      }
    }

    def undoName(implicit tx: S#Tx): String = _undoName().get

    def canRedo(implicit tx: S#Tx): Boolean = toRedo().nonEmpty

    def redo()(implicit tx: S#Tx): Unit = {
      val _pending = pending.swap(Empty)
      if (_pending.nonEmpty) {
        _pending.undo()
      }
      toRedo() match {
        case action :: tail =>
          action.redo()
          val toUndoOld   = toUndo.getAndTransform(action :: _)
          val canUndoOld  = toUndoOld.nonEmpty
          val undoNameOld = _undoName()
          val redoNameOld = _redoName()
          toRedo()        = tail
          val canRedoNew  = tail.nonEmpty
          if (action.significant) {
            _undoName()   = Some(action.name)
            _redoName()   = tail match {
              case head :: _ if head.significant  => Some(head.name)
              case _ :: pen :: _                  => Some(pen .name)
              case _                              => None
            }
          }

          val _fire = !canRedoNew || !canUndoOld || _undoName() != undoNameOld || _redoName() != redoNameOld
          blockMerge()
          if (_fire) fire(Update(impl, undoName = _undoName(), redoName = _redoName()))

        case _ =>
          throw new IllegalStateException("Nothing to redo")
      }
    }

    def redoName(implicit tx: S#Tx): String = _redoName().get

    def clear()(implicit tx: S#Tx): Unit = {
      if (clearNoFire()) fire(Update(impl, undoName = None, redoName = None))
    }

    private def clearNoFire()(implicit tx: S#Tx): Boolean = {
      val hadUndo = toUndo.swap(Nil).nonEmpty
      val _redo   = toRedo.swap(Nil)
      val hadRedo = _redo.nonEmpty
      _redo.foreach(_.dispose())
      hadUndo || hadRedo
    }

    def dispose()(implicit tx: S#Tx): Unit = {
      clearNoFire()
    }
  }

  private class Compound[S <: Sys[S]](val name: String, val edits: List[UndoableEdit[S]], val significant: Boolean)
    extends UndoableEdit[S] {

    def isEmpty : Boolean = edits.isEmpty
    def nonEmpty: Boolean = edits.nonEmpty

    def dispose()(implicit tx: S#Tx): Unit =
      edits.foreach(_.dispose())

    private def mergeEdits(succ: List[UndoableEdit[S]])(implicit tx: S#Tx): List[UndoableEdit[S]] =
      (succ, edits) match {
        case (init :+ succLast, head :: tail) =>
          head.tryMerge(succLast) match {
            case Some(merge)  => init ::: merge :: tail
            case None         => succ ::: edits
          }

        case _ => succ ::: edits
      }

    def merge(succ: UndoableEdit[S])(implicit tx: S#Tx): Compound[S] = {
      val succEdits = succ match {
        case a: Compound[S] => a.edits
        case _              => succ :: Nil
      }
      val newEdits = mergeEdits(succEdits)
      new Compound(name, newEdits, significant = significant || succ.significant)
    }

    def undo()(implicit tx: S#Tx): Unit =
      edits.foreach(_.undo())

    def redo()(implicit tx: S#Tx): Unit =
      edits.reverse.foreach(_.redo())

    def tryMerge(succ: UndoableEdit[S])(implicit tx: S#Tx): Option[UndoableEdit[S]] = succ match {
      case that: Compound[S] if !this.significant && !that.significant =>
        val newEdits = mergeEdits(that.edits)
        val m = new Compound(name, edits = newEdits, significant = significant)
        Some(m)

      case _ => None
    }
  }
}