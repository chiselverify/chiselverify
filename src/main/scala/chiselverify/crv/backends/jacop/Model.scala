package chiselverify.crv.backends.jacop

import scala.collection.mutable.ListBuffer
import scala.util.Random

/** Internal class defined only in the jacop backand. This class is used as a database in which all the random variables
  * are stored.
  * @param seed with which the current class is initialized
  */
class Model(val seed: Int = new Random().nextInt()) extends org.jacop.scala.Model {
  val crvconstr = new ListBuffer[Constraint]()
  val randcVars = new ListBuffer[Randc]
  val distConst = new ListBuffer[DistConstraint]

  override def imposeAllConstraints() {
    // Reset number of constraints
    this.numberOfConstraints = 0
    this.crvconstr.filter(_.isEanble).foreach(e => this.impose(e.getConstraint))
  }

  def apply(s: String): Rand = {
    vars.filter(_ != null).find(_.id() == s).get.asInstanceOf[Rand]
  }
}
