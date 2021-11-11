package chiselverify.assembly

import scala.math.{ceil, log10, pow}
import scala.util.Random

object RandomHelpers {

  def randomSelect[T](xs: Seq[T]): T = xs.apply(Random.nextInt(xs.length))

  /*
   * Generate random BigInt from bitwidth
   */
  def rand(n: Int): BigInt = BigInt(n, Random)

  /*
   * Generate random BigInt from max
   */
  def rand(max: BigInt): BigInt = {
    val r = rand(max.bitCount)
    if (r > max) rand(max) else r
  }

  def rand(r: BigRange): BigInt = rand(r.length) + r.min

  def rand(w: Width): BigInt = rand(BigRange(w))

  def pow2(x: Int): BigInt = BigInt(pow(2, x).toLong)

  def log2Ceil(x: Int): Int = ceil(log10(x) / log10(2)).toInt

  def fits(value: BigInt)(w: Width): Boolean = BigRange(w).contains(value)

  //TODO: write real random split
  def randSplit(value: BigInt)(w1: Width, w2: Width): (BigInt, BigInt) = {
    (rand(w1), rand(w2))
  }

  case class BigRange(min: BigInt, max: BigInt) {
    val length = max - min - 1

    def contains(that: BigInt): Boolean = (that > min) && (that < max)
  }

  object BigRange {
    def apply(w: Width): BigRange = {
      w match {
        case Signed(w) => BigRange(-pow2(w - 1), pow2(w - 1))
        case Unsigned(w) => BigRange(0, pow2(w))
      }
    }

    def apply(r: Range): BigRange = BigRange(r.min, r.max)

    def apply(v: Int): BigRange = BigRange(v, v)
  }

}
