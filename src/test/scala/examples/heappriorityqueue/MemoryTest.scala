package examples.heappriorityqueue

import chisel3._
import chiseltest._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import examples.heappriorityqueue.Helpers._
import examples.heappriorityqueue.modules.linearSearchMem

/**
  * contains randomized tests for the memory module
  *   - one checks read and write specifications
  *   - the other tests the search behaviour
  */
class MemoryTest extends AnyFreeSpec with ChiselScalatestTester with Matchers {
  // Use a static seed for reproducibility
  val rand = new scala.util.Random(0)
  def pokeMask(port: UInt, mask: Int = -1): Int = {
    if (mask != -1) {
      port.poke(mask.U)
      mask
    } else {
      pokeMask(port, rand.nextInt(Math.pow(2, parameters.order).toInt))
    }
  }

  def applyMask(data: Seq[Seq[Int]], mask: Int, resetVal: Seq[Int]): Seq[Seq[Int]] = {
    Seq.tabulate(parameters.order)(i => if ((mask & 1 << i) != 0) data(i) else resetVal)
  }

  def getResetVal(cWid: Int, nWid: Int, rWid: Int): Seq[Int] = {
    Seq(math.pow(2, cWid).toInt, math.pow(2, nWid).toInt, math.pow(2, rWid).toInt).map(_ - 1)
  }

  implicit val parameters = PriorityQueueParameters(32, 4, 4, 8, 5)
  val debugOutput = true

  "MaskedSeqMem should read and write with mask" in {
    test(new linearSearchMem(parameters.size)) { dut =>
      import parameters._

      while (!dut.srch.done.peek().litToBoolean) dut.clock.step(1)

      for (i <- 0 until parameters.size / order) {
        dut.wr.address.poke(i.U)
        dut.rd.address.poke(i.U)
        dut.srch.search.poke(false.B)
        dut.clock.step(1)

        val poke = pokePrioAndIDVec(dut.wr.data)
        val mask = pokeMask(dut.wr.mask)
        val res = applyMask(poke, mask, getResetVal(superCycleWidth, cycleWidth, referenceIdWidth))

        dut.wr.write.poke(true.B)
        dut.clock.step(1)
        dut.wr.write.poke(false.B)

        res should equal (peekPrioAndIdVec(dut.rd.data))
      }
    }
  }

  "MaskedSeqMem should find reference IDs" in {
    test(new linearSearchMem(parameters.size)) { dut =>
      import parameters._
      val refIDs = Array.fill(parameters.size)(0)

      while (!dut.srch.done.peek().litToBoolean) dut.clock.step(1)

      for (i <- 0 until parameters.size / order) {
        dut.wr.address.poke(i.U)
        dut.srch.search.poke(false.B)
        dut.clock.step(1)

        val poke = pokePrioAndIDVec(dut.wr.data)
        pokeMask(dut.wr.mask, 15)
        Seq.tabulate(order)(j => refIDs(i * 4 + j) = poke(j)(2))

        dut.wr.write.poke(true.B)
        dut.clock.step(1)
        dut.wr.write.poke(false.B)
      }

      dut.srch.heapSize.poke(parameters.size.U)

      for (i <- 0 until 1000) {
        dut.srch.search.poke(true.B)
        val searchKey = rand.nextInt(Math.pow(2, referenceIdWidth).toInt)
        dut.srch.refID.poke(searchKey.U)

        dut.clock.step(1)

        while (!dut.srch.done.peek().litToBoolean && !dut.srch.error.peek().litToBoolean) dut.clock.step(1)

        dut.srch.search.poke(false.B)
        dut.clock.step(1)

        if (refIDs.contains(searchKey)) {
          dut.srch.res.peek().litValue should equal (refIDs.indexOf(searchKey) + 1)
        } else {
          dut.srch.error.peek().litToBoolean should be (true)
        }
      }
    }
  }
}
