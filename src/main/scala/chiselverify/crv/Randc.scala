package chiselverify.crv

/** 
  * Describes a random-cyclic variable
  * 
  * As for [[Rand]] the name of the trait reflect the same keyword used in SystemVerilog.
  */
trait Randc {
  /** 
    * Returns the current value of the variable
    */
  def value(): Int

  /** 
    * Gets the next value of the random variable
    */
  def next(): Int

  /** 
    * Sets the value of the variable
    * @param that the value to be set
    */
  def setVar(that: Int): Unit

  override def toString: String = value().toString
}
