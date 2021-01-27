package chiselverify.crv.backends.jacop

/**
  * A bucket represent a range of values between 0 and 1.
  * The length of the bucket intrinsically represents the weight
  * associated with this bucket
  *
  * @param min the minimum value of the bucket
  * @param max the maximum value of the bucket
  */
case class Bucket(min: Double, max: Double)

/**
  * A Weight is used in distribution constraints to
  * set the priority of a range or a value
  */
sealed trait Weight {
  val weight: Int
}

/**
  * WeightedValue is a compound type between an integer and a weight
  * @param value integer value
  * @param weight integer weight
  */
case class WeightedValue(value: Int, weight: Int) extends Weight

/**
  * WeightedRange is a compound type between an range and a weight
  * @param range integer value
  * @param weight integer weight
  */
case class WeightedRange(range: Range, weight: Int) extends Weight

/**
  * Weighted constraint group is a class that assign to each constraint group a bucket
  *
  * @param bucket the current bucket
  * @param group a group of constraints
  */
class WConstraintGroup(val bucket: Bucket, group: Constraint*) extends ConstraintGroup {
  override val constraints: List[Constraint] = group.toList
  def contains(point: Double): Boolean =  bucket.min < point && point <= bucket.max
}
