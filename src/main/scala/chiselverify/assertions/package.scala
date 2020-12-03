package chiselverify

import chisel3._

package object assertions {

    /* Checks for a condition to be valid in the circuit at all times, or within the specified amount of clock cycles.
      * If the condition evaluates to false, the circuit simulation stops with an error.
      *
      * @param dut the design under test
      * @param cond optional, condition, assertion fires (simulation fails) when false. Is passed as an anonymous function
      * @param cycles optional, amount of clock cycles for which the assertion is checked, instead of an immediate assertion
      * @param message optional, format string to print when assertion fires
      * @param event optional, a trigger event that will signal the end of the assertion
      * @param signal optional, an unsigned integer which serves as part of the condition
      *
      * This package is part of the special course "Verification of Digital Designs" on DTU, autumn semester 2020.
      *
      * @author Victor Alexander Hansen, s194027@student.dtu.dk
      * @author Niels Frederik Flemming Holm Frandsen, s194053@student.dtu.dk
      */

    /** assertOneHot():
      * checks if exactly one bit of the expression is high
      * This can be combined with any of the other assertions
      * because it returns a boolean value.
      */
    object AssertOneHot {
        def apply(signal: UInt = "b0001".U, message: String = "Error") : Boolean = {

            var in = signal.litValue
            var i = 0

            while (in > 0) {
                if ((in & 1) == 1) {
                    i = i + 1
                }
                in = in >> 1
                if (i > 1) {
                    assert(cond = false, message)
                }
            }

            true
        }
    }
}
