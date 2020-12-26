package examples.heappriorityqueue

import chisel3._
import chiseltest._
import chiselverify.coverage.{Bins, CoverPoint, CoverageReporter, Cross, CrossBin, CrossPoint}
import examples.heappriorityqueue.Helpers._
import examples.heappriorityqueue.modules.QueueControl
import org.scalatest._

/** contains randomized tests for the queue controller
  */
class QueueControlTest extends FreeSpec with ChiselScalatestTester {

  val cWid = 2
  val nWid = 8
  val rWid = 3

  "HeapPriorityQueue pass a single random poke test run" in {
    val heapSize = 17
    val chCount = 4
    test(new QueueControl(heapSize, chCount, cWid, nWid, rWid)) { c => tester(c, heapSize, chCount, 0, 100) }
  }

  "HeapPriorityQueue should pass random poke test runs with different memory sizes and children count" in {
    val sizes = Array(33, 65, 129, 257)
    val chCounts = Array(2, 4, 8, 16)
    sizes.foreach(heapSize =>
      chCounts.foreach(chCount => {
        test(new QueueControl(heapSize, chCount, cWid, nWid, rWid)) { c => tester(c, heapSize, chCount, 0, 50) }
      })
    )
  }

  def tester(c: QueueControl, heapSize: Int, chCount: Int, debugLvl: Int, testRuns: Int): Unit = {
    var stepCounter = 0
    var successfulInsertions = 0
    var successfulRemovals = 0
    var insertionTimes = 0
    var removalTimes = 0
    var refIDcounter = 0

    val rand = scala.util.Random

    val dut = new QueueControlWrapper(c, heapSize, chCount, debugLvl)(cWid, nWid, rWid)
    c.clock.setTimeout(0)
    val model = new Behavioural(heapSize, chCount)(cWid, nWid, rWid)

    ////////////////////////////////////////////////////Coverage////////////////////////////////////////////////////////

    val cr = new CoverageReporter(c)
    cr.register(
      CoverPoint(c.io.cmd.op, "operation")(Bins("insertion", 0 to 0) :: Bins("removal", 1 to 1) :: Nil) ::
        CoverPoint(c.io.cmd.prio.cycl, "cmd.prio.cycl")(Bins("cyclic", 0 to 3) :: Nil) ::
        CoverPoint(c.io.cmd.prio.norm, "cmd.prio.norm")(
          Bins("lower half", 0 to (Math.pow(2, nWid) / 2 - 1).toInt) :: Bins(
            "upper half",
            (Math.pow(2, nWid) / 2 - 1).toInt to (Math.pow(2, nWid) - 1).toInt
          ) :: Nil
        ) ::
        CoverPoint(c.io.head.prio.cycl, "head.prio.cycl")(Bins("cyclic", 0 to 3) :: Nil) ::
        CoverPoint(c.io.head.prio.norm, "head.prio.norm")(
          Bins("lower half", 0 to (Math.pow(2, nWid) / 2 - 1).toInt) :: Bins(
            "upper half",
            (Math.pow(2, nWid) / 2 - 1).toInt to (Math.pow(2, nWid) - 1).toInt
          ) :: Nil
        ) ::
        Nil,
      //Declare cross points
      CrossPoint("cyclics at ops", "operation", "cmd.prio.cycl")(
        CrossBin("insertion", 0 to 0, 0 to 3) :: CrossBin("removal", 1 to 1, 0 to 3) :: Nil
      ) ::
        CrossPoint("normals at ops", "operation", "cmd.prio.norm")(
          CrossBin("insertion lower half", 0 to 0, 0 to (Math.pow(2, nWid) / 2 - 1).toInt) :: CrossBin(
            "insertion upper half",
            0 to 0,
            (Math.pow(2, nWid) / 2 - 1).toInt to (Math.pow(2, nWid) - 1).toInt
          ) ::
            CrossBin("removal lower half", 1 to 1, 0 to (Math.pow(2, nWid) / 2 - 1).toInt) :: CrossBin(
              "removal upper half",
              1 to 1,
              (Math.pow(2, nWid) / 2 - 1).toInt to (Math.pow(2, nWid) - 1).toInt
            ) :: Nil
        ) ::
        Nil
    )

    ////////////////////////////////////////////////helper functions////////////////////////////////////////////////////

    def randomPoke(): Seq[Int] = {
      // op, cprio, nprio, refID
      val ret =
        Seq(rand.nextInt(2), rand.nextInt(math.pow(2, cWid).toInt), rand.nextInt(math.pow(2, nWid).toInt), refIDcounter)
      refIDcounter += 1
      if (refIDcounter >= Math.pow(2, rWid) - 1) refIDcounter = 0
      ret
    }

    def test(values: Seq[Seq[Int]]): Unit = {
      for (poke <- values) {
        var debug = ""
        if (poke.head == 0) { // insert operation
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
        assert(c.io.head.prio.cycl.peek.litValue == model.mem(0)(0), debug)
        assert(c.io.head.prio.norm.peek.litValue == model.mem(0)(1), debug)
        assert(c.io.head.refID.peek.litValue == model.mem(0)(2), debug)
        assert(model.mem.slice(1, model.mem.length).deep == dut.mem.flatten.deep, debug)
      }
    }

    //////////////////////////////////////////////////////testing/////////////////////////////////////////////////////////

    val pokes = Seq.fill(testRuns)(randomPoke())

    test(pokes)

    ///////////////////////////////////////////////////reports//////////////////////////////////////////////////////////

    val avgIns = insertionTimes.toDouble / successfulInsertions
    val avgRem = removalTimes.toDouble / successfulRemovals
    val successRate = ((successfulInsertions + successfulRemovals.toDouble) / testRuns) * 100

    println(
      s"${"=" * 20}Report${"=" * 20}\n" +
        s"Heapsize = $heapSize, children count = $chCount\n" +
        s"${"%.2f".format(successRate)}% of operations were valid\n" +
        s"$successfulInsertions insertions which took on average ${"%.2f".format(avgIns)} cycles\n" +
        s"$successfulRemovals removals which took on average ${"%.2f".format(avgRem)} cycles\n" +
        s"${"=" * 46}"
    )

    cr.printReport()
  }

  /////////////////////////////////////////////////////tests////////////////////////////////////////////////////////////

}

/** Wrapper class to abstract interaction with the heap-based priority queue
  *
  * @param dut     a HeapPriorityQueue instance
  * @param size    size of the heap
  * @param chCount number of children per node
  * @param debug   0=no output, 1=operation reviews, 2=step-wise outputs
  * @param cWid    width of cyclic priorities
  * @param nWid    width of normal priorities
  * @param rWid    width of reference IDs
  */
class QueueControlWrapper(dut: QueueControl, size: Int, chCount: Int, debug: Int)(cWid: Int, nWid: Int, rWid: Int) {
  var pipedRdAddr = 0
  var pipedWrAddr = 0
  var searchSimDelay = 0
  var stepCounter = 0
  var totalSteps = 0
  var debugLvl = debug
  val states = Array(
    "idle",
    "headInsertion",
    "normalInsertion",
    "initSearch",
    "waitForSearch",
    "resetCell",
    "lastRemoval",
    "headRemoval",
    "tailRemoval",
    "removal",
    "waitForHeapifyUp",
    "waitForHeapifyDown"
  )

