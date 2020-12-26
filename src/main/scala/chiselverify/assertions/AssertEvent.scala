/*
 * Copyright 2020 DTU Compute - Section for Embedded Systems Engineering
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
import chiselverify.timing._

/* Checks for a condition to be valid in the circuit at all times, or within the specified amount of clock cycles.
 * If the condition evaluates to false, the circuit simulation stops with an error.
 *
 * This package is part of the special course "Verification of Digital Designs" on DTU, autumn semester 2020.
 *
 * @author Victor Alexander Hansen, s194027@student.dtu.dk
 * @author Niels Frederik Flemming Holm Frandsen, s194053@student.dtu.dk
 *
 * @experimental
 */
object AssertEvent {
  def apply[T <: Module](
    dut:       T,
    cond:      () => Boolean = () => true,
    event:     () => Boolean = () => false,
    message:   String = "Assertion Error"
  )(eventType: EventType
  ): TesterThreadList = eventType match {

    //Checks for the argument condition to be true in the number of cycles passed
    case Always =>
      // Assertion for single thread clock cycle 0
      assert(cond(), message)
      fork {
        dut.clock.step(1)
        while (!event()) {
          assert(cond(), message)
          dut.clock.step(1)
        }
      }

    //Checks for the argument event to be true just once before the condition is valid
    case Eventually =>
      fork {
        while (!cond()) {
          if (event()) {
            assert(cond = false, message)
          }
          dut.clock.step(1)
        }
      }

    //Checks for the argument condition to not be true in the number of cycles passed
    case Never =>
      // Assertion for single thread clock cycle 0
      assert(!cond(), message)
      fork {
        dut.clock.step(1)
        while (!event()) {
          assert(!cond(), message)
          dut.clock.step(1)
        }
      }

    case EventuallyAlways =>
      fork {
        while (!cond()) {
          if (event()) {
            assert(cond = false, message)
          }
          dut.clock.step(1)
        }

        while (!event()) {
          assert(cond(), message)
          dut.clock.step(1)
        }
      }

    case _ => throw new IllegalArgumentException("Event Type not implemented for assertions")
  }
}
