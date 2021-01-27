package chiselverify.crv.backends.jacop

import chiselverify.crv.CRVException
import chiselverify.crv.backends.jacop.experimental.WConstraintGroup

import scala.collection.immutable
import scala.util.Random

trait DistTrait {
  protected val random: Random
  protected val wConstraintGList: Seq[WConstraintGroup]
  protected val _var: Rand
  /**
    * Like for normal constraints distribution constraint
    * can be enabled or disabled
    */
  var isEanble = true

  def disable(): Unit = {
    isEanble = false
  }

  def enable(): Unit = {
    isEanble = true
  }

  /**
    * Randomly enable one of the constraint groups defined in the
    * constraint group list
    */
  def randomlyEnable(): Unit = {
    val number = random.nextDouble()
    wConstraintGList.find(x => x.contains(number)) match {
      case None => throw CRVException(s"Something went wrong in the distribution selection of ${_var.toString}")
      case Some(x) => x.enable()
    }
  }

  /**
    * disable all the constraints in the constraint group list
    */
  def disableAll(): Unit = wConstraintGList foreach (_.disable())

  /**
    * From a list of doubles creates a list of buckets where
    * the min is the current double and the max is the next element on the list
   */
  protected def swipeAndSum(buckets: List[Double]): List[Bucket] = {
    buckets match {
      case Nil => Nil
      case _ :: Nil => Nil
      case x :: xs => Bucket(x, xs.head) :: swipeAndSum(xs)
    }
  }
}

/**
  * Create a [[DistConstraint]] between a [[Rand]] and a list of [[WeightedRange]] and [[WeightedValue]]
  * @param variable the [[Rand]] variable to constraint
  * @param listOfDistC list of [[WeightedRange]] and [[WeightedValue]]
  * @param model implicit [[Model]] of the current [[RandObj]]
  */
class DistConstraint(variable: Rand, listOfDistC: List[Weight])(implicit model: Model) extends DistTrait {
  protected override val _var: Rand = variable
  protected override val random = new Random(model.seed + listOfDistC.length)

  /**
    * Create a [[WConstraintGroup]] based on the type of [[Weight]] and [[Bucket]]
    * @param rangeAndBucket tuple of [[Weight]] and [[Bucket]]
    * @return
    */
  private def createWeightedConstraintGroup(rangeAndBucket: (Weight, Bucket)): WConstraintGroup = {
    rangeAndBucket match {
      case (weight, bucket) => weight match {
        case WeightedRange(range, _) => new WConstraintGroup(bucket, variable #> range.start, variable #<= range.end)
        case WeightedValue(value, _) => new WConstraintGroup(bucket, variable #= value)
      }
    }
  }

  /**
    * Constraint group list
    */
  protected override val wConstraintGList: immutable.Seq[WConstraintGroup] = {
    val total: Double = listOfDistC.map(_.weight).sum.toDouble
    val buckets:  List[Double] = listOfDistC.map(_.weight / total).scan(0.0)((a, b) => a + b)
    val zipped  = listOfDistC zip swipeAndSum(buckets)
    zipped.map(createWeightedConstraintGroup)
  }
}
