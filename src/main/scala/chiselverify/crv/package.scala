package chiselverify

import chiselverify.crv.backends.jacop.{WeightedRange, WeightedValue}

package object crv {
  
  final case class CRVException(private val message: String = "", private val cause: Throwable = None.orNull)
    extends Exception(message, cause)
  
  /** 
    * Generates a random string of alphanumeric characters
    * @param length the length of the wanted string
    * @param seed the seed to the random nunber generator
    * @todo perhaps replace with scala.util.Random.alphanumeric
    */
  def randName(length: Int, seed: Int) : String = {
    val rand = new scala.util.Random(seed)
    val res: StringBuilder = new StringBuilder()
    (0 until length).foreach(_ => {
        val randNum = rand.nextInt(122 - 48) + 48
        res += randNum.toChar
    })
    res.mkString
  }

  /**
    * Implicit class to allow declaring distribution constraints like 1 := 10
    * @param value the current value
    */
  implicit class ValueBinder(value: Int) {
    def :=(weight: Int): WeightedValue = {
      WeightedValue(value, weight)
    }
  }

  /**
    * Implicit class to allow declaring distribution constraints like (1 to 10) := 10
    * @param value the current value
    */
  implicit class RangeBinder(value: scala.collection.immutable.Range) {
    def := (weight: Int): WeightedRange ={
      WeightedRange(value, weight)
    }
  }
}
