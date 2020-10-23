package heappriorityqueue

import chisel3.iotesters.PeekPokeTester

/**
 * Wrapper class to abstract interaction with the heap-based priority queue
 * @param dut a HeapPriorityQueue instance
 * @param size size of the heap
 * @param chCount number of children per node
 * @param debug 0=no output, 1=operation reviews, 2=step-wise outputs
 * @param cWid  width of cyclic priorities
 * @param nWid  width of normal priorities
 * @param rWid  width of reference IDs
 */
class HeapPriorityQueueWrapper(dut: HeapPriorityQueue, size: Int, chCount: Int, debug: Int)(cWid : Int,nWid : Int,rWid : Int) extends PeekPokeTester(dut){
  var pipedRdAddr = 0
  var pipedWrAddr = 0
  var searchSimDelay = 0
  var stepCounter = 0
  var totalSteps = 0
  var debugLvl = debug
  val states = Array("idle" ,"headInsertion", "normalInsertion", "initSearch", "waitForSearch", "resetCell", "lastRemoval", "headRemoval", "tailRemoval" ,"removal", "waitForHeapifyUp", "waitForHeapifyDown")
  val heapifierStates = Array("idle", "warmUp1", "warmDown1", "warmUp2", "warmDown2", "readUp", "readDown", "wbUp1", "wbDown1", "wbUp2", "wbDown2")

  var mem = Array.fill(size-1)(Array(Math.pow(2,cWid).toInt-1,Math.pow(2,nWid).toInt-1,Math.pow(2,rWid).toInt-1)).sliding(chCount,chCount).toArray

  //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
  def stepDut(n: Int) : String = {
    val str = new StringBuilder
    for(i <- 0 until n){
      // read port
      try {
        for (i <- 0 until chCount) {
          // ignores reads outside of array
          poke(dut.io.rdPort.data(i).prio.cycl, mem(pipedRdAddr)(i)(0))
          poke(dut.io.rdPort.data(i).prio.norm, mem(pipedRdAddr)(i)(1))
          poke(dut.io.rdPort.data(i).id, mem(pipedRdAddr)(i)(2))
        }
      } catch {
        case e: IndexOutOfBoundsException => {}
      }
      // write port
      if (peek(dut.io.wrPort.write) == 1) {
        for (i <- 0 until chCount) {
          if((peek(dut.io.wrPort.mask) & (BigInt(1) << i)) != 0){
            mem(pipedWrAddr)(i)(0) = peek(dut.io.wrPort.data(i).prio.cycl).toInt
            mem(pipedWrAddr)(i)(1) = peek(dut.io.wrPort.data(i).prio.norm).toInt
            mem(pipedWrAddr)(i)(2) = peek(dut.io.wrPort.data(i).id).toInt
          }
        }
      }
      // search port
      if(peek(dut.io.srch.search)==1){
        if(searchSimDelay > 0){
          var idx = peek(dut.io.head.refID).toInt
          if(idx != peek(dut.io.srch.refID).toInt){
            idx = mem.flatten.map(_(2)==peek(dut.io.srch.refID).toInt).indexOf(true) + 1
          }
          if(idx == 0){
            poke(dut.io.srch.error, 1)
          }else{
            poke(dut.io.srch.res, idx)
          }
          poke(dut.io.srch.done, 1)
          searchSimDelay = 0
        }else{
          poke(dut.io.srch.done, 0)
          poke(dut.io.srch.error, 0)
          searchSimDelay += 1
        }
      }else{
        searchSimDelay = 0
      }


      str.append(
        s"${states(peek(dut.io.state).toInt)}\n"+
          s"ReadPort: ${peek(dut.io.rdPort.address)} | ${mem.apply(pipedRdAddr).map(_.mkString(":")).mkString(",")}\n"+
          s"WritePort: ${peek(dut.io.wrPort.address)} | ${peek(dut.io.wrPort.data).sliding(3,3).map(_.mkString(":")).mkString(",")} | ${peek(dut.io.wrPort.write)} | ${peek(dut.io.wrPort.mask).toString(2).reverse}\n"+
          getMem()+s"\n${"-"*40}\n"
      )


      // simulate synchronous memory
      pipedRdAddr = peek(dut.io.rdPort.address).toInt
      pipedWrAddr = peek(dut.io.wrPort.address).toInt

      step(1)
      stepCounter += 1
      totalSteps += 1
    }
    return str.toString
  }

  def stepUntilDone(max: Int = Int.MaxValue) : String = {
    var iterations = 0
    val str = new StringBuilder
    while(iterations < max && (peek(dut.io.cmd.done)==0 || iterations < 1)){
      str.append(stepDut(1))
      iterations += 1
    }
    return str.toString
  }

  def pokeID(id: Int) : Unit = {
    poke(dut.io.cmd.refID, id)
  }

  def pokePriority(c: Int, n: Int) : Unit = {
    poke(dut.io.cmd.prio.norm,n)
    poke(dut.io.cmd.prio.cycl,c)
  }

  def pokePrioAndID(c: Int, n: Int, id: Int) : Unit = {
    pokePriority(n,c)
    pokeID(id)
  }

  def insert(c: Int, n: Int, id: Int) : (Int, Boolean, String) = {
    if(debugLvl >= 2) println(s"Inserting $c:$n:$id${"-"*20}")
    pokePrioAndID(n,c,id)
    poke(dut.io.cmd.op, 1)
    poke(dut.io.cmd.valid,1)
    stepCounter = 0
    val debug = stepUntilDone()
    poke(dut.io.cmd.valid,0)
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
    poke(dut.io.cmd.op, 0)
    poke(dut.io.cmd.valid,1)
    stepCounter = 0
    val debug = stepUntilDone()
    poke(dut.io.cmd.valid, 0)
    if(debugLvl >= 1) println(s"Remove ID=$id ${if(!getSuccess())"failed" else "success: "+getRmPrio().mkString(":")} in $stepCounter cycles")
    (stepCounter,getSuccess(),debug)
  }

  def printMem(style: Int = 0) : Unit = {
    if(style==0) println(s"DUT: ${peek(dut.io.head.prio).values.mkString(":")}:${peek(dut.io.head.refID)} | ${mem.map(_.map(_.mkString(":")).mkString(", ")).mkString(" | ")}")
    else if(style==1) println(s"DUT:\n${peek(dut.io.head.prio).values.mkString(":")}:${peek(dut.io.head.refID)}\n${mem.map(_.map(_.mkString(":")).mkString(", ")).mkString("\n")}")
  }

  def getMem() : String = {
    return s"${peek(dut.io.head.prio).values.mkString(":")}:${peek(dut.io.head.refID)} | ${mem.map(_.map(_.mkString(":")).mkString(", ")).mkString(" | ")}"
  }

  def getRmPrio() : Array[Int] = {
    return peek(dut.io.cmd.rm_prio).values.map(_.toInt).toArray
  }

  def getSuccess() : Boolean = {
    return peek(dut.io.cmd.result)==0
  }

  def compareWithModel(arr: Array[Array[Int]]) : Boolean = {
    var res = true
    res &= expect(dut.io.head.prio.cycl, arr(0)(0))
    res &= expect(dut.io.head.prio.norm, arr(0)(1))
    res &= expect(dut.io.head.refID, arr(0)(2))
    res &= expect(arr.slice(1,arr.length).deep == mem.flatten.deep,"Memory did not match")
    return res
  }

  def setDebugLvl(lvl: Int) : Unit = {
    debugLvl = lvl
  }
}