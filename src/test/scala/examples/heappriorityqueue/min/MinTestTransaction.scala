package examples.heappriorityqueue.min

import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chisel3._
import chiselverify.crv.backends.jacop._
import chiselverify.crv.{RangeBinder, ValueBinder}
import org.junit.runners.model.TestTimedOutException

import scala.math.{pow, random}

class MinTestTransaction(n: Int, width: Int, seed: Int) extends RandObj {
  currentModel = new Model(seed)

  val isParentSeq = Seq.tabulate(n)(i => new Rand(s"isParent($i)",0,1))
  val validSeq = Seq.tabulate(n)(i => new Rand(s"valid($i)",0,1))
  val valueSeq = Seq.tabulate(n)(i => new Rand(s"value($i)",0,pow(2,width).toInt-1))

  val onlyOneParent = isParentSeq.reduce((l, r) => l #+ r) #= 1

  def toBundle: Seq[MinTestType] = {
    isParentSeq.zip(validSeq).zip(valueSeq).map{ case ((isParent,valid),value) => (isParent.value(),valid.value(),value.value())}.map{ case (isParent,valid,value) =>
      (new MinTestType(width)).Lit(_.isParent -> isParent.B, _.valid -> valid.B, _.value -> value.U)
    }
  }

  override def toString: String = {
    isParentSeq.zip(validSeq).zip(valueSeq).map{ case ((isParent,valid),value) => (isParent.value(),valid.value(),value.value())}.map{ case (isParent,valid,value) =>
      s"${if(valid == 1) value else "invalid"}${if(isParent == 1) " (parent)" else ""}"
    }.mkString("(",", ",")")
  }

}
