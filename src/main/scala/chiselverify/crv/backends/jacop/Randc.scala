package chiselverify.crv.backends.jacop

import scala.util.Random

class Randc(val min: BigInt, val max: BigInt)(implicit model: Model) extends chiselverify.crv.Randc {
  model.randcVars += this

  private val rand = new Random(model.seed)
  private var currentValue: BigInt = (math.abs(rand.nextInt) % (max - min)) + min

  /** Returns the current value of the variable
    * @return return the current value of the variable
    */
  override def value(): BigInt = currentValue

  /** Gets the next value of the random variable
    * @return return the next value of the variable
    */
  override def next(): BigInt = {
    currentValue = if (currentValue == max) min else currentValue + 1
    currentValue
  }

  /** Set the value of the variable
    * @param that the value to be set
    */
  override def setVar(that: BigInt): Unit = {
    currentValue = that
  }

}
