package chiselverify.crv.backends

import org.jacop.constraints._
import org.jacop.core.IntDomain
import org.jacop.scala.{IntSet, SetVar}
import org.jacop.set.constraints.{EinA, XinA}
import scala.util.Random

import chiselverify.crv.randName

package object jacop {
  type RandVar = Rand
  type RandCVar = Randc
  private[crv] class Rand(name: String, min: Int, max: Int)(implicit val model: Model)
    extends org.jacop.core.IntVar(model, name, min, max)
      with chiselverify.crv.Rand {

    override type U = Rand

    /** Defines an anonymous finite domain integer variable.
      *
      * @constructor Creates a new finite domain integer variable.
      * @param min minimal value of variable's domain.
      * @param max maximal value of variable's domain.
      */
    def this(min: Int, max: Int)(implicit model: Model) = {
      this("_$" + model.n, min, max)(model)
      model.n += 1
    }

    /** Defines an anonymous finite domain integer variable.
      *
      * @constructor Creates a new finite domain integer variable with minimal and maximal
      *              values in the domain defined by [[org.jacop]]
      * @param name variable's identifier.
      */
    def this(name: String)(implicit model: Model) = {
      this(name, org.jacop.core.IntDomain.MinInt, org.jacop.core.IntDomain.MaxInt)(model)
      model.n += 1
    }

    /** Defines an anonymous finite domain integer variable.
      *
      * @constructor Creates a new finite domain integer variable with minimal and maximal
      *              values in the domain defined by [[org.jacop]]
      */
    def this()(implicit model: Model) = {
      this(org.jacop.core.IntDomain.MinInt, org.jacop.core.IntDomain.MaxInt)(model)
      model.n += 1
    }

    /** Defines a finite domain integer variable.
      *
      * @constructor Create a new finite domain integer variable with the domain defined by IntSet.
      * @param dom variable's domain defined as a set of integers IntSet.
      */
    def this(dom: IntSet)(implicit model: Model) = {
      this()(model)
      this.dom.intersectAdapt(dom)
      model.n += 1
    }

    /** Defines a finite domain integer variable.
      *
      * @constructor Create a new finite domain integer variable with the domain
      *              defined by IntSet.
      * @param name variable's identifier.
      * @param dom  variable's domain defined as a set of IntSet.
      */
    def this(name: String, dom: IntSet)(implicit model: Model) = {
      this(name)(model)
      this.dom.intersectAdapt(dom)
      model.n += 1
    }

    /** Assign a specific value to the current variable
      * @param v BigInt
      */
    def setVar(v: BigInt): Unit = {
      require(v < Int.MaxValue)
      setDomain(v.toInt, v.toInt)
    }

    /** Defines the add [[JaCoPConstraint]] between two Rand variables
      *
      * @param that a second parameter for the addition constraint
      * @return [[Rand]] variable being the result of the addition [[JaCoPConstraint]].
      */
    def +(that: Rand): Rand = {
      val result = new Rand(IntDomain.addInt(this.min(), that.min()), IntDomain.addInt(this.max(), that.max()))
      val c = new XplusYeqZ(this, that, result)
      model.crvconstr += new JaCoPConstraint(c)
      result
    }

    /** Defines add [[JaCoPConstraint]] between Rand and an integer value.
      *
      * @param that a second integer parameter for the addition [[JaCoPConstraint]].
      * @return [[Rand]] variable being the result of the addition [[JaCoPConstraint]].
      */
    def +(that: BigInt): Rand = {
      require(that <= Int.MaxValue)
      val result = new Rand(IntDomain.addInt(this.min(), that.toInt), IntDomain.addInt(this.max(), that.toInt))
      val c = new XplusCeqZ(this, that.toInt, result)
      model.crvconstr += new JaCoPConstraint(c)
      result
    }

    /** Defines subtract [[JaCoPConstraint]] between two Rand.
      *
      * @param that a second parameter for the subtraction [[JaCoPConstraint]].
      * @return [[Rand]] variable being the result of the subtraction [[JaCoPConstraint]].
      */
    def -(that: Rand): Rand = {
      val result = new Rand(IntDomain.subtractInt(this.min(), that.max()), IntDomain.subtractInt(this.max(), that.min()))
      val c = new XplusYeqZ(result, that, this)
      model.crvconstr += new JaCoPConstraint(c)
      result
    }

    /** Defines subtract [[JaCoPConstraint]] between [[Rand]] and an integer value.
      *
      * @param that a second integer parameter for the subtraction [[JaCoPConstraint]].
      * @return [[Rand]] variable being the result of the subtraction [[JaCoPConstraint]].
      */
    def -(that: BigInt): Rand = {
      require(that <= Int.MaxValue)
      val result = new Rand(IntDomain.subtractInt(this.min(), that.toInt), IntDomain.subtractInt(this.max(), that.toInt))
      val c = new XplusCeqZ(result, that.toInt, this)
      val crvc = new JaCoPConstraint(c)
      model.crvconstr += crvc
      result
    }

    /** Defines equation [[JaCoPConstraint]] between two [[Rand]].
      *
      * @param that a second parameter for equation [[JaCoPConstraint]].
      * @return the defined [[JaCoPConstraint]].
      */
    def ==(that: Rand): JaCoPConstraint = {
      val c = new XeqY(this, that)
      val crvc = new JaCoPConstraint(c)
      model.crvconstr += crvc
      crvc
    }

    /** Defines equation [[JaCoPConstraint]] between [[Rand]] and a integer constant.
      *
      * @param that a second parameter for equation [[JaCoPConstraint]].
      * @return the defined [[JaCoPConstraint]].
      */
    def ==(that: BigInt): JaCoPConstraint = {
      require(that <= Int.MaxValue)
      val c = new XeqC(this, that.toInt)
      val crvc = new JaCoPConstraint(c)
      model.crvconstr += crvc
      crvc
    }
    def ==(that: Int): JaCoPConstraint = ==(BigInt(that))

    /** Defines multiplication [[JaCoPConstraint]] between two [[Rand]].
      *
      * @param that a second parameter for the multiplication [[JaCoPConstraint]].
      * @return [[Rand]] variable being the result of the multiplication [[JaCoPConstraint]].
      */
    def *(that: Rand): Rand = {
      val bounds = IntDomain.mulBounds(this.min(), this.max(), that.min(), that.max())
      val result = new Rand(bounds.min(), bounds.max())
      val c = new XmulYeqZ(this, that, result)
      model.crvconstr += new JaCoPConstraint(c)
      result
    }

    /** Defines multiplication [[JaCoPConstraint]] between [[Rand]] and an integer value.
      *
      * @param that a second integer parameter for the multiplication [[JaCoPConstraint]].
      * @return [[Rand]] variable being the result of the multiplication [[JaCoPConstraint]].
      */
    def *(that: BigInt): Rand = {
      require(that <= Int.MaxValue)
      val bounds = IntDomain.mulBounds(this.min(), this.max(), that.toInt, that.toInt)
      val result = new Rand(bounds.min(), bounds.max())
      val c = new XmulCeqZ(this, that.toInt, result)
      model.crvconstr += new JaCoPConstraint(c)
      result
    }

    /** Defines integer division [[JaCoPConstraint]] between two [[Rand]].
      *
      * @param that a second parameter for the integer division [[JaCoPConstraint]].
      * @return [[Rand]] variable being the result of the integer division [[JaCoPConstraint]].
      */
    def div(that: Rand): Rand = {
      val bounds = IntDomain.divBounds(this.min(), this.max(), that.min(), that.max())
      val result = new Rand(bounds.min(), bounds.max())
      val c = new XdivYeqZ(this, that, result)
      model.crvconstr += new JaCoPConstraint(c)
      result
    }

    /** Defines integer division [[JaCoPConstraint]] between [[Rand]] and an integer value.
      *
      * @param that a second parameter for the integer division [[JaCoPConstraint]].
      * @return [[Rand]] variable being the result of the integer division [[JaCoPConstraint]].
      */
    def div(that: BigInt): Rand = {
      require(that < Int.MaxValue)
      this.div(new Rand(that.toInt, that.toInt))
    }

    /** Defines [[JaCoPConstraint]] for integer reminder from division between two [[Rand]].
      *
      * @param that a second parameter for integer reminder from division [[JaCoPConstraint]].
      * @return [[Rand]] variable being the result of the integer reminder from division [[JaCoPConstraint]].
      */
    def mod(that: Rand): Rand = {
      var reminderMin: Int = 0
      var reminderMax: Int = 0

      if (this.min() >= 0) {
        reminderMin = 0
        reminderMax = Math.max(Math.abs(that.min()), Math.abs(that.max())) - 1
      } else if (this.max() < 0) {
        reminderMax = 0
        reminderMin = -Math.max(Math.abs(that.min()), Math.abs(that.max())) + 1
      } else {
        reminderMin = Math.min(Math.min(that.min(), -that.min()), Math.min(that.max(), -that.max())) + 1
        reminderMax = Math.max(Math.max(that.min(), -that.min()), Math.max(that.max(), -that.max())) - 1
      }

      val result = new Rand(reminderMin, reminderMax)
      val c = new XmodYeqZ(this, that, result)
      model.crvconstr += new JaCoPConstraint(c)
      result
    }

    /** Defines [[JaCoPConstraint]] for integer reminder from division [[Rand]] and an integer value.
      *
      * @param that a second parameter for integer reminder from division [[JaCoPConstraint]].
      * @return [[Rand]] variable being the result of the integer reminder from division [[JaCoPConstraint]].
      */
    def mod(that: BigInt): Rand = {
      require(that <= Int.MaxValue)
      this.mod(new Rand(that.toInt, that.toInt))
    }

    /** Defines exponentiation [[JaCoPConstraint]] between two [[Rand]].
      *
      * @param that exponent for the exponentiation [[JaCoPConstraint]].
      * @return [[Rand]] variable being the result of the exponentiation [[JaCoPConstraint]].
      */
    def ^(that: Rand): Rand = {
      val result = new Rand()
      val c = new XexpYeqZ(this, that, result)
      model.crvconstr += new JaCoPConstraint(c)
      result
    }

    /** Defines exponentiation [[JaCoPConstraint]] between [[Rand]] and an integer value.
      *
      * @param that exponent for the exponentiation [[JaCoPConstraint]].
      * @return [[Rand]] variable being the result of the exponentiation [[JaCoPConstraint]].
      */
    def ^(that: BigInt): Rand = {
      require(that <= Int.MaxValue)
      this ^ new Rand(that.toInt, that.toInt)
    }

    /** Defines unary "-" [[JaCoPConstraint]] for [[Rand]].
      *
      * @return the defined [[JaCoPConstraint]].
      */
    def unary_- : Rand = {
      val result = new Rand(-this.max(), -this.min())
      val c = new XplusYeqC(this, result, 0)
      model.crvconstr += new JaCoPConstraint(c)
      result
    }

    /** Defines inequality [[JaCoPConstraint]] between two [[Rand]].
      *
      * @param that a second parameter for inequality [[JaCoPConstraint]].
      * @return the defined [[JaCoPConstraint]].
      */
    def \=(that: Rand): JaCoPConstraint = {
      val c = new XneqY(this, that)
      val crvc = new JaCoPConstraint(c)
      model.crvconstr += crvc
      crvc
    }

    /** Defines inequality [[JaCoPConstraint]] between [[Rand]] and integer constant.
      *
      * @param that a second parameter for inequality [[JaCoPConstraint]].
      * @return the defined [[JaCoPConstraint]].
      */
    def \=(that: BigInt): JaCoPConstraint = {
      require(that <= Int.MaxValue)
      val c = new XneqC(this, that.toInt)
      val crvc = new JaCoPConstraint(c)
      model.crvconstr += crvc
      crvc
    }

    /** Defines "less than" [[JaCoPConstraint]] between two [[Rand]].
      *
      * @param that a second parameter for "less than" [[JaCoPConstraint]].
      * @return the defined [[JaCoPConstraint]].
      */
    def <(that: Rand): JaCoPConstraint = {
      val c = new XltY(this, that)
      val crvc = new JaCoPConstraint(c)
      model.crvconstr += crvc
      crvc
    }

    /** Defines "less than" [[JaCoPConstraint]] between [[Rand]] and integer constant.
      *
      * @param that a second parameter for "less than" [[JaCoPConstraint]].
      * @return the equation [[JaCoPConstraint]].
      */
    def <(that: BigInt): JaCoPConstraint = {
      require(that <= Int.MaxValue)
      val c = new XltC(this, that.toInt)
      val crvc = new JaCoPConstraint(c)
      model.crvconstr += crvc
      crvc
    }

    /** Defines "less than or equal" [[JaCoPConstraint]] between two [[Rand]].
      *
      * @param that a second parameter for "less than or equal" [[JaCoPConstraint]].
      * @return the defined [[JaCoPConstraint]].
      */
    def <=(that: Rand): JaCoPConstraint = {
      val c = new XlteqY(this, that)
      val crvc = new JaCoPConstraint(c)
      model.crvconstr += crvc
      crvc
    }

    /** Defines "less than or equal" [[JaCoPConstraint]] between [[Rand]] and integer constant.
      *
      * @param that a second parameter for "less than or equal" [[JaCoPConstraint]].
      * @return the equation [[JaCoPConstraint]].
      */
    def <=(that: BigInt): JaCoPConstraint = {
      require(that <= Int.MaxValue)
      val c = new XlteqC(this, that.toInt)
      val crvc = new JaCoPConstraint(c)
      model.crvconstr += crvc
      crvc
    }

    /** Defines "greater than" [[JaCoPConstraint]] between two [[Rand]].
      *
      * @param that a second parameter for "greater than" [[JaCoPConstraint]].
      * @return the defined [[JaCoPConstraint]].
      */
    def >(that: Rand): JaCoPConstraint = {
      val c = new XgtY(this, that)
      val crvc = new JaCoPConstraint(c)
      model.crvconstr += crvc
      crvc
    }

    /** Defines "greater than" [[JaCoPConstraint]] between [[Rand]] and integer constant.
      *
      * @param that a second parameter for "greater than" [[JaCoPConstraint]].
      * @return the equation [[JaCoPConstraint]].
      */
    def >(that: BigInt): JaCoPConstraint = {
      require(that <= Int.MaxValue)
      val c = new XgtC(this, that.toInt)
      val crvc = new JaCoPConstraint(c)
      model.crvconstr += crvc
      crvc
    }

    /** Defines "greater than or equal" [[JaCoPConstraint]] between two [[Rand]].
      *
      * @param that a second parameter for "greater than or equal" [[JaCoPConstraint]].
      * @return the defined [[JaCoPConstraint]].
      */
    def >=(that: Rand): JaCoPConstraint = {
      val c = new XgteqY(this, that)
      val crvc = new JaCoPConstraint(c)
      model.crvconstr += crvc
      crvc
    }

    /** Defines "greater than or equal" [[JaCoPConstraint]] between [[Rand]] and integer constant.
      *
      * @param that a second parameter for "greater than or equal" [[JaCoPConstraint]].
      * @return the equation [[JaCoPConstraint]].
      */
    def >=(that: BigInt): JaCoPConstraint = {
      require(that <= Int.MaxValue)
      val c = new XgteqC(this, that.toInt)
      val crvc = new JaCoPConstraint(c)
      model.crvconstr += crvc
      crvc
    }

    /** defines the add constraint between two [[Rand]] variables.
      * @param that a second parameter for the addition constraint
      * @return rand variable being the result of the addition constraint.
      */
    def #+(that: U): U = this.+(that)

    /** defines the add constraint between a [[Rand]] variable and a BigInt.
      * @param that a second parameter for the addition constraint
      * @return rand variable being the result of the addition constraint.
      */
    def #+(that: BigInt): U = this.+(that)

    /** defines the subtraction constraint between two [[Rand]] variables.
      * @param that a second parameter for the addition constraint
      * @return rand variable being the result of the addition constraint.
      */
    def #-(that: U): U = this.-(that)

    /** defines the subtraction constraint between a [[Rand]] variable and a BigInt.
      * @param that a second parameter for the addition constraint
      * @return rand variable being the result of the addition constraint.
      */
    def #-(that: BigInt): U = this.-(that)

    /** defines the multiplication constraint between two [[Rand]] variables.
      * @param that a second parameter for the addition constraint
      * @return rand variable being the result of the addition constraint.
      */
    def #*(that: U): U = this.*(that)

    /** defines the multiplication constraint between a [[Rand]] variable and a BigInt.
      * @param that a second parameter for the addition constraint
      * @return rand variable being the result of the addition constraint.
      */
    def #*(that: BigInt): U = this.*(that)

    /** defines the exponential constraint between two [[Rand]] variables.
      * @param that a second parameter for the addition constraint
      * @return rand variable being the result of the addition constraint.
      */
    def #^(that: U): U = this.^(that)

    /** defines the exponential constraint between a [[Rand]] variable and a BigInt.
      * @param that a second parameter for the addition constraint
      * @return rand variable being the result of the addition constraint.
      */
    def #^(that: BigInt): U = this.^(that)

    /** Defines inequality [[JaCoPConstraint]] between [[Rand]] and BigInt constant.
      *
      * @param that a second parameter for inequality [[JaCoPConstraint]].
      * @return the defined [[JaCoPConstraint]].
      */
    def #\=(that: U): JaCoPConstraint = this.\=(that)

    /** Defines inequality [[JaCoPConstraint]] between [[Rand]] and BigInt constant.
      *
      * @param that a second parameter for inequality [[JaCoPConstraint]].
      * @return the defined [[JaCoPConstraint]].
      */
    def #\=(that: BigInt): JaCoPConstraint = this.\=(that)

    /** Defines inequality [[JaCoPConstraint]] between [[Rand]] and BigInt constant.
      *
      * @param that a second parameter for inequality [[JaCoPConstraint]].
      * @return the defined [[JaCoPConstraint]].
      */
    def #=(that: U): JaCoPConstraint = this.==(that)

    /** Defines inequality [[JaCoPConstraint]] between [[Rand]] and BigInt constant.
      *
      * @param that a second parameter for inequality [[JaCoPConstraint]].
      * @return the defined [[JaCoPConstraint]].
      */
    def #=(that: BigInt): JaCoPConstraint = this.==(that)

    /** Defines "less than" [[JaCoPConstraint]] between two [[Rand]].
      *
      * @param that a second parameter for "less than" [[JaCoPConstraint]].
      * @return the defined [[JaCoPConstraint]].
      */
    def #<(that: U): JaCoPConstraint = this.<(that)

    /** Defines "less than" [[JaCoPConstraint]] between [[Rand]] and BigInt constant.
      *
      * @param that a second parameter for "less than" [[JaCoPConstraint]].
      * @return the equation [[JaCoPConstraint]].
      */
    def #<(that: BigInt): JaCoPConstraint = this.<(that)

    /** Defines "less than or equal" [[JaCoPConstraint]] between two [[Rand]].
      *
      * @param that a second parameter for "less than or equal" [[JaCoPConstraint]].
      * @return the defined [[JaCoPConstraint]].
      */
    def #<=(that: U): JaCoPConstraint = this.<=(that)

    /** Defines "less than or equal" [[JaCoPConstraint]] between [[Rand]] and BigInt constant.
      *
      * @param that a second parameter for "less than or equal" [[JaCoPConstraint]].
      * @return the equation [[JaCoPConstraint]].
      */
    def #<=(that: BigInt): JaCoPConstraint = this.<=(that)

    /** Defines "greater than" [[JaCoPConstraint]] between two [[Rand]].
      *
      * @param that a second parameter for "greater than or equal" [[JaCoPConstraint]].
      * @return the defined [[JaCoPConstraint]].
      */
    def #>(that: U): JaCoPConstraint = this.>(that)

    /** Defines "greater than" [[JaCoPConstraint]] between [[Rand]] and BigInt constant.
      *
      * @param that a second parameter for "greater than or equal" [[JaCoPConstraint]].
      * @return the equation [[JaCoPConstraint]].
      */
    def #>(that: BigInt): JaCoPConstraint = this.>(that)

    /** Defines "greater than or equal" [[JaCoPConstraint]] between two [[Rand]].
      *
      * @param that a second parameter for "greater than or equal" [[JaCoPConstraint]].
      * @return the defined [[JaCoPConstraint]].
      */
    def #>=(that: U): JaCoPConstraint = this.>=(that)

    /** Defines "greater than or equal" [[JaCoPConstraint]] between [[Rand]] and BigInt constant.
      *
      * @param that a second parameter for "greater than or equal" [[JaCoPConstraint]].
      * @return the equation [[JaCoPConstraint]].
      */
    def #>=(that: BigInt): JaCoPConstraint = this.>=(that)

    /** Defines [[JaCoPConstraint]] on inclusion of a [[Rand]] variable value in a set.
      *
      * @param that set that this variable's value must be included.
      * @return the equation [[JaCoPConstraint]].
      */
    def in(that: SetVar): JaCoPConstraint = {
      if (min == max) {
        val c = new EinA(min, that)
        val crvc = new JaCoPConstraint(c)
        model.crvconstr += crvc
        crvc
      } else {
        val c = new XinA(this, that)
        val crvc = new JaCoPConstraint(c)
        model.crvconstr += crvc
        crvc
      }
    }

    /** Defines [[JaCoPConstraint]] on inclusion of a [[Rand]] variable value in a set.
      *
      * @param that set that this variable's value must be included.
      * @return the equation [[JaCoPConstraint]].
      */
    def inside(that: SetVar): JaCoPConstraint = {
      this.in(that)
    }

    /**
      * Create a distribution constraint for the current value
      * @param groups the [[WeightedRange]] or [[WeightedValue]] to assign to the current variable
      * @return the [[DistConstraint]]
      */
    def dist(groups: Weight*): DistConstraint = {
      val c = new DistConstraint(this, groups.toList)
      model.distConst += c
      c
    }
  }

  private[crv] class Randc(min: Int, max: Int)(implicit model: Model)
    extends Rand(randName(10 ,model.seed), min, max) with chiselverify.crv.Randc {
    model.randcVars += this

    private val rand = new Random(model.seed)
    private var currentValue: Int = (math.abs(rand.nextInt()) % (max - min)) + min

    /** Returns the current value of the variable
      * @return return the current value of the variable
      */
    override def value(): Int = currentValue.toInt

    /** Gets the next value of the random variable
      * @return return the next value of the variable
      */
    override def next(): Int = {
      currentValue = if (currentValue == max) min else currentValue + 1
      currentValue
    }

    /** Set the value of the variable
      * @param that the value to be set
      */
    override def setVar(that: Int): Unit = currentValue = that
  }

  abstract class RandType
  case object Normal extends RandType
  case object Cyclic extends RandType

  /**
    * Allows for the declaration of either type of RandomVar.
    * @param min the minimum value that the random variable can take.
    * @param max the maximum value that the random variable can take.
    * @param randType Either Normal or Cyclic, the type of the random variable.
    * @param model the model used to store the random data.
    * @return a random variable of the correct type
    */
  def rand(min: Int, max: Int, randType: RandType = Normal)(implicit model: Model) : Rand = randType match {
    case Normal =>
      val rand = new Random(model.seed)
      val randLength = (rand.nextInt() % 30) + 10
      new Rand(randName(randLength, model.seed), min, max)

    case Cyclic => new Randc(min, max)
  }
}
