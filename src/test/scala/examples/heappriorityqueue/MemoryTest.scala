package examples.heappriorityqueue

import chisel3._
import chiseltest._
import examples.heappriorityqueue.Helpers._
import examples.heappriorityqueue.modules.linearSearchMem
import org.scalatest.FreeSpec

/** contains randomized tests for the memory module
  *   - one checks read and write specifications
  *   - the other tests the search behaviour
  */
class MemoryTest extends FreeSpec with ChiselScalatestTester {

  def pokeMask(port: UInt, mask: Int = -1): Int = {
    if (mask != -1) {
      port.poke(mask.U)
      mask
    } else {
      val rand = scala.util.Random
      pokeMask(port, rand.nextInt(Math.pow(2, chCount).toInt))
    }
  }

  def applyMask(data: Seq[Seq[Int]], mask: Int, resetVal: Seq[Int]): Seq[Seq[Int]] = {
    Seq.tabulate(chCount)(i => if ((mask & 1 << i) != 0) data(i) else resetVal)
  }

  def getResetVal(cWid: Int, nWid: Int, rWid: Int): Seq[Int] = {
    Seq(math.pow(2, cWid).toInt, math.pow(2, nWid).toInt, math.pow(2, rWid).toInt).map(_ - 1)
  }

  val heapSize = 32
  val chCount = 4
  val nWid = 8
  val cWid = 2
  val rWid = 5
  val debugOutput = true

  "MaskedSeqMem should read and write with mask" in {
    test(new linearSearchMem(heapSize, chCount, cWid, nWid, rWid)) { dut =>
      while (!dut.srch.done.peek.litToBoolean) dut.clock.step(1)

      for (i <- 0 until heapSize / chCount) {

        dut.wr.address.poke(i.U)
        dut.rd.address.poke(i.U)
        dut.srch.search.poke(false.B)
        dut.clock.step(1)

        val poke = pokePrioAndIDVec(dut.wr.data)
        val mask = pokeMask(dut.wr.mask)
        val res = applyMask(poke, mask, getResetVal(cWid, nWid, rWid))

        dut.wr.write.poke(true.B)
        dut.clock.step(1)
        dut.wr.write.poke(false.B)

        assert(
          res == peekPrioAndIdVec(dut.rd.data),
          s"\npoked: ${prioAndIdVecToString(poke)} with mask: ${mask.toBinaryString.reverse.padTo(4, '0')}"
        )

      }
    }
  }
  "MaskedSeqMem should find reference IDs" in {
    test(new linearSearchMem(heapSize, chCount, cWid, nWid, rWid)) { dut =>
      while (!dut.srch.done.peek.litToBoolean) dut.clock.step(1)

      val refIDs = Array.fill(heapSize)(0)

      for (i <- 0 until heapSize / chCount) {

        dut.wr.address.poke(i.U)
        dut.srch.search.poke(false.B)
        dut.clock.step(1)

        val poke = pokePrioAndIDVec(dut.wr.data)
        pokeMask(dut.wr.mask, 15)
        Seq.tabulate(chCount)(j => refIDs(i * 4 + j) = poke(j)(2))

        dut.wr.write.poke(true.B)
        dut.clock.step(1)
        dut.wr.write.poke(false.B)

      }

      dut.srch.heapSize.poke(heapSize.U)

      for (i <- 0 until 1000) {
        dut.srch.search.poke(true.B)
        val searchKey = scala.util.Random.nextInt(Math.pow(2, rWid).toInt)
        dut.srch.refID.poke(searchKey.U)

        dut.clock.step(1)

        while (!dut.srch.done.peek.litToBoolean && !dut.srch.error.peek.litToBoolean) dut.clock.step(1)

        dut.srch.search.poke(false.B)
        dut.clock.step(1)

        val clue =
          s"\n${refIDs.mkString(", ")}\nsearched for: $searchKey should be: ${if (refIDs.contains(searchKey)) refIDs.indexOf(searchKey)
          else "error"} received: ${if (dut.srch.error.peek.litToBoolean) "error" else dut.srch.res.peek.litValue}"

        if (refIDs.contains(searchKey)) {
          assert(dut.srch.res.peek.litValue == refIDs.indexOf(searchKey) + 1)
        } else {
          assert(dut.srch.error.peek.litToBoolean, clue)
        }
      }
    }
  }
}
