package chiselverify.crv

/** Trait that describe a random-cyclic variable. As for [[Rand]] the name of the trait reflect the same keyword used
  * in SystemVerilog.
  */
trait Randc {

  /** Returns the current value of the variable
    * @return return the current value of the variable
    */
  def value(): BigInt

  /** Gets the next value of the random variable
    * @return return the next value of the variable
    */
  def next(): BigInt

  /** Set the value of the variable
    * @param that the value to be set
    */
  def setVar(that: BigInt): Unit

  override def toString: String = value().toString
}
