package chiselverify

import chisel3.Module

import chiselverify.assertions.AssertTimed

package object timing {
  
  /**
    * Defines a type of delay that can be applied to timed constructs
    * @param delay the number of cycles by which we want a delay
    */
  abstract class DelayType(val delay: Int) {
    def toString: String
  }

  /**
    * Specifies a timed relation with no delay (used for generics)
    */
  case object NoDelay extends DelayType(0)

  /**
    * This considers the opposite of always: No cycles within the given number of cycles should meet the requirements.
    * @param delay the number of cycles by which we want a delay
    */
  case class Never(override val delay: Int) extends DelayType(delay) {
    override def toString: String = s" WITH A NEVER DELAY OF $delay CYCLES"
  }

  /**
    * This considers EVERY cycle between the starting point and a given number of cycles later
    * @param delay the number of cycles by which we want a delay
    */
  case class Always(override val delay: Int) extends DelayType(delay) {
    override def toString: String = s" WITH AN ALWAYS DELAY OF $delay CYCLES"
  }

  /**
    * This considers ANY cycle between the starting point and a given number of cycles later
    * @param delay the number of cycles by which we want a delay
    */
  case class Eventually(override val delay: Int) extends DelayType(delay) {
    override def toString: String =s" WITH AN EVENTUAL DELAY OF $delay CYCLES"
  }

  /**
    * This ONLY considers the cycle that comes a given number of cycles later than the starting point
    * @param delay the number of cycles by which we want a delay
    */
  case class Exactly(override val delay: Int) extends DelayType(delay) {
    override def toString: String = s" WITH AN EXACT DELAY OF $delay CYCLES"
  }

  /**
    * This considers any cycle (between the starting point and a given number of cycles),
    * as well as every cycle that comes after it until the given number of cycles.
    * @param delay the number of cycles by which we want a delay
    */
  case class EventuallyAlways(override val delay: Int) extends DelayType(delay) {
    override def toString: String = s" WITH EVENTUALLY AN ALWAYS DELAY OF $delay CYCLES"
  }

  /**
    * Computes an Eventually Timed Assertion using the given condition on the currently implicit DUT
    * @param d the delay (default is 100 cycles)
    * @param msg the error message in case the assertion doesn't pass
    * @param cond the condition itself
    * @param dut an implicit DUT (defined in test function)
    * @tparam T the implicit DUT's type (also defined in test function)
    */
  def eventually[T <: Module](d: Int = 100, msg: String = s"EVENTUALLY ASSERTION FAILED")
                             (cond: => Boolean)(implicit dut: T) : Unit =
          AssertTimed(dut, cond, msg)(Eventually(d)).join()

  /**
    * Computes an Always Timed Assertion using the given condition on the currently implicit DUT
    * @param d the delay (default is 100 cycles)
    * @param msg the error message in case the assertion doesn't pass
    * @param cond the condition itself
    * @param dut an implicit DUT (defined in test function)
    * @tparam T the implicit DUT's type (also defined in test function)
    */
  def always[T <: Module](d: Int = 100,  msg: String = s"ALWAYS ASSERTION FAILED")
                         (cond: => Boolean)(implicit dut: T): Unit =
          AssertTimed(dut, cond, msg)(Always(d)).join()

  /**
    * Computes a Never Timed Assertion using the given condition on the currently implicit DUT
    * @param d the delay (default is 100 cycles)
    * @param msg the error message in case the assertion doesn't pass
    * @param cond the condition itself
    * @param dut an implicit DUT (defined in test function)
    * @tparam T the implicit DUT's type (also defined in test function)
    */
  def never[T <: Module](d: Int = 100,  msg: String = s"NEVER ASSERTION FAILED")
                        (cond: => Boolean)(implicit dut: T): Unit =
          AssertTimed(dut, cond, msg)(Never(d)).join()

  /**
    * Computes an Exactly Timed Assertion using the given condition on the currently implicit DUT
    * @param d the delay, which in this case is mandatory
    * @param msg the error message in case the assertion doesn't pass
    * @param cond the condition itself
    * @param dut an implicit DUT (defined in test function)
    * @tparam T the implicit DUT's type (also defined in test function)
    */
  def exact[T <: Module](d: Int,  msg: String = s"EXACTLY ASSERTION FAILED")
                        (cond: => Boolean)(implicit dut: T): Unit =
          AssertTimed(dut, cond, msg)(Exactly(d)).join()
}
