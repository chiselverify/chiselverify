package examples.heappriorityqueue.min

import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor

object MinModel {
  def compute(transaction: Seq[MinTestType]): MinBundle[MinTestType] = {
    case class Wiggle(isParent: Boolean, valid: Boolean, value: BigInt)
    val res = transaction.reduce { (l, r) =>
      if (!l.valid.litToBoolean && !r.valid.litToBoolean) {
        if (l.isParent.litToBoolean) l else r
      } else if (l.valid.litToBoolean && !r.valid.litToBoolean) l
      else if (!l.valid.litToBoolean && r.valid.litToBoolean) r
      else {
        if (l.value.litValue == r.value.litValue) {
          if (l.isParent.litToBoolean) l else r
        } else {
          if (l.value.litValue < r.value.litValue) l else r
        }
      }
    }
    (new MinBundle[MinTestType](transaction.head.cloneType)).Lit(_.item -> res, _.index -> transaction.indexOf(res).U)
  }
}
