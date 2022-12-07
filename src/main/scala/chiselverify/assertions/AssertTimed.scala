package chiselverify.assertions

import chisel3._
import chiseltest._
import chiseltest.internal.TesterThreadList
import chiselverify.timing.TimedOp._
import chiselverify.timing._

object AssertTimed {
  /** 
    * Creates a timed assertion on a DUT given a timed operator
    * @param dut the DUT to apply the assertion to
    * @param op the timed operator to apply
    * @param message the message to throw in case of assertion failure
    * @param delayType the delay type to use in the assertion
    */
  def apply[T <: Module](dut: T, op: TimedOperator, message: String)
                        (delayType: DelayType): TesterThreadList = {
    delayType match {
      case NoDelay => 
        fork {
          assert(op(op.operand1.peek().litValue, op.operand2.peek().litValue), message)
        }

      // Checks for the argument condition to be true in the number of cycles passed
      case Always(delay) =>
        // Sample op1 at cycle 0
        val value1 = op.operand1.peek().litValue
        assert(op(value1, op.operand2.peek().litValue), message)

        // Check the same thing at every cycle
        fork {
          dut.clock.step()
          (1 until delay).foreach (_ => {
            assert(op(value1, op.operand2.peek().litValue), message)
            dut.clock.step()
          })
        }

      case Exactly(delay) =>
        // Sample operand 1 at cycle 0
        val value1 = op.operand1.peek().litValue

        // Check the same thing in exactly `delay` cycles
        fork {
          dut.clock.step(delay)
          assert(op(value1, op.operand2.peek().litValue), message)
        }

      case Eventually(delay) =>
        // Sample operand 1 at cycle 0
        val value1 = op.operand1.peek().litValue
        val initRes = op(value1, op.operand2.peek().litValue)

        // Check that the result appears in `delay` cycles
        fork {
          assert((1 until delay).exists(_ => {
            dut.clock.step()
            op(value1, op.operand2.peek().litValue)
          }) || initRes, message)
        }

      case Never(delay) =>
        // Sample op1 at cycle 0
        val value1 = op.operand1.peek().litValue
        assert(!op(value1, op.operand2.peek().litValue), message)

        // Check the same thing at every cycle
        fork {
          dut.clock.step()
          (1 until delay) foreach (_ => {
            assert(!op(value1, op.operand2.peek().litValue), message)
            dut.clock.step()
          })
        }
      
      case _ => throw new IllegalArgumentException("Delay Type not implemented for assertions")
    }
  }
  
  /** 
    * Creates a timed assertion on a DUT given a Boolean condition
    * @param dut the DUT to apply the assertion to
    * @param cond the Boolean condition to apply
    * @param message the message to throw in case of assertion failure
    * @param delayType the delay type to use in the assertion
    */
  def apply[T <: Module](dut: T, cond: => Boolean, message: String)
                        (delayType: DelayType): TesterThreadList = {
    delayType match {
      case NoDelay => 
        fork {
          assert(cond, message)
        }

      // Checks for the argument condition to be true in the number of cycles passed
      case Always(delay) =>
        // Assertion for single thread clock cycle 0
        assert(cond, message)
        fork {
          dut.clock.step(1)
          (1 until delay) foreach (_ => {
            assert(cond, message)
            dut.clock.step(1)
          })
        }

      // Asserts the passed condition after stepping x clock cycles after the fork
      case Exactly(delay) =>
        fork {
          dut.clock.step(delay)
          assert(cond, message)
        }

      /** Checks for the argument condition to be true just once within the number of
        * clock cycles passed, a liveness property. Fails if the condition is not true
        * at least once within the window of cycles
        */
      case Eventually(delay) =>
        // Sample condition at cycle 0
        val initRes = cond
        fork {
          assert((1 until delay).exists(_ => {
            dut.clock.step()
            cond
          }) || initRes, message)
        }

      // Checks for the argument condition to not be true in the number of cycles passed
      case Never(delay) =>
        // Assertion for single thread clock cycle 0
        assert(!cond, message)
        fork {
          dut.clock.step(1)
          (1 until delay) foreach (_ => {
            assert(!cond, message)
            dut.clock.step(1)
          })
        }

      /** Checks for the argument condition to be true within the number of
        * clock cycles passed, and hold true until the last cycle. Fails if the
        * condition is not true at least once within the window of cycles, or if
        * condition becomes false after it becomes true.
        */
      case EventuallyAlways(delay) =>
        fork {
          var start = 0
          while (!cond) {
            if (start == delay) {
              assert(cond = false, message)
            }
            start += 1
            dut.clock.step(1)
          }
          
          (start until delay) foreach (_ => {
            assert(cond, message)
            dut.clock.step(1)
          })
        }

      case _ => throw new IllegalArgumentException("Delay Type not implemented for assertions")
    }
  }
}
