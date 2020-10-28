package heappriorityqueue

import org.scalatest._
import chiseltest._
import coverage.Coverage.{Bins, CoverPoint, Cross, CrossBin}
import coverage.CoverageReporter
import heappriorityqueue.helpers._

class PriorityQueueCoverageTest extends FreeSpec with ChiselScalatestTester {

  val cWid = 2
  val nWid = 8
  val rWid = 3

  def tester(c: HeapPriorityQueue, heapSize: Int, chCount: Int, debugLvl: Int, testRuns: Int) : Unit = {
    var stepCounter = 0
    var successfulInsertions = 0
    var successfulRemovals = 0
    var insertionTimes = 0
    var removalTimes = 0
    var refIDcounter = 0

    val rand = scala.util.Random

    val dut = new HeapPriorityQueueWrapper(c, heapSize, chCount, debugLvl)(cWid, nWid, rWid)
    c.clock.setTimeout(0)
    val model = new Behavioural(heapSize, chCount)(cWid, nWid, rWid)

    ////////////////////////////////////////////////////Coverage////////////////////////////////////////////////////////

    val cr = new CoverageReporter
    cr.register(
      CoverPoint(c.io.cmd.op, "operation",
        Bins("insertion", 0 to 0)::Bins("removal", 1 to 1)::Nil)::
        CoverPoint(c.io.cmd.prio.cycl, "cmd.prio.cycl",
          Bins("cyclic", 0 to 3)::Nil)::
        CoverPoint(c.io.cmd.prio.norm, "cmd.prio.norm",
          Bins("lower half", 0 to (Math.pow(2,nWid)/2-1).toInt)::Bins("upper half", (Math.pow(2,nWid)/2-1).toInt to (Math.pow(2,nWid)-1).toInt)::Nil)::
        CoverPoint(c.io.head.prio.cycl, "head.prio.cycl",
          Bins("cyclic", 0 to 3)::Nil)::
        CoverPoint(c.io.head.prio.norm, "head.prio.norm",
          Bins("lower half", 0 to (Math.pow(2,nWid)/2-1).toInt)::Bins("upper half", (Math.pow(2,nWid)/2-1).toInt to (Math.pow(2,nWid)-1).toInt)::Nil)::

        Nil,
      //Declare cross points
      Cross("cyclics at ops", "operation", "cmd.prio.cycl",
        CrossBin("insertion", 0 to 0, 0 to 3)::CrossBin("removal", 1 to 1, 0 to 3)::Nil)::
        Cross("normals at ops", "operation", "cmd.prio.norm",
          CrossBin("insertion lower half", 0 to 0, 0 to (Math.pow(2,nWid)/2-1).toInt)::CrossBin("insertion upper half", 0 to 0, (Math.pow(2,nWid)/2-1).toInt to (Math.pow(2,nWid)-1).toInt)::
            CrossBin("removal lower half", 1 to 1, 0 to (Math.pow(2,nWid)/2-1).toInt)::CrossBin("removal upper half", 1 to 1, (Math.pow(2,nWid)/2-1).toInt to (Math.pow(2,nWid)-1).toInt)::Nil)::
        Nil)

    ////////////////////////////////////////////////helper functions////////////////////////////////////////////////////

    def randomPoke() : Seq[Int] = {
      // op, cprio, nprio, refID
      val ret = Seq(rand.nextInt(2),rand.nextInt(math.pow(2, cWid).toInt), rand.nextInt(math.pow(2, nWid).toInt), refIDcounter)
      refIDcounter += 1
      if (refIDcounter >= Math.pow(2, rWid)-1) refIDcounter = 0
      ret
    }

    def test(values: Seq[Seq[Int]]) : Unit = {
      for (poke <- values) {
        var debug = ""
        if (poke.head==0) { // insert operation
          // simulate dut and model
          val (steps, success, debugStr) = dut.insert(poke(1), poke(2), poke(3))
          model.insert(poke(1), poke(2), poke(3))

          // process results
          debug = debugStr
          stepCounter += steps
          if (success) {
            insertionTimes += steps
            successfulInsertions += 1
          }
        } else { // remove operation
          // simulate dut and model
          val (steps, success, debugStr) = dut.remove(poke(3))
          model.remove(poke(3))

          // process results
          debug = debugStr
          stepCounter += steps
          if (success) {
            removalTimes += steps
            successfulRemovals += 1
          }
        }
        // cross check dut and model; kill test and print debug if not matching
        cr.sample()
        assert(c.io.head.prio.cycl.peek.litValue == model.mem(0)(0),debug)
        assert(c.io.head.prio.norm.peek.litValue == model.mem(0)(1),debug)
        assert(c.io.head.refID.peek.litValue == model.mem(0)(2),debug)
        assert(model.mem.slice(1,model.mem.length).deep == dut.mem.flatten.deep,debug)
      }
    }

    //////////////////////////////////////////////////////testing/////////////////////////////////////////////////////////

    val pokes = Seq.fill(testRuns)(randomPoke())

    test(pokes)

    ///////////////////////////////////////////////////reports//////////////////////////////////////////////////////////

    val avgIns = insertionTimes.toDouble/successfulInsertions
    val avgRem = removalTimes.toDouble/successfulRemovals
    val successRate = ((successfulInsertions+successfulRemovals.toDouble)/testRuns)*100

    println(s"${"="*20}Report${"="*20}\n"+
      s"Heapsize = $heapSize, children count = $chCount\n"+
      s"${"%.2f".format(successRate)}% of operations were valid\n"+
      s"$successfulInsertions insertions which took on average ${"%.2f".format(avgIns)} cycles\n"+
      s"$successfulRemovals removals which took on average ${"%.2f".format(avgRem)} cycles\n"+
      s"${"="*46}"
    )

    cr.printReport()
  }

  /////////////////////////////////////////////////////tests////////////////////////////////////////////////////////////

  "HeapPriorityQueue pass a single random poke test run" in {
    val heapSize = 17
    val chCount = 4
    test(new HeapPriorityQueue(heapSize,chCount,cWid,nWid,rWid)) { c => tester(c,heapSize,chCount,0,100)}
  }

  "HeapPriorityQueue should pass random poke test runs with different memory sizes and children count" in {
    val sizes = Array(33,65,129,257)
    val chCounts = Array(2,4,8,16)
    sizes.foreach(heapSize => chCounts.foreach(chCount => {
      test(new HeapPriorityQueue(heapSize,chCount,cWid,nWid,rWid)) { c => tester(c,heapSize,chCount,0,100)}
    }))
  }

}