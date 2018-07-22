package de.sciss.lucre.expr

import de.sciss.lucre.aux.Aux.{Eq, Num, NumBool, NumDouble, NumFrac, NumInt, Ord, ToNum, Widen, Widen2, WidenToDouble}
import de.sciss.lucre.expr.graph.{Constant, BinaryOp => BinOp, UnaryOp => UnOp}

import scala.language.implicitConversions

object ExOps {
  implicit def constIntPat    (x: Int     ): Ex[Int     ] = Constant[Int    ](x)
  implicit def constDoublePat (x: Double  ): Ex[Double  ] = Constant[Double ](x)
  implicit def constBooleanPat(x: Boolean ): Ex[Boolean ] = Constant[Boolean](x)
  implicit def constStringPat (x: String  ): Ex[String  ] = Constant[String ](x)
  
  implicit def exOps[A](x: Ex[A]): ExOps[A] = new ExOps(x)
}
final class ExOps[A](private val x: Ex[A]) extends AnyVal {
  // unary element-wise

  def unary_-   (implicit num: Num[A]     ): Ex[A]         = UnOp(UnOp.Neg[A](), x)
  def unary_!   (implicit num: NumBool[A] ): Ex[A]         = UnOp(UnOp.Not[A](), x)
  def unary_~   (implicit num: NumInt[A]  ): Ex[A]         = UnOp(UnOp.BitNot[A](), x)
  def abs       (implicit num: Num[A]     ): Ex[A]         = UnOp(UnOp.Abs[A](), x)

  def toDouble  (implicit to: ToNum[A]): Ex[to.Double]     = UnOp(UnOp.ToDouble[A, to.Double]()(to), x)
  def toInt     (implicit to: ToNum[A]): Ex[to.Int]        = UnOp(UnOp.ToInt   [A, to.Int   ]()(to), x)

  def ceil      (implicit num: NumFrac[A] ): Ex[A]         = UnOp(UnOp.Ceil    [A](), x)
  def floor     (implicit num: NumFrac[A] ): Ex[A]         = UnOp(UnOp.Floor   [A](), x)
  def frac      (implicit num: NumFrac[A] ): Ex[A]         = UnOp(UnOp.Frac    [A](), x)
  def signum    (implicit num: Num[A]     ): Ex[A]         = UnOp(UnOp.Signum  [A](), x)
  def squared   (implicit num: Num[A]     ): Ex[A]         = UnOp(UnOp.Squared [A](), x)
  def cubed     (implicit num: Num[A]     ): Ex[A]         = UnOp(UnOp.Cubed   [A](), x)

  def sqrt   [B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Sqrt[A, B](), x)
  def exp    [B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Exp [A, B](), x)

  def reciprocal[B](implicit w: Widen[A, B], num: NumFrac[B]): Ex[B] = UnOp(UnOp.Reciprocal[A, B](), x)

  def midiCps  [B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Midicps   [A, B](), x)
  def cpsMidi  [B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Cpsmidi   [A, B](), x)
  def midiRatio[B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Midiratio [A, B](), x)
  def ratioMidi[B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Ratiomidi [A, B](), x)
  def dbAmp    [B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Dbamp     [A, B](), x)
  def ampDb    [B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Ampdb     [A, B](), x)

  def octCps   [B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Octcps    [A, B](), x)
  def cpsOct   [B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Cpsoct    [A, B](), x)
  def log      [B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Log       [A, B](), x)
  def log2     [B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Log2      [A, B](), x)
  def log10    [B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Log10     [A, B](), x)
  def sin      [B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Sin       [A, B](), x)
  def cos      [B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Cos       [A, B](), x)
  def tan      [B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Tan       [A, B](), x)
  def asin     [B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Asin      [A, B](), x)
  def acos     [B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Acos      [A, B](), x)
  def atan     [B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Atan      [A, B](), x)
  def sinh     [B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Sinh      [A, B](), x)
  def cosh     [B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Cosh      [A, B](), x)
  def tanh     [B](implicit wd: WidenToDouble[A, B]): Ex[B] = UnOp(UnOp.Tanh      [A, B](), x)

//  def rand      (implicit num: Num[A]       ): Ex[A]           = UnOp(UnOp.Rand  [A](), x)
//  def rand2     (implicit num: Num[A]       ): Ex[A]           = UnOp(UnOp.Rand2 [A](), x)
//  def coin      (implicit num: NumDouble[A] ): Ex[num.Boolean] = UnOp(UnOp.Coin  [A, num.Boolean]()(num), x)

