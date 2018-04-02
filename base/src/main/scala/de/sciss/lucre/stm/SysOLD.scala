//package de.sciss.lucre.stm
//
//import de.sciss.lucre.stm
//import de.sciss.serial.{DataInput, DataOutput, Serializer}
//
//import scala.concurrent.stm.InTxn
//
////trait Tx[T <: Tx] extends Tx {
////  final type Self = T
////}
//
//object Sys {
//  @inline
//  def newVar[A, T <: Tx](tx: T)(id: tx.Id, init: A)
//                                 (implicit serializer: Serializer[tx.Self, A]): tx.Var[A] =
//    tx.newVar[A](id, init)(serializer)
//}
//
//trait TestTx extends Tx {
//  final type Self   = TestTx
//  type Id     = TestId
//  type Acc    = Unit
//  type Var[A] = TestVar[A]
//
//  def newId(): Id = ???
//
//  def newVar[A](id: Id, init: A)(implicit serializer: Serializer[Self, A]): Var[A] =
//    new TestVar(id, init)(serializer)
//
//  def readVar[A](id: Id, in: DataInput)(implicit serializer: Serializer[Self, A]): Var[A] = ???
//
//  def readId(in: DataInput)(implicit acc: Acc): Id = ???
//}
//
//class TestTxImpl extends TestTx {
//  def self: Self = this
//
//  def newBooleanVar(id: TestId, init: Boolean): TestVar[Boolean] = ???
//
//  def newIntVar(id: TestId, init: Int): TestVar[Int] = ???
//
//  def newLongVar(id: TestId, init: Long): TestVar[Long] = ???
//
//  def newVarArray[A](size: Int): Array[TestVar[A]] = ???
//
//  def readBooleanVar(id: TestId, in: DataInput): TestVar[Boolean] = ???
//
//  def readIntVar(id: TestId, in: DataInput): TestVar[Int] = ???
//
//  def readLongVar(id: TestId, in: DataInput): TestVar[Long] = ???
//
//  def newHandle[A](value: A)(implicit serializer: Serializer[Self, A]): Source[Self, A] = ???
//
//  def beforeCommit(fun: Self => Unit): Unit = ???
//
//  def afterCommit(fun: => Unit): Unit = ???
//
//  def peer: InTxn = ???
//}
//
//class TestId extends Identifier[TestTx] {
//  def write(out: DataOutput): Unit = ???
//
//  def dispose()(implicit tx: TestTx): Unit = ???
//}
//
//class TestVar[A](id: TestId, init: A)(implicit serializer: Serializer[TestTx, A])
//  extends stm.Var[TestTx, A] {
//
//  private[this] var STATE = init
//
//  def write(out: DataOutput): Unit = serializer.write(STATE, out)
//
//  def update(v: A)(implicit tx: TestTx): Unit = {
//    STATE = v
//    serializer.write(STATE, null)
//  }
//
//  def apply()(implicit tx: TestTx): A = {
//    serializer.read(null, tx)(())
//  }
//
//  def dispose()(implicit tx: TestTx): Unit = ()
//}
//
//object TestSafety {
//  def main(args: Array[String]): Unit = {
//    val ser: Serializer[TestTx, Int] = implicitly[Serializer[TestTx, Int]]
//    val dOut = DataOutput()
//    val input = 33
//    ser.write(input, dOut)
//    val arr = dOut.toByteArray
//    val dIn = DataInput(arr)
//    val output = ser.read(dIn, new TestTxImpl)(())
//    assert(input == output)
//  }
//}