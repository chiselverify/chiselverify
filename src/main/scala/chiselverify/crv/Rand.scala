package chiselverify.crv

/** The trait Rand represent a random field inside a random object. This trait should be used as a common interface
  * between all the csp backends. The name Rand correspond to the keyword used in for defining random variables.
  * Since this library is targeted for chisel and chisel-tester2, the only concrete type supported for the random variables is BigInt.
  *
  * @see <a href="https://www.chipverify.com/systemverilog/systemverilog-random-variables">systemverilog-random-variables</a>
  */
trait Rand {

  /** Concrete type implemented in each backed.
    *
    * TODO: Check if there is a better way of describing this sort of "backend" pattern in which every implementation
    *  gives the same basic functionalities but with different performance and tradeoffs
    */
  type U <: Rand

  /** defines the add constraint between two [[Rand]] variables.
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint.
    */
  def #+(that: U): U

  /** defines the add constraint between a [[Rand]] variable and a BigInt.
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint.
    */
  def #+(that: BigInt): U

  /** defines the subtraction constraint between two [[Rand]] variables.
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint.
    */
  def #-(that: U): U

  /** defines the subtraction constraint between a [[Rand]] variable and a BigInt.
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint.
    */
  def #-(that: BigInt): U

  /** defines the multiplication constraint between two [[Rand]] variables.
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint.
    */
  def #*(that: U): U

  /** defines the multiplication constraint between a [[Rand]] variable and a BigInt.
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint.
    */
  def #*(that: BigInt): U

  /** defines the division constraint between two [[Rand]] variables.
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint.
    */
  def div(that: U): U

  /** defines the division constraint between a [[Rand]] variable and a BigInt.
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint.
    */
  def div(that: BigInt): U

  /** defines the modulo constraint between two [[Rand]] variables.
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint.
    */
  def mod(that: U): U

  /** defines the modulo constraint between a [[Rand]] variable and a BigInt.
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint.
    */
  def mod(that: BigInt): U

  /** defines the exponential constraint between two [[Rand]] variables.
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint.
    */
  def #^(that: U): U

  /** defines the exponential constraint between a [[Rand]] variable and a BigInt.
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint.
    */
  def #^(that: BigInt): U

  /** Defines inequality [[Constraint]] between [[Rand]] and BigInt constant.
    *
    * @param that a second parameter for inequality [[Constraint]].
    * @return the defined [[Constraint]].
    */
  def #\=(that: U): Constraint

  /** Defines inequality [[Constraint]] between [[Rand]] and BigInt constant.
    *
    * @param that a second parameter for inequality [[Constraint]].
    * @return the defined [[Constraint]].
    */
  def #\=(that: BigInt): Constraint

  /** Defines "less than" [[Constraint]] between two [[Rand]].
    *
    * @param that a second parameter for "less than" [[Constraint]].
    * @return the defined [[Constraint]].
    */
  def #<(that: U): Constraint

  /** Defines "less than" [[Constraint]] between [[Rand]] and BigInt constant.
    *
    * @param that a second parameter for "less than" [[Constraint]].
    * @return the equation [[Constraint]].
    */
  def #<(that: BigInt): Constraint

  /** Defines "less than or equal" [[Constraint]] between two [[Rand]].
    *
    * @param that a second parameter for "less than or equal" [[Constraint]].
    * @return the defined [[Constraint]].
    */
  def #<=(that: U): Constraint

  /** Defines "less than or equal" [[Constraint]] between [[Rand]] and BigInt constant.
    *
    * @param that a second parameter for "less than or equal" [[Constraint]].
    * @return the equation [[Constraint]].
    */
  def #<=(that: BigInt): Constraint

  /** Defines "greater than" [[Constraint]] between two [[Rand]].
    *
    * @param that a second parameter for "greater than or equal" [[Constraint]].
    * @return the defined [[Constraint]].
    */
  def #>(that: U): Constraint

  /** Defines "greater than" [[Constraint]] between [[Rand]] and BigInt constant.
    *
    * @param that a second parameter for "greater than or equal" [[Constraint]].
    * @return the equation [[Constraint]].
    */
  def #>(that: BigInt): Constraint

  /** Defines "greater than or equal" [[Constraint]] between two [[Rand]].
    *
    * @param that a second parameter for "greater than or equal" [[Constraint]].
    * @return the defined [[Constraint]].
    */
  def #>=(that: U): Constraint

  /** Defines "greater than or equal" [[Constraint]] between [[Rand]] and BigInt constant.
    *
    * @param that a second parameter for "greater than or equal" [[Constraint]].
    * @return the equation [[Constraint]].
    */
  def #>=(that: BigInt): Constraint

  /** Set the value of the current random variable
    * @param that the value to assign to the random variable
    */
  def setVar(that: BigInt): Unit

}