  // binary

  def +         [A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: Num       [A2]): Ex[A2] = BinOp(BinOp.Plus     [A2](), x, that)
  def -         [A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: Num       [A2]): Ex[A2] = BinOp(BinOp.Minus    [A2](), x, that)
  def *         [A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: Num       [A2]): Ex[A2] = BinOp(BinOp.Times    [A2](), x, that)
  def /         [A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: NumFrac   [A2]): Ex[A2] = BinOp(BinOp.Div      [A2](), x, that)
  def %         [A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: Num       [A2]): Ex[A2] = BinOp(BinOp.ModJ        [A2](), x, that)
  def mod       [A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: Num       [A2]): Ex[A2] = BinOp(BinOp.Mod      [A2](), x, that)

  def sig_== (that: Ex[A])(implicit eq: Eq[A]): Ex[eq.Boolean] = BinOp(BinOp.Eq [A, eq.Boolean]()(eq), x, that)
  def sig_!= (that: Ex[A])(implicit eq: Eq[A]): Ex[eq.Boolean] = BinOp(BinOp.Neq[A, eq.Boolean]()(eq), x, that)

  def <  (that: Ex[A])(implicit ord: Ord[A]): Ex[ord.Boolean] = BinOp(BinOp.Lt [A, ord.Boolean]()(ord), x, that)
  def >  (that: Ex[A])(implicit ord: Ord[A]): Ex[ord.Boolean] = BinOp(BinOp.Gt [A, ord.Boolean]()(ord), x, that)
  def <= (that: Ex[A])(implicit ord: Ord[A]): Ex[ord.Boolean] = BinOp(BinOp.Leq[A, ord.Boolean]()(ord), x, that)
  def >= (that: Ex[A])(implicit ord: Ord[A]): Ex[ord.Boolean] = BinOp(BinOp.Geq[A, ord.Boolean]()(ord), x, that)

  def min       [A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: Num       [A2]): Ex[A2] = BinOp(BinOp.Min      [A2](), x, that)
  def max       [A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: Num       [A2]): Ex[A2] = BinOp(BinOp.Max      [A2](), x, that)

  def &   (that: Ex[A])(implicit num: NumInt[A]): Ex[A] = BinOp(BinOp.BitAnd[A](), x, that)
  def |   (that: Ex[A])(implicit num: NumInt[A]): Ex[A] = BinOp(BinOp.BitOr [A](), x, that)
  def ^   (that: Ex[A])(implicit num: NumInt[A]): Ex[A] = BinOp(BinOp.BitXor[A](), x, that)

  def lcm (that: Ex[A])(implicit num: NumInt[A]): Ex[A] = BinOp(BinOp.Lcm   [A](), x, that)
  def gcd (that: Ex[A])(implicit num: NumInt[A]): Ex[A] = BinOp(BinOp.Gcd   [A](), x, that)

  def roundTo   [A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: Num       [A2]): Ex[A2] = BinOp(BinOp.RoundTo  [A2](), x, that)
  def roundUpTo [A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: Num       [A2]): Ex[A2] = BinOp(BinOp.RoundUpTo[A2](), x, that)
  def trunc     [A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: Num       [A2]): Ex[A2] = BinOp(BinOp.Trunc    [A2](), x, that)

