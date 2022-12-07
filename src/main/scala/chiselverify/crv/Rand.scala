package chiselverify.crv

/** 
  * Represents a random field inside a random object 
  * 
  * This trait should be used as a common interface between all the csp backends. 
  * The name Rand corresponds to the keyword used in SystemVerilog for defining random variables.
  * Since this library is targeted for chisel and chiseltest, the only concrete type supported for the random variables is BigInt.
  *
  * @see <a href="https://www.chipverify.com/systemverilog/systemverilog-random-variables">systemverilog-random-variables</a>
  */
trait Rand {
  /** 
    * Concrete type implemented in each backend
    *
    * @todo Check if there is a better way of describing this sort of "backend" pattern in which every implementation gives the same basic functionalities but with different performance and tradeoffs.
    */
  type U <: Rand

  /** 
    * Defines the addition constraint between two [[Rand]] variables
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint
    */
  def +(that: U): U

  /** 
    * Defines the addition constraint between a [[Rand]] variable and a BigInt
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint
    */
  def +(that: BigInt): U

  /** 
    * Defines the subtraction constraint between two [[Rand]] variables
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint
    */
  def -(that: U): U

  /** 
    * Defines the subtraction constraint between a [[Rand]] variable and a BigInt
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint
    */
  def -(that: BigInt): U

  /** 
    * Defines the multiplication constraint between two [[Rand]] variables
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint
    */
  def *(that: U): U

  /** 
    * Defines the multiplication constraint between a [[Rand]] variable and a BigInt
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint
    */
  def *(that: BigInt): U

  /** 
    * Defines the division constraint between two [[Rand]] variables
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint
    */
  def div(that: U): U

  /** 
    * Defines the division constraint between a [[Rand]] variable and a BigInt
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint
    */
  def div(that: BigInt): U

  /** 
    * Defines the modulo constraint between two [[Rand]] variables
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint
    */
  def mod(that: U): U

  /** 
    * Defines the modulo constraint between a [[Rand]] variable and a BigInt
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint
    */
  def mod(that: BigInt): U

  /** 
    * Defines the exponential constraint between two [[Rand]] variables
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint
    */
  def ^(that: U): U

  /** 
    * Defines the exponential constraint between a [[Rand]] variable and a BigInt
    * @param that a second parameter for the addition constraint
    * @return rand variable being the result of the addition constraint
    */
  def ^(that: BigInt): U

  /** 
    * Defines the inequality [[Constraint]] between two [[Rand]] variables
    * @param that a second parameter for inequality constraint
    * @return the defined constraint
    */
  def \=(that: U): Constraint

  /** 
    * Defines the inequality [[Constraint]] between a [[Rand]] variable and a BigInt
    * @param that a second parameter for the inequality constraint
    * @return the defined constraint
    */
  def \=(that: BigInt): Constraint

  /** 
    * Defines the "less than" [[Constraint]] between two [[Rand]] variables
    * @param that a second parameter for the "less than" constraint
    * @return the defined constraint
    */
  def <(that: U): Constraint

  /** 
    * Defines the "less than" [[Constraint]] between a [[Rand]] varaiable and a BigInt
    * @param that a second parameter for the "less than" constraint
    * @return the defined constraint
    */
  def <(that: BigInt): Constraint

  /** 
    * Defines the "less than or equal" [[Constraint]] between two [[Rand]] variables
    * @param that a second parameter for the "less than or equal" constraint
    * @return the defined constraint
    */
  def <=(that: U): Constraint

  /** 
    * Defines the "less than or equal" [[Constraint]] between a [[Rand]] variable and a BigInt
    * @param that a second parameter for the "less than or equal" constraint
    * @return the defined constraint
    */
  def <=(that: BigInt): Constraint

  /** 
    * Defines the "greater than" [[Constraint]] between two [[Rand]] variables
    * @param that a second parameter for the "greater than or equal" constraint
    * @return the defined constraint
    */
  def >(that: U): Constraint

  /** 
    * Defines the "greater than" [[Constraint]] between a [[Rand]] variable and a BigInt
    * @param that a second parameter for the "greater than or equal" constraint
    * @return the defined constraint
    */
  def >(that: BigInt): Constraint

  /** 
    * Defines the "greater than or equal" [[Constraint]] between two [[Rand]] variables
    * @param that a second parameter for the "greater than or equal" constraint
    * @return the defined constraint
    */
  def >=(that: U): Constraint

  /** 
    * Defines the "greater than or equal" [[Constraint]] between a [[Rand]] variable and a BigInt
    * @param that a second parameter for the "greater than or equal" constraint
    * @return the defined constraint
    */
  def >=(that: BigInt): Constraint

  /** 
    * Sets the value of the current random variable
    * @param that the value to assign to the random variable
    */
  def setVar(that: BigInt): Unit
/*
  //==========DUPLICATES FOR COMPATIBILITY==============
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
*/
}