  var mem = Array
    .fill(size - 1)(Array(Math.pow(2, cWid).toInt - 1, Math.pow(2, nWid).toInt - 1, Math.pow(2, rWid).toInt - 1))
    .sliding(chCount, chCount)
    .toArray
  dut.io.srch.done.poke(true.B)

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  def stepDut(n: Int): String = {
    val str = new StringBuilder
    for (i <- 0 until n) {
      // read port
      try {
        for (i <- 0 until chCount) {
          // ignores reads outside of array
          dut.io.rdPort.data(i).prio.cycl.poke(mem(pipedRdAddr)(i)(0).U)
          dut.io.rdPort.data(i).prio.norm.poke(mem(pipedRdAddr)(i)(1).U)
          dut.io.rdPort.data(i).id.poke(mem(pipedRdAddr)(i)(2).U)
        }
      } catch {
        case e: IndexOutOfBoundsException => {}
      }
      // write port
      if (dut.io.wrPort.write.peek.litToBoolean) {
        for (i <- 0 until chCount) {
          if ((dut.io.wrPort.mask.peek.litValue & (1 << i)) != 0) {
            mem(pipedWrAddr)(i)(0) = dut.io.wrPort.data(i).prio.cycl.peek.litValue.toInt
            mem(pipedWrAddr)(i)(1) = dut.io.wrPort.data(i).prio.norm.peek.litValue.toInt
            mem(pipedWrAddr)(i)(2) = dut.io.wrPort.data(i).id.peek.litValue.toInt
          }
        }
      }
      // search port
      if (dut.io.srch.search.peek.litToBoolean) {
        if (searchSimDelay > 1) {
          var idx = 0
          if (!(dut.io.head.refID.peek.litValue == dut.io.srch.refID.peek.litValue)) {
            idx = mem.flatten.map(_(2) == dut.io.srch.refID.peek.litValue.toInt).indexOf(true)
            if (idx == -1) {
              dut.io.srch.error.poke(true.B)
            } else {
              dut.io.srch.res.poke((idx + 1).U)
            }
          } else {
            dut.io.srch.res.poke(0.U)
          }
          dut.io.srch.done.poke(true.B)
          searchSimDelay = 0
        } else {
          dut.io.srch.done.poke(false.B)
          dut.io.srch.error.poke(false.B)
          searchSimDelay += 1
        }
      } else {
        dut.io.srch.done.poke(true.B)
        searchSimDelay = 0
      }
      str.append(
        s"${states(dut.io.state.peek.litValue.toInt)} : ${dut.io.cmd.done.peek.litValue} : ${dut.io.cmd.result.peek.litValue}\n" +
          s"ReadPort: ${dut.io.rdPort.address.peek.litValue} | ${if (pipedRdAddr < size / chCount)
            mem(pipedRdAddr).map(_.mkString(":")).mkString(",")
          else ""}\n" +
          s"WritePort: ${dut.io.wrPort.address.peek.litValue} | ${prioAndIdVecToString(
            peekPrioAndIdVec(dut.io.wrPort.data)
          )} | ${dut.io.wrPort.write.peek.litToBoolean} | ${dut.io.wrPort.mask.peek.litValue.toString(2).reverse}\n" +
          getMem() + s"\n${"-" * 40}\n"
      )

