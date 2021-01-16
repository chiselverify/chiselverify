package chiselverify.crv

/** Trait that describes the common interface for a group of Constraints. Contrary to [[Constraint]], in SystemVerilog
  * a group of constraint is treated as a normal constraint. A constraint group in this case is just a container that
  * exposes the same methods of a normal constraint.
  *
  * @see <a href="https://www.chipverify.com/systemverilog/systemverilog-constraint-examples">systemverilog-random-variables</a>
  */
trait ConstraintGroup {

  /** List of all the constraint declared in the group
    */
  val constraints: List[Constraint]

  /** Enables the current constraint group. Each constraint is by default enable when instantiated. This method has effect only
    * if called after [[disable]]
    */
  def enable(): Unit = constraints.foreach(_.enable())

  /** Disable the current constraint group
    */
  def disable(): Unit = constraints.foreach(_.disable())
}
