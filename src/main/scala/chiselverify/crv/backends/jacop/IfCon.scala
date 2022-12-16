package chiselverify.crv.backends.jacop

import org.jacop.constraints.PrimitiveConstraint

/** 
  * Since not all csp solvers have support for conditional constraints, the IfThen and IfThenElse are not part of the common crv package.
  */
class IfCon(val const: JaCoPConstraint, val ifC: chiselverify.crv.CRVConstraint, val thenC: chiselverify.crv.CRVConstraint)(implicit model: Model)
  extends JaCoPConstraint(const.getConstraint) {

  /** 
    * Companion class for creating if-then-else constraint
    * @param ifCons an IfConstraint
    * @param elseC an ElseConstraint
    */
  class IfElseCon(val ifCons: IfCon, val elseC: chiselverify.crv.CRVConstraint) extends IfCon(const, ifC, thenC) {
    val newConstraint = new org.jacop.constraints.IfThenElse(
      ifC.getConstraint.asInstanceOf[PrimitiveConstraint],
      thenC.getConstraint.asInstanceOf[PrimitiveConstraint],
      elseC.getConstraint.asInstanceOf[PrimitiveConstraint]
    )
    val crvc = new JaCoPConstraint(newConstraint)
    model.crvconstr += crvc
    elseC.disable()

    override def disable(): Unit = {
      crvc.disable()
    }

    override def enable(): Unit = {
      crvc.enable()
    }
  }

  override def enable(): Unit = {
    const.enable()
  }

  override def disable(): Unit = {
    const.disable()
  }

  /** Create an if-then-else constraint
    * @param elseC the constraint to be applied if the ifC condition is NOT true
    * @return
    */
  def ElseC(elseC: chiselverify.crv.CRVConstraint): IfElseCon = {
    const.disable()
    new IfElseCon(this, elseC)
  }

  // Initialization conde
  model.crvconstr += const
  ifC.disable()
  thenC.disable()
}

/** 
  * Helper object for defining IfThen constraints
  */
object IfCon {
  /** 
    * Creates a new IfThen constraint
    * @param ifC the if condition represented as a constraint
    * @param thenC the constraint to be applied if the ifC condition is true
    * @param model the current [[Model]], constraint can only be defined inside a [[RandObj]]
    * @return return the newly constructed [[Constraint]]
    */
  def apply(ifC: chiselverify.crv.CRVConstraint)(thenC: chiselverify.crv.CRVConstraint)(implicit model: Model): IfCon = {
    val newConstraint =
      new org.jacop.constraints.IfThen(
        ifC.getConstraint.asInstanceOf[PrimitiveConstraint],
        thenC.getConstraint.asInstanceOf[PrimitiveConstraint]
      )
    val crvc = new JaCoPConstraint(newConstraint)
    new IfCon(crvc, ifC, thenC)
  }
}
