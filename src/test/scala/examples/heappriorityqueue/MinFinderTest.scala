package examples.heappriorityqueue

import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import examples.heappriorityqueue.Helpers._
import examples.heappriorityqueue.modules.MinFinder

/**
  * contains a randomized test for the MinFinder module
  */
class MinFinderTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  def calculateOut(values: Seq[Seq[Int]]): Int = {
    val cyclic = values.map(_.head)
    val cyclicMins = cyclic.zipWithIndex.filter(_._1 == cyclic.min).map(_._2)
    if (cyclicMins.length == 1) {
      cyclicMins.head
    } else {
      val normals = values.map(_ (1))
      val candidates = Seq.tabulate(values.length)(i => if (cyclicMins.contains(i)) normals(i) else Int.MaxValue)
      candidates.indexOf(candidates.min)
    }
  }

  val n = 8
  implicit val parameters = PriorityQueueParameters(32, 4, 4, 8, 5)

  "MinFinder should identify minimum value with the lowest index" in {
    test(new MinFinder(n)) { dut =>
      import parameters._
      setWidths(superCycleWidth, cycleWidth, referenceIdWidth)

      for (_ <- 0 until 1000) {
        val values = pokePrioAndIDVec(dut.io.values)
        dut.io.index.peek().litValue should equal (calculateOut(values))
        peekPrioAndId(dut.io.res) should equal (values(calculateOut(values)))
      }
    }
  }
}