      // simulate synchronous memory
      pipedRdAddr = dut.io.rdPort.address.peek.litValue.toInt
      pipedWrAddr = dut.io.wrPort.address.peek.litValue.toInt

      dut.clock.step(1)
      stepCounter += 1
      totalSteps += 1
    }
    return str.toString
  }

  def stepUntilDone(): String = {
    var iterations = 0
    val str = new StringBuilder
    str.append(stepDut(1))
    while (!dut.io.cmd.done.peek.litToBoolean) {
      str.append(stepDut(1))
    }
    return str.toString
  }

  def pokeID(id: Int): Unit = {
    dut.io.cmd.refID.poke(id.U)
  }

  def pokePriority(c: Int, n: Int): Unit = {
    dut.io.cmd.prio.norm.poke(n.U)
    dut.io.cmd.prio.cycl.poke(c.U)
  }

  def pokePrioAndID(c: Int, n: Int, id: Int): Unit = {
    pokePriority(c, n)
    pokeID(id)
  }

  def insert(c: Int, n: Int, id: Int): (Int, Boolean, String) = {
    if (debugLvl >= 2) println(s"Inserting $c:$n:$id${"-" * 20}")
    pokePrioAndID(c, n, id)
    dut.io.cmd.op.poke(true.B)
    dut.io.cmd.valid.poke(true.B)
    stepCounter = 0
    val debug = stepUntilDone()
    dut.io.cmd.valid.poke(false.B)
    if (debugLvl >= 1)
      println(s"Inserting $c:$n:$id ${if (!getSuccess()) "failed" else "success"} in $stepCounter cycles")
    return (stepCounter, getSuccess(), debug)
  }

  def insert(arr: Array[Array[Int]]): (Int, Boolean) = {
    var success = true
    var steps = 0
    for (i <- arr) {
      val ret = insert(i(0), i(1), i(2))
      success &= ret._2
      steps += ret._1
    }
    return (steps, success)
  }

  def remove(id: Int): (Int, Boolean, String) = {
    if (debugLvl >= 2) println(s"Removing $id${"-" * 20}")
    pokeID(id)
    dut.io.cmd.op.poke(false.B)
    dut.io.cmd.valid.poke(true.B)
    stepCounter = 0
    val debug = stepUntilDone()
    dut.io.cmd.valid.poke(false.B)
    if (debugLvl >= 1)
      println(
        s"Remove ID=$id ${if (!getSuccess()) "failed" else "success: " + getRmPrio().mkString(":")} in $stepCounter cycles"
      )
    (stepCounter, getSuccess(), debug)
  }

  def printMem(style: Int = 0): Unit = {
    if (style == 0) println(s"DUT: ${dut.io.head.prio.peek.getElements
      .map(_.litValue.toInt)
      .mkString(":")}:${dut.io.head.refID.peek.litValue} | ${mem.map(_.map(_.mkString(":")).mkString(", ")).mkString(" | ")}")
    else if (style == 1) println(s"DUT:\n${dut.io.head.prio.peek.getElements
      .mkString(":")}:${dut.io.head.refID.peek.litValue}\n${mem.map(_.map(_.mkString(":")).mkString(", ")).mkString("\n")}")
  }

  def getMem(): String = {
    return s"${dut.io.head.prio.cycl.peek.litValue}:${dut.io.head.prio.norm.peek.litValue}:${dut.io.head.refID.peek.litValue} | ${mem
      .map(_.map(_.mkString(":")).mkString(", "))
      .mkString(" | ")}"
  }

  def getRmPrio(): Array[Int] = {
    return Seq(dut.io.cmd.rm_prio.cycl, dut.io.cmd.rm_prio.norm).map(_.peek.litValue.toInt).toArray
  }

  def getSuccess(): Boolean = {
    return !dut.io.cmd.result.peek.litToBoolean
  }

  def compareWithModel(arr: Array[Array[Int]]): Boolean = {
    var res = true
    dut.io.head.prio.cycl.expect(arr(0)(0).U)
    res &= dut.io.head.prio.cycl.peek.litValue == arr(0)(0)
    dut.io.head.prio.norm.expect(arr(0)(1).U)
    res &= dut.io.head.prio.norm.peek.litValue == arr(0)(1)
    dut.io.head.refID.expect(arr(0)(2).U)
    res &= dut.io.head.refID.peek.litValue == arr(0)(2)
    res &= arr.slice(1, arr.length).deep == mem.flatten.deep
    return res
  }

  def setDebugLvl(lvl: Int): Unit = {
    debugLvl = lvl
  }
}