  def atan2     [A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: NumDouble [A2]): Ex[A2] = BinOp(BinOp.Atan2    [A2](), x, that)
  def hypot     [A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: NumDouble [A2]): Ex[A2] = BinOp(BinOp.Hypot    [A2](), x, that)
  def hypotApx  [A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: NumDouble [A2]): Ex[A2] = BinOp(BinOp.Hypotx   [A2](), x, that)
  def pow       [A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: NumDouble [A2]): Ex[A2] = BinOp(BinOp.Pow      [A2](), x, that)

  def <<  (that: Ex[A])(implicit num: NumInt[A]): Ex[A] = BinOp(BinOp.LeftShift         [A](), x, that)
  def >>  (that: Ex[A])(implicit num: NumInt[A]): Ex[A] = BinOp(BinOp.RightShift        [A](), x, that)
  def >>> (that: Ex[A])(implicit num: NumInt[A]): Ex[A] = BinOp(BinOp.UnsignedRightShift[A](), x, that)

  def difSqr[A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: Num[A2]): Ex[A2] = BinOp(BinOp.Difsqr[A2](), x, that)
  def sumSqr[A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: Num[A2]): Ex[A2] = BinOp(BinOp.Sumsqr[A2](), x, that)
  def sqrSum[A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: Num[A2]): Ex[A2] = BinOp(BinOp.Sqrsum[A2](), x, that)
  def sqrDif[A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: Num[A2]): Ex[A2] = BinOp(BinOp.Sqrdif[A2](), x, that)
  def absDif[A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: Num[A2]): Ex[A2] = BinOp(BinOp.Absdif[A2](), x, that)

  def clip2 [A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: Num[A2]): Ex[A2] = BinOp(BinOp.Clip2 [A2](), x, that)
  def excess[A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: Num[A2]): Ex[A2] = BinOp(BinOp.Excess[A2](), x, that)
  def fold2 [A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: Num[A2]): Ex[A2] = BinOp(BinOp.Fold2 [A2](), x, that)
  def wrap2 [A1, A2](that: Ex[A1])(implicit w: Widen2[A, A1, A2], num: Num[A2]): Ex[A2] = BinOp(BinOp.Wrap2 [A2](), x, that)

//  def linLin[A1, A2](inLo: Ex[A], inHi: Ex[A], outLo: Ex[A1], outHi: Ex[A1])
//                    (implicit w: Widen2[A, A1, A2], num: NumFrac[A2]): Ex[A2] =
//    LinLin[A, A1, A2](x, inLo = inLo, inHi = inHi, outLo = outLo, outHi = outHi)
//
//  def linExp[A1, A2](inLo: Ex[A], inHi: Ex[A], outLo: Ex[A1], outHi: Ex[A1])
//                    (implicit w: Widen2[A, A1, A2], num: NumDouble[A2]): Ex[A2] =
//    LinExp[A, A1, A2](x, inLo = inLo, inHi = inHi, outLo = outLo, outHi = outHi)
//
//  def expLin[A1, A2](inLo: Ex[A], inHi: Ex[A], outLo: Ex[A1], outHi: Ex[A1])
//                    (implicit w: Widen2[A, A1, A2], num: NumDouble[A2]): Ex[A2] =
//    ExpLin[A, A1, A2](x, inLo = inLo, inHi = inHi, outLo = outLo, outHi = outHi)
//
//  def expExp[A1, A2](inLo: Ex[A], inHi: Ex[A], outLo: Ex[A1], outHi: Ex[A1])
//                    (implicit w: Widen2[A, A1, A2], num: NumDouble[A2]): Ex[A2] =
//    ExpExp[A, A1, A2](x, inLo = inLo, inHi = inHi, outLo = outLo, outHi = outHi)
//
//  def poll(label: Ex[String] = "poll", gate: Ex[Boolean] = true): Ex[A] =
//    Poll(x, gate = gate, label = label)
}
