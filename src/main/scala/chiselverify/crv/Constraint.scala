package chiselverify.crv

/** Constraint trait that describes the common interface that each constraint needs to implement. As for [[Rand]], the
  * common method for the constraints are defined based on the SystemVerilog implementation.
  *
  * @see <a href="https://www.chipverify.com/systemverilog/systemverilog-constraint-examples">systemverilog-random-variables</a>
  */
trait Constraint {

  /** Type used to define the concrete implementation of the constraint for each backends.
    */
  type U

  /** Enables the current constraint. Each constraint is by default enable when instantiated. This method has effect only
    * if called after [[disable]]
    */
  def enable(): Unit

  /** Disable the current constraint
    */
  def disable(): Unit

  /** Returns the current constraint. This method was introduced as an helper class for the jacop backend
    * @return U
    */
  def getConstraint: U
}
