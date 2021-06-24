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
    abstract class TimedOperator(val operand1: Data, val operand2: Data)

    case object NoOp extends TimedOperator(null, null)

    /**
      * Checks if two operands are equal, given an certain timing delay
      * @param op1 the first operand
      * @param op2 the second operand
      */
    case class Equals(op1: Data, op2: Data) extends TimedOperator(op1, op2)
}
