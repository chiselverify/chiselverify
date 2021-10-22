package chiselverify.assembly

import scala.math.{ceil, log10, pow}
import scala.util.Random

object Random {

  def rand(n: Int): Int = Random.nextInt(n)

  def rand(r: Range): Int = Random.nextInt(r.max-r.min) + r.min

  def randomSelect[T](xs: Seq[T]): T = xs.apply(rand(xs.length))

  def pow2(x: Int): Int = pow(2,x).toInt

  def log2Ceil(x: Int): Int = ceil( log10(x) / log10(2) ).toInt

  def randPow2(width: Width): Int = {
    width match {
      case Signed(w) => rand(pow2(w)) - pow2(w)/2
      case Unsigned(w) => rand(pow2(w))
    }
  }

  def width2Range(w: Width): Range = {
    w match {
      case Signed(w) => -pow2(w-1) until pow2(w-1)
      case Unsigned(w) => 0 until pow2(w)
    }
  }

  def fits(value: Int, w: Width): Boolean = width2Range(w).contains(value)

  class Counter {
    var c = 0
    def get(): Int = {
      c += 1
      return c-1
    }
  }


  //TODO
  def randSplit(value: Int)(w1: Width, w2: Width): (Int,Int) = {

    var random = 0
    var rest = 0
    var i = 0
    do {
      i += 1
      random = randPow2(w1)
      rest = value - random
    } while(!fits(rest,w2))

    println(s"split took $i iterations")

    (random,rest)
  }

}
