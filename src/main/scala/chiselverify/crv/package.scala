package chiselverify

import chiselverify.crv.backends.jacop.{WeightedRange, WeightedValue}
import chiselverify.crv.backends.jacop.experimental.WeightedRange

package object crv {
  final case class CRVException(private val message: String = "", private val cause: Throwable = None.orNull)
    extends Exception(message, cause)

  /**
    * Implicit class to allow declaring distribution constraints like
    * 1 := 10
    * @param value the current value
    */
  implicit class ValueBinder(value: Int) {
    def :=(weight: Int): WeightedValue = {
      WeightedValue(value, weight)
    }
  }

  /**
    * Implicit class to allow declaring distribution constraints like
    * (1 to 10) := 10
    * @param value the current value
    */
  implicit class RangeBinder(value: scala.collection.immutable.Range) {
    def := (weight: Int): WeightedRange ={
      WeightedRange(value, weight)
    }
  }
}
