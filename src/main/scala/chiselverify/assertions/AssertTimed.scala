/*
* Copyright 2021 DTU Compute - Section for Embedded Systems Engineering
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
* or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package chiselverify.assertions

import chisel3._
import chiseltest._
import chiseltest.internal.TesterThreadList
import chiselverify.timing.TimedOp._
import chiselverify.timing._

object AssertTimed {
    def apply[T <: Module](dut: T, op: TimedOperator, message: String)
                          (delayType: DelayType): TesterThreadList = delayType match {
        case NoDelay => fork {
            assert(op(op.operand1.peek().litValue(), op.operand2.peek().litValue()), message)
        }

        case Always(delay) =>
            //Sample op1 at cycle 0
            val value1 = op.operand1.peek().litValue()
            assert(op(value1, op.operand2.peek().litValue()), message)

            //Check the same thing at every cycle
            fork {
                dut.clock.step()
                (1 until delay) foreach (_ => {
                    assert(op(value1, op.operand2.peek().litValue()), message)
                    dut.clock.step()
                })
            }

        case Exactly(delay) =>
            //Sample operand 1 at cycle 0
            val value1 = op.operand1.peek().litValue()

            //Check the same thing in exactly x cycles
            fork {
                dut.clock.step(delay)
                assert(op(value1, op.operand2.peek().litValue()), message)
            }

        case Eventually(delay) =>
            //Sample operand 1 at cycle 0
            val value1 = op.operand1.peek().litValue()
            val initRes = op(value1, op.operand2.peek().litValue())
            fork {
                assert((1 until delay).exists(_ => {
                    dut.clock.step()
                    op(value1, op.operand2.peek().litValue())
                }) || initRes, message)
            }

        case Never(delay) =>
            //Sample op1 at cycle 0
            val value1 = op.operand1.peek().litValue()
            assert(!op(value1, op.operand2.peek().litValue()), message)

            //Check the same thing at every cycle
            fork {
                dut.clock.step()
                (1 until delay) foreach (_ => {
                    assert(!op(value1, op.operand2.peek().litValue()), message)
                    dut.clock.step()
                })
            }
    }
    /* Checks for a condition to be valid in the circuit at all times, or within the specified amount of clock cycles.
      * If the condition evaluates to false, the circuit simulation stops with an error.
      *
      * This package is part of the special course "Verification of Digital Designs" on DTU, autumn semester 2020.
      *
      * Everytime the assertion is called it must be joined.
      *
      * @author Victor Alexander Hansen, s194027@student.dtu.dk
      * @author Niels Frederik Flemming Holm Frandsen, s194053@student.dtu.dk
      */
    def apply[T <: Module](dut: T, cond: () => Boolean, message: String)
                          (delayType: DelayType): TesterThreadList = delayType match {
        //Basic assertion
        case NoDelay => fork {
            assert(cond(), message)
        }

        //Checks for the argument condition to be true in the number of cycles passed
        case Always(delay) =>
            // Assertion for single thread clock cycle 0
            assert(cond(), message)
            fork {
                dut.clock.step(1)
                (1 until delay) foreach (_ => {
                    assert(cond(), message)
                    dut.clock.step(1)
                })
            }

        /* Checks for the argument condition to be true just once within the number of
         * clock cycles passed, a liveness property. Fails if the condition is not true
         * at least once within the window of cycles
         */
        case Eventually(delay) =>
            //Sample condition at cycle 0
            val initRes = cond()
            fork {
                assert((1 until delay).exists(_ => {
                    dut.clock.step()
                    cond()
                }) || initRes, message)
            }

        //Asserts the passed condition after stepping x clock cycles after the fork
        case Exactly(delay) =>
            fork {
                dut.clock.step(delay)
                assert(cond(), message)
            }

        //Checks for the argument condition to not be true in the number of cycles passed
        case Never(delay) =>
            // Assertion for single thread clock cycle 0
            assert(!cond(), message)
            fork {
                dut.clock.step(1)
                (1 until delay) foreach (_ => {
                    assert(!cond(), message)
                    dut.clock.step(1)
                })
            }

        /* Checks for the argument condition to be true within the number of
         * clock cycles passed, and hold true until the last cycle. Fails if the
         * condition is not true at least once within the window of cycles, or if
         * condition becomes false after it becomes true.
         */
        case EventuallyAlways(delay) =>
            fork {
                var i = 0
                while (!cond()) {
                    if (i == delay) {
                        assert(cond = false, message)
                    }
                    i += 1
                    dut.clock.step(1)
                }

                (0 until delay - i) foreach (_ => {
                    assert(cond(), message)
                    dut.clock.step(1)
                })
            }

        case _ => throw new IllegalArgumentException("Delay Type not implemented for assertions")
    }

    /* FANCY SYNTACTIC SUGAR BELOW */

    abstract class DT
    object Evt extends DT
    object Alw extends DT
    object Nvr extends DT
    object Exct extends DT

    /**
      * Thread Wrapper
      */
    case class TW(t: TesterThreadList) {
        def cycles: Boolean = {
            t.join()
            true
        }

    }

    implicit def TWtoThreadList(tw: TW): TesterThreadList = tw.t

    def dtToDelayType(dt: DT, delay: Int): DelayType = dt match {
        case Evt => Eventually(delay)
        case Alw => Always(delay)
        case Nvr => Never(delay)
        case Exct => Exactly(delay)
    }

    implicit def opToOption(op:TimedOperator): Option[TimedOperator] = Some(op)
    implicit def condToOption(cond: () => Boolean): Option[() => Boolean] = Some(cond)

    /**
      * Final Assertion type
      */
    case class FA[T <: Module](dut: T, dt : DT, op: Option[TimedOperator] = None, cond: Option[() => Boolean] = None) {
        def delay(ia: IA): TW = {
            val delayType = dtToDelayType(dt, ia.d)
            if(op.isDefined) {
                TW(apply(dut, op.get, s"ASSERTION ${delayType.toString} FAILED")(delayType))
            } else if(cond.isDefined) {
                TW(apply(dut, cond.get, s"ASSERTION ${delayType.toString} FAILED")(delayType))
            } else {
                throw new IllegalArgumentException("Either op or cond should be defined")
            }
        }
    }

    def by(d: Int): IA = IA(d)

    /**
      * Intermediate assertion type
      */
    case class IA(d: Int)

    def eventually[T <: Module](d: Int = 100)(op: TimedOperator)(implicit dut: T): Unit =
        FA(dut, Evt, op) delay by (d) cycles
    def always[T <: Module](d: Int = 100)(op: TimedOperator)(implicit dut: T): Unit =
        FA(dut, Alw, op) delay by (d) cycles
    def never[T <: Module](d: Int = 100)(op: TimedOperator)(implicit dut: T): Unit =
        FA(dut, Nvr, op) delay by (d) cycles
    def exact[T <: Module](d: Int = 100)(op: TimedOperator)(implicit dut: T): Unit =
        FA(dut, Exct, op) delay by (d) cycles

    def eventually[T <: Module](op: TimedOperator)(implicit dut: T): FA[T] = FA(dut, Evt, op)
    def always[T <: Module](op: TimedOperator)(implicit dut: T): FA[T] = FA(dut, Alw, op)
    def never[T <: Module](op: TimedOperator)(implicit dut: T): FA[T] = FA(dut, Nvr, op)
    def exact[T <: Module](op: TimedOperator)(implicit dut: T): FA[T] = FA(dut, Exct, op)

    def eventually[T <: Module](d: Int = 100)(cond: () => Boolean)(implicit dut: T): Unit =
        FA(dut, Evt, cond = cond) delay by (d) cycles
    def always[T <: Module](d: Int = 100)(cond: () => Boolean)(implicit dut: T): Unit =
        FA(dut, Alw, cond = cond) delay by (d) cycles
    def never[T <: Module](d: Int = 100)(cond: () => Boolean)(implicit dut: T): Unit =
        FA(dut, Nvr, cond = cond) delay by (d) cycles
    def exact[T <: Module](d: Int = 100)(cond: () => Boolean)(implicit dut: T): Unit =
        FA(dut, Exct, cond = cond) delay by (d) cycles

    def eventually[T <: Module](cond: () => Boolean)(implicit dut: T): FA[T] = FA(dut, Evt, cond = cond)
    def always[T <: Module](cond: () => Boolean)(implicit dut: T): FA[T] = FA(dut, Alw, cond = cond)
    def never[T <: Module](cond: () => Boolean)(implicit dut: T): FA[T] = FA(dut, Nvr, cond = cond)
    def exact[T <: Module](cond: () => Boolean)(implicit dut: T): FA[T] = FA(dut, Exct, cond = cond)
}
