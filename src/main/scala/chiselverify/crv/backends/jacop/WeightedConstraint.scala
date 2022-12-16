package chiselverify.crv.backends.jacop

/**
  * Represents a range of values between 0 and 1.
  * @param min the minimum value of the bucket
  * @param max the maximum value of the bucket 
  * 
  * The length of a bucket intrinsically represents the weight associated with it.
  */
case class Bucket(min: Double, max: Double)

/**
  * Used in distribution constraints to set the priority of a range or a value
  */
sealed trait Weight {
  val weight: Int
}

/**
  * Compound type between a value and a weight
  * @param value integer value
  * @param weight integer weight
  */
case class WeightedValue(value: Int, weight: Int) extends Weight

/**
  * Compound type between a range and a weight
  * @param range integer range
  * @param weight integer weight
  */
case class WeightedRange(range: Range, weight: Int) extends Weight

/**
  * Weighted constraint group is a class that assigns to each constraint group a bucket
  * @param bucket the current bucket
  * @param group a group of constraints
  */
class WConstraintGroup(val bucket: Bucket, group: JaCoPConstraint*) extends JaCoPConstraintGroup {
  override val constraints: List[JaCoPConstraint] = group.toList
  def contains(point: Double): Boolean =  bucket.min < point && point <= bucket.max
}
