package heappriorityqueue

import org.scalatest._
import chiseltest._
import chisel3._
import coverage.Coverage.{Bins, CoverPoint}
import coverage.CoverageReporter

class PriorityQueueCoverageTest extends FreeSpec with ChiselScalatestTester {
  val heapSize = 17
  val chCount = 4
  val debugLvl = 0
  val cWid = 2
  val nWid = 8
  val rWid = 3
  val testRuns = 100
  "HeapPriorityQueue should do well" in {
    test(new HeapPriorityQueue(heapSize,chCount,nWid,cWid,rWid)) { c =>

      val cr = new CoverageReporter

      //single bit signals can only score 1 hit due to ".distinct"

      // setup cover points and bins
      cr.register(
        CoverPoint(c.io.cmd.op, "op",
          Bins("insertion", 0 to 0)::Bins("removal", 1 to 1)::Nil)::
        CoverPoint(c.io.head.prio.cycl, "head cycl",
          Bins("0 cyclic", 0 to 0)::Bins("1 cyclic", 1 to 1)::Bins("2 cyclic", 2 to 2)::Bins("3 cyclic", 3 to 3)::Nil)::
        CoverPoint(c.io.head.prio.norm, "head norm",
          Bins("lower half", 0 to (Math.pow(2,nWid)/2-1).toInt)::Bins("upper half", (Math.pow(2,nWid)/2-1).toInt to (Math.pow(2,nWid)-1).toInt)::Nil)::
        CoverPoint(c.io.cmd.prio.cycl, "cmd cycl",
          Bins("0 cyclic", 0 to 0)::Bins("1 cyclic", 1 to 1)::Bins("2 cyclic", 2 to 2)::Bins("3 cyclic", 3 to 3)::Nil)::
        CoverPoint(c.io.cmd.prio.norm, "cmd norm",
          Bins("lower half", 0 to (Math.pow(2,nWid)/2-1).toInt)::Bins("upper half", (Math.pow(2,nWid)/2-1).toInt to (Math.pow(2,nWid)-1).toInt)::Nil)::
        Nil)

      // instantiate wrapper class and model
      val rand = scala.util.Random
      val dut = new HeapPriorityQueueWrapper2(c, heapSize, chCount, debugLvl)(cWid, nWid, rWid)
      val model = new Behavioural(heapSize, chCount)(cWid, nWid, rWid)

      var refIDcounter = 0

      // tracking variables
      var stepCounter = 0
      var successfulInsertions = 0
      var successfulRemovals = 0
      var insertionTimes = 0
      var removalTimes = 0

      cr.sample()

      for (i <- 0 until testRuns) {
        var debug = ""
        if (rand.nextInt(2) == 1) { // insert operation
          // determine what to insert
          val ins = Array(rand.nextInt(math.pow(2, cWid).toInt), rand.nextInt(math.pow(2, nWid).toInt), refIDcounter)
          // increment reference ID counter and wrap if necessary
          refIDcounter += 1
          if (refIDcounter >= Math.pow(2, rWid)) refIDcounter = 0

          // simulate dut and model
          val (steps, success, debugStr) = dut.insert(ins(0), ins(1), ins(2))
          model.insert(ins(0), ins(1), ins(2))

          // process results
          debug = debugStr
          stepCounter += steps
          if (success) {
            insertionTimes += steps
            successfulInsertions += 1
          }
        } else { // remove operation
          // determine removal reference ID
          val rem = rand.nextInt(math.pow(2, rWid).toInt - 1)

          // simulate dut and model
          val (steps, success, debugStr) = dut.remove(rem)
          model.remove(rem)

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
        if (!dut.compareWithModel(model.mem)) {
          println(debug)
          sys.exit(0)
        }
      }
      cr.printReport()
    }
  }
}

class HeapPriorityQueueWrapper2(dut: HeapPriorityQueue, size: Int, chCount: Int, debug: Int)(cWid : Int, nWid : Int, rWid : Int){
  var pipedRdAddr = 0
  var pipedWrAddr = 0
  var searchSimDelay = 0
  var stepCounter = 0
  var totalSteps = 0
  var debugLvl = debug
  val states = Array("idle" ,"headInsertion", "normalInsertion", "initSearch", "waitForSearch", "resetCell", "lastRemoval", "headRemoval", "tailRemoval" ,"removal", "waitForHeapifyUp", "waitForHeapifyDown")

  var mem = Array.fill(size-1)(Array(Math.pow(2,cWid).toInt-1,Math.pow(2,nWid).toInt-1,Math.pow(2,rWid).toInt-1)).sliding(chCount,chCount).toArray

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  def stepDut(n: Int) : String = {
    val str = new StringBuilder
    for(i <- 0 until n){
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
          if((dut.io.wrPort.mask.peek.litValue & (1 << i)) != 0){
            mem(pipedWrAddr)(i)(0) = dut.io.wrPort.data(i).prio.cycl.peek.litValue.toInt
            mem(pipedWrAddr)(i)(1) = dut.io.wrPort.data(i).prio.norm.peek.litValue.toInt
            mem(pipedWrAddr)(i)(2) = dut.io.wrPort.data(i).id.peek.litValue.toInt
          }
        }
      }
      // search port
      if(dut.io.srch.search.peek.litToBoolean){
        if(searchSimDelay > 3){
          var idx = dut.io.head.refID.peek.litValue.toInt
          if(idx != dut.io.srch.refID.peek.litValue.toInt){
            idx = mem.flatten.map(_(2)==dut.io.srch.refID.peek.litValue.toInt).indexOf(true) + 1
          }
          if(idx == 0){
            dut.io.srch.error.poke(true.B)
          }else{
            dut.io.srch.res.poke(idx.U)
          }
          dut.io.srch.done.poke(true.B)
          searchSimDelay = 0
        }else{
          dut.io.srch.done.poke(false.B)
          dut.io.srch.error.poke(false.B)
          searchSimDelay += 1
        }
      }else{
        searchSimDelay = 0
      }

