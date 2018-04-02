/*
 *  Ordering.scala
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

package de.sciss.lucre.data

object Ordering {
  implicit object Int extends Ordering[Any, scala.Int] {
    def compare        (a: scala.Int, b: scala.Int)(implicit tx: Any): scala.Int  = if (a < b) -1 else if (a > b) 1 else 0
    override def lt    (a: scala.Int, b: scala.Int)(implicit tx: Any): Boolean    = a < b
    override def lteq  (a: scala.Int, b: scala.Int)(implicit tx: Any): Boolean    = a <= b
    override def gt    (a: scala.Int, b: scala.Int)(implicit tx: Any): Boolean    = a > b
    override def gteq  (a: scala.Int, b: scala.Int)(implicit tx: Any): Boolean    = a >= b
    override def equiv (a: scala.Int, b: scala.Int)(implicit tx: Any): Boolean    = a == b
    override def nequiv(a: scala.Int, b: scala.Int)(implicit tx: Any): Boolean    = a != b
    override def max   (a: scala.Int, b: scala.Int)(implicit tx: Any): scala.Int  = if (a >= b) a else b
    override def min   (a: scala.Int, b: scala.Int)(implicit tx: Any): scala.Int  = if (a < b) a else b
  }

  implicit object Float extends Ordering[Any, scala.Float] {
    def compare        (a: scala.Float, b: scala.Float)(implicit tx: Any): scala.Int    = if (a < b) -1 else if (a > b) 1 else 0
    override def lt    (a: scala.Float, b: scala.Float)(implicit tx: Any): Boolean      = a < b
    override def lteq  (a: scala.Float, b: scala.Float)(implicit tx: Any): Boolean      = a <= b
    override def gt    (a: scala.Float, b: scala.Float)(implicit tx: Any): Boolean      = a > b
    override def gteq  (a: scala.Float, b: scala.Float)(implicit tx: Any): Boolean      = a >= b
    override def equiv (a: scala.Float, b: scala.Float)(implicit tx: Any): Boolean      = a == b
    override def nequiv(a: scala.Float, b: scala.Float)(implicit tx: Any): Boolean      = a != b
    override def max   (a: scala.Float, b: scala.Float)(implicit tx: Any): scala.Float  = if (a >= b) a else b
    override def min   (a: scala.Float, b: scala.Float)(implicit tx: Any): scala.Float  = if (a < b) a else b
  }

  implicit object Long extends Ordering[Any, scala.Long] {
    def compare        (a: scala.Long, b: scala.Long)(implicit tx: Any): scala.Int  = if (a < b) -1 else if (a > b) 1 else 0
    override def lt    (a: scala.Long, b: scala.Long)(implicit tx: Any): Boolean    = a < b
    override def lteq  (a: scala.Long, b: scala.Long)(implicit tx: Any): Boolean    = a <= b
    override def gt    (a: scala.Long, b: scala.Long)(implicit tx: Any): Boolean    = a > b
    override def gteq  (a: scala.Long, b: scala.Long)(implicit tx: Any): Boolean    = a >= b
    override def equiv (a: scala.Long, b: scala.Long)(implicit tx: Any): Boolean    = a == b
    override def nequiv(a: scala.Long, b: scala.Long)(implicit tx: Any): Boolean    = a != b
    override def max   (a: scala.Long, b: scala.Long)(implicit tx: Any): scala.Long = if (a >= b) a else b
    override def min   (a: scala.Long, b: scala.Long)(implicit tx: Any): scala.Long = if (a < b) a else b
  }

  implicit object Double extends Ordering[Any, scala.Double] {
    def compare        (a: scala.Double, b: scala.Double)(implicit tx: Any): scala.Int    = if (a < b) -1 else if (a > b) 1 else 0
    override def lt    (a: scala.Double, b: scala.Double)(implicit tx: Any): Boolean      = a < b
    override def lteq  (a: scala.Double, b: scala.Double)(implicit tx: Any): Boolean      = a <= b
    override def gt    (a: scala.Double, b: scala.Double)(implicit tx: Any): Boolean      = a > b
    override def gteq  (a: scala.Double, b: scala.Double)(implicit tx: Any): Boolean      = a >= b
    override def equiv (a: scala.Double, b: scala.Double)(implicit tx: Any): Boolean      = a == b
    override def nequiv(a: scala.Double, b: scala.Double)(implicit tx: Any): Boolean      = a != b
    override def max   (a: scala.Double, b: scala.Double)(implicit tx: Any): scala.Double = if (a >= b) a else b
    override def min   (a: scala.Double, b: scala.Double)(implicit tx: Any): scala.Double = if (a < b) a else b
  }

  implicit def fromMath[A](implicit underlying: math.Ordering[A]): Ordering[Any, A] =
    new MathWrapper[A](underlying)

  implicit def fromOrdered[T, A <: Ordered[T, A]]: Ordering[T, A] = new OrderedWrapper[T, A]

  private final class MathWrapper[A](underlying: math.Ordering[A]) extends Ordering[Any, A] {
    def compare(a: A, b: A)(implicit tx: Any): Int = underlying.compare(a, b)
  }

  private final class OrderedWrapper[T, A <: Ordered[T, A]] extends Ordering[T, A] {
    def compare(a: A, b: A)(implicit tx: T): Int = a.compare(b)
  }
}

trait Ordering[-T, A] {
  def compare(a: A, b: A)(implicit tx: T): Int
  def lt     (a: A, b: A)(implicit tx: T): Boolean  = compare(a, b)  < 0
  def lteq   (a: A, b: A)(implicit tx: T): Boolean  = compare(a, b) <= 0
  def gt     (a: A, b: A)(implicit tx: T): Boolean  = compare(a, b)  > 0
  def gteq   (a: A, b: A)(implicit tx: T): Boolean  = compare(a, b) >= 0
  def equiv  (a: A, b: A)(implicit tx: T): Boolean  = compare(a, b) == 0
  def nequiv (a: A, b: A)(implicit tx: T): Boolean  = compare(a, b) != 0
  def max    (a: A, b: A)(implicit tx: T): A        = if (compare(a, b) >= 0) a else b
  def min    (a: A, b: A)(implicit tx: T): A        = if (compare(a, b)  < 0) a else b
}