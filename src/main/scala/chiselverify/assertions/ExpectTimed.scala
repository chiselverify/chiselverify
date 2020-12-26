package chiselverify.assertions

import chisel3._
import chiseltest._
import chiseltest.internal.TesterThreadList
import chiselverify.timing._

object ExpectTimed {
  def apply[T <: Module](
    dut:         T,
    port:        Data,
    expectedVal: UInt,
    message:     String = "Assertion Error"
  )(delayType:   DelayType = NoDelay
  ): TesterThreadList = delayType match {
    //Basic assertion
    case NoDelay =>
      fork {
        port.expect(expectedVal, message)
      }

    //Checks for the argument condition to be true in the number of cycles passed
    case Always(delay) =>
      // Assertion for single thread clock cycle 0
      port.expect(expectedVal)
      fork {
        dut.clock.step(1)
        (1 until delay).foreach(_ => {
          port.expect(expectedVal, message)
          dut.clock.step(1)
        })
      }

    /* Checks for the argument condition to be true just once within the number of
     * clock cycles passed, a liveness property. Fails if the condition is not true
     * at least once within the window of cycles
     */
    case Eventually(delay) =>
      //TODO Why test the condition at cycle 0? port.expect(expectedVal, message)
      fork {
        dut.clock.step(1)
        for {
          i <- 0 until delay
          if (port.peek().asUInt().litValue() != expectedVal.litValue())
        } {
          if (i == (delay - 1)) sys.error(message)
          else dut.clock.step(1)
        }
      }

    //Asserts the passed condition after stepping x clock cycles after the fork
    case Exactly(delay) =>
      fork {
        dut.clock.step(delay)
        port.expect(expectedVal, message)
      }

    //Checks for the argument condition to not be true in the number of cycles passed
    case Never(delay) =>
      // Assertion for single thread clock cycle 0
      if (port.peek().asUInt().litValue() == expectedVal.litValue()) sys.error(message)
      fork {
        dut.clock.step(1)
        (1 until delay).foreach(_ => {
          if (port.peek().asUInt().litValue() == expectedVal.litValue()) sys.error(message)
          dut.clock.step(1)
        })
      }

    case _ => throw new IllegalArgumentException("Delay Type not implemented for expects")
  }
}