      // TODO: how can we read vectors of bundles with peek? error:
      //don't know how to peek PriorityAndID[4](IO io_wrPort_data in HeapPriorityQueue)
      //chiseltest.LiteralTypeException: don't know how to peek PriorityAndID[4](IO io_wrPort_data in HeapPriorityQueue)
      str.append(
        s"${states(dut.io.state.peek.litValue.toInt)}\n"+
          s"ReadPort: ${dut.io.rdPort.address.peek.litValue} | ${mem.apply(pipedRdAddr).map(_.mkString(":")).mkString(",")}\n"+
          s"WritePort: ${dut.io.wrPort.address.peek.litValue} | ${dut.io.wrPort.data.mkString/*peek.map(_.getElements).map(_.map(_.litValue.toInt)).map(_.mkString(":")).mkString(",")*/} | ${dut.io.wrPort.write.peek.litToBoolean} | ${dut.io.wrPort.mask.peek.litValue.toString(2).reverse}\n"+
          getMem()+s"\n${"-"*40}\n"
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

  def stepUntilDone(max: Int = Int.MaxValue) : String = {
    var iterations = 0
    val str = new StringBuilder
    while(iterations < max && (!dut.io.cmd.done.peek.litToBoolean || iterations < 1)){
      str.append(stepDut(1))
      iterations += 1
    }
    return str.toString
  }

  def pokeID(id: Int) : Unit = {
    dut.io.cmd.refID.poke(id.U)
  }

  def pokePriority(c: Int, n: Int) : Unit = {
    dut.io.cmd.prio.norm.poke(n.U)
    dut.io.cmd.prio.cycl.poke(c.U)
  }

  def pokePrioAndID(c: Int, n: Int, id: Int) : Unit = {
    pokePriority(n,c)
    pokeID(id)
  }

  def insert(c: Int, n: Int, id: Int) : (Int, Boolean, String) = {
    if(debugLvl >= 2) println(s"Inserting $c:$n:$id${"-"*20}")
    pokePrioAndID(n,c,id)
    dut.io.cmd.op.poke(true.B)
    dut.io.cmd.valid.poke(true.B)
    stepCounter = 0
    val debug = stepUntilDone()
    dut.io.cmd.valid.poke(false.B)
    if(debugLvl >= 1) println(s"Inserting $c:$n:$id ${if(!getSuccess())"failed" else "success"} in $stepCounter cycles")
    return (stepCounter,getSuccess(),debug)
  }

  def insert(arr: Array[Array[Int]]) : (Int, Boolean) = {
    var success = true
    var steps = 0
    for(i <- arr){
      val ret = insert(i(0),i(1),i(2))
      success &= ret._2
      steps += ret._1
    }
    return (steps, success)
  }

  def remove(id: Int) : (Int, Boolean, String) = {
    if(debugLvl >= 2) println(s"Removing $id${"-"*20}")
    pokeID(id)
    dut.io.cmd.op.poke(false.B)
    dut.io.cmd.valid.poke(true.B)
    stepCounter = 0
    val debug = stepUntilDone()
    dut.io.cmd.valid.poke(false.B)
    if(debugLvl >= 1) println(s"Remove ID=$id ${if(!getSuccess())"failed" else "success: "+getRmPrio().mkString(":")} in $stepCounter cycles")
    (stepCounter,getSuccess(),debug)
  }

  def printMem(style: Int = 0) : Unit = {
    if(style==0) println(s"DUT: ${dut.io.head.prio.peek.getElements.map(_.litValue.toInt).mkString(":")}:${dut.io.head.refID.peek.litValue} | ${mem.map(_.map(_.mkString(":")).mkString(", ")).mkString(" | ")}")
    else if(style==1) println(s"DUT:\n${dut.io.head.prio.peek.getElements.mkString(":")}:${dut.io.head.refID.peek.litValue}\n${mem.map(_.map(_.mkString(":")).mkString(", ")).mkString("\n")}")
  }

  def getMem() : String = {
    return s"${dut.io.head.prio.peek.getElements.mkString(":")}:${dut.io.head.refID.peek.litValue} | ${mem.map(_.map(_.mkString(":")).mkString(", ")).mkString(" | ")}"
  }

  def getRmPrio() : Array[Int] = {
    return dut.io.cmd.rm_prio.peek.getElements.map(_.litValue.toInt).toArray
  }

  def getSuccess() : Boolean = {
    return !dut.io.cmd.result.peek.litToBoolean
  }

  def compareWithModel(arr: Array[Array[Int]]) : Boolean = {
    // TODO: can we make expect return a boolean?
    var res = true
    dut.io.head.prio.cycl.expect(arr(0)(0).U)
    res &= dut.io.head.prio.cycl.peek.litValue == arr(0)(0)
    dut.io.head.prio.norm.expect(arr(0)(1).U)
    res &= dut.io.head.prio.norm.peek.litValue == arr(0)(1)
    dut.io.head.refID.expect(arr(0)(2).U)
    res &= dut.io.head.refID.peek.litValue == arr(0)(2)
    res &= arr.slice(1,arr.length).deep == mem.flatten.deep
    return res
  }

  def setDebugLvl(lvl: Int) : Unit = {
    debugLvl = lvl
  }
}
