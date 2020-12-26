package examples.heappriorityqueue

import chiseltest._
import examples.heappriorityqueue.Helpers._
import examples.heappriorityqueue.modules.MinFinder
import org.scalatest._

/** contains a randomized test for the MinFinder module
  */
class MinFinderTest extends FreeSpec with ChiselScalatestTester {

  def calculateOut(values: Seq[Seq[Int]]): Int = {
    val cyclic = values.map(_.head)
    val cyclicMins = cyclic.zipWithIndex.filter(_._1 == cyclic.min).map(_._2)
    if (cyclicMins.length == 1) {
      cyclicMins.head
    } else {
      val normals = values.map(_(1))
      val candidates = Seq.tabulate(values.length)(i => if (cyclicMins.contains(i)) normals(i) else Int.MaxValue)
      candidates.indexOf(candidates.min)
    }
  }

  val cWid = 2
  val nWid = 8
  val rWid = 3
  val n = 8

  "MinFinder should identify minimum value with the lowest index" in {
    test(new MinFinder(n, cWid, nWid, rWid)) { dut =>
      setWidths(cWid, nWid, rWid)

      for (i <- 0 until 1000) {

        val values = pokePrioAndIDVec(dut.io.values)

        assert(
          peekPrioAndId(dut.io.res) == values(calculateOut(values)),
          s"\n${prioAndIdVecToString(values)} should be ${prioAndIdToString(values(calculateOut(values)))} received ${prioAndIdToString(peekPrioAndId(dut.io.res))}"
        )

        assert(
          dut.io.idx.peek.litValue == calculateOut(values),
          s"\n${prioAndIdVecToString(values)} should be ${calculateOut(values)} received ${dut.io.idx.peek.litValue}"
        )
      }

    }
  }
}
