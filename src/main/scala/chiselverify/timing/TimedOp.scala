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
package chiselverify.timing

import chisel3.Data
import chisel3.tester.testableData

/**
  * Set of timing operators (only usable if a Timing delay is available)
  */
object TimedOp {

    /**
      * Represents a timed operation
      * @param operands the operands needed for the operation
      */
    abstract class TimedOperator(operands: Data*) {
        val operand1: Data = operands.head
        val operand2: Data = operands.tail.head
        /**
          * Executes the current operation
          * @param value1 the value of the first operand
          * @param value2 the value of the second operand at the wanted cycle
          * @return the result of the boolean operation
          */
        def apply(value1: BigInt, value2: BigInt): Boolean

        /**
          * Defines a new TimedOperator as the sum of two existing ones
          * @param that the Timed operator that we want to sum to ours
          * @return an operator that considers a result true if it satisfies both operators
          */
        def +(that: TimedOperator): TimedOperator = {
            val compute1 = this(operand1.peek().litValue, operand2.peek().litValue)
            val compute2 = that(that.operand1.peek().litValue, that.operand2.peek().litValue)

            new TimedOperator(operand1, operand2, that.operand1, that.operand2) {
                override def apply(value1: BigInt, value2: BigInt): Boolean = compute1 && compute2
            }
        }
    }

    case object NoOp extends TimedOperator(null, null) {
        override def apply(value1: BigInt, value2: BigInt): Boolean = true
    }

    /**
      * Checks if two operands are equal, given an certain timing delay
      * @param op1 the first operand
      * @param op2 the second operand
      */
    case class Equals(op1: Data, op2: Data) extends TimedOperator(op1, op2) {
        override def apply(value1: BigInt, value2: BigInt): Boolean = value1 == value2
    }

    /**
      * Greater than timed operator ( op1 > (op2 after x cycles) )
      * @param op1 the first operand
      * @param op2 the second operand
      */
    case class Gt(op1: Data, op2: Data) extends TimedOperator(op1, op2) {
        override def apply(value1: BigInt, value2: BigInt): Boolean = value1 > value2
    }

    /**
      * Less than timed operator ( op1 < (op2 after x cycles) )
      * @param op1 the first operand
      * @param op2 the second operand
      */
    case class Lt(op1: Data, op2: Data) extends TimedOperator(op1, op2) {
        override def apply(value1: BigInt, value2: BigInt): Boolean = value1 < value2
    }

    /**
      * Less than or Equal to timed operator ( op1 <= (op2 after x cycles) )
      * @param op1 the first operand
      * @param op2 the second operand
      */
    case class LtEq(op1: Data, op2: Data) extends TimedOperator(op1, op2) {
        override def apply(value1: BigInt, value2: BigInt): Boolean = value1 <= value2
    }

    /**
      * Greater than or equal to timed operator ( op1 >= (op2 after x cycles) )
      * @param op1 the first operand
      * @param op2 the second operand
      */
    case class GtEq(op1: Data, op2: Data) extends TimedOperator(op1, op2) {
        override def apply(value1: BigInt, value2: BigInt): Boolean = value1 >= value2
    }

    /* MORE FANCY SYNTACTIC SUGAR BELOW */

    /**
      * Internal data wrapper
      */
    case class ID(data: Data) {
        def ?==(that: Data): Equals = Equals(data, that)
        def ?<(that: Data): Lt = Lt(data, that)
        def ?<=(that: Data): LtEq = LtEq(data, that)
        def ?>(that: Data): Gt = Gt(data, that)
        def ?>=(that: Data): GtEq = GtEq(data, that)
    }

    implicit def dataToID(data: Data): ID = ID(data)
}
