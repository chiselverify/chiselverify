package chiselverify.timing

import chisel3.Data

/**
  * Set of timing operators (only usable if a Timing delay is available)
  */
object TimedOp {

    /**
      * Represents a timed operation
      * @param operands the operands needed for the operation
      */
    abstract class TimedOperator(val operand1: Data, val operand2: Data) {
        /**
          * Executes the current operation
          * @param value1 the value of the first operand
          * @param value2 the value of the second operand at the wanted cycle
          * @return the result of the boolean operation
          */
        def compute(value1: BigInt, value2: BigInt): Boolean
    }

    case object NoOp extends TimedOperator(null, null) {
        override def compute(value1: BigInt, value2: BigInt): Boolean = true
    }

    /**
      * Checks if two operands are equal, given an certain timing delay
      * @param op1 the first operand
      * @param op2 the second operand
      */
    case class Equals(op1: Data, op2: Data) extends TimedOperator(op1, op2) {
        override def compute(value1: BigInt, value2: BigInt): Boolean = value1 == value2
    }

    /**
      * Greater than timed operator ( op1 > (op2 after x cycles) )
      * @param op1 the first operand
      * @param op2 the second operand
      */
    case class Gt(op1: Data, op2: Data) extends TimedOperator(op1, op2) {
        override def compute(value1: BigInt, value2: BigInt): Boolean = value1 > value2
    }

    /**
      * Less than timed operator ( op1 < (op2 after x cycles) )
      * @param op1 the first operand
      * @param op2 the second operand
      */
    case class Lt(op1: Data, op2: Data) extends TimedOperator(op1, op2) {
        override def compute(value1: BigInt, value2: BigInt): Boolean = value1 < value2
    }

    /**
      * Less than or Equal to timed operator ( op1 <= (op2 after x cycles) )
      * @param op1 the first operand
      * @param op2 the second operand
      */
    case class LtEq(op1: Data, op2: Data) extends TimedOperator(op1, op2) {
        override def compute(value1: BigInt, value2: BigInt): Boolean = value1 <= value2
    }

    /**
      * Greater than or equal to timed operator ( op1 >= (op2 after x cycles) )
      * @param op1 the first operand
      * @param op2 the second operand
      */
    case class GtEq(op1: Data, op2: Data) extends TimedOperator(op1, op2) {
        override def compute(value1: BigInt, value2: BigInt): Boolean = value1 >= value2
    }
}
