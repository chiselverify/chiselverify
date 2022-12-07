package chiselverify.crv.backends.jacop

class Constraint(private val constraint: org.jacop.constraints.Constraint)(implicit var model: Model)
  extends chiselverify.crv.Constraint {

  override type U = org.jacop.constraints.Constraint
  var isEnabled: Boolean = true

  /** 
    * Disables the constraint
    */
  override def disable() = {
    isEnabled = false
    constraint.removeConstraint()
  }

  /** 
    * Enables the constraint
    * 
    * Each constraint is by default enabled when instantiated. This method has effect only if called after [[disable]]
    */
  override def enable() = {
    isEnabled = true
  }

  /** 
    * Returns the constraint 
    * 
    * This method was introduced as an helper class for the jacop backend
    * @return [[Constraint]]
    */
  override def getConstraint: org.jacop.constraints.Constraint = constraint

  override def toString: String = constraint.toString
}
