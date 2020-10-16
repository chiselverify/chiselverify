package heappriorityqueue

import chisel3.iotesters.PeekPokeTester
import org.scalatest.{FlatSpec, Matchers}

class PriorityQueueTests extends FlatSpec with Matchers {
  val cWid = 2
  val nWid = 8
  val rWid = 5
  val heapSize = 9
  val chCount = 2
  val debugLvl = 1
  "HeapPrioQ" should "pass" in {
    chisel3.iotesters.Driver(() => new HeapPriorityQueue(heapSize,chCount,nWid,cWid,rWid)) {
      c => {
        val dut = new HeapPriorityQueueWrapper(c,heapSize,chCount,debugLvl)(cWid,nWid,rWid)
        val toBeInserted = Array(Array(2,55),Array(2,45),Array(0,240),Array(1,2),Array(0,241),Array(0,239),Array(2,76),Array(1,111),Array(0,233),Array(1,123))
        for(i <- toBeInserted.indices) {
          dut.insert(toBeInserted(i)(0),toBeInserted(i)(1),i)
        }
        dut.printMem()
        dut.remove(1,true)
        dut.insert(0,238,11,true)
        dut.remove(11,true)
        dut.remove(1,true)
        dut.remove(3,true)
        dut.remove(2,true)
        dut.insert(0,22,23,true)
        dut.insert(1,22,21,true)
        dut
      }
    } should be(true)
  }
}

/**
 * Wrapper class to abstract interaction with the heap-based priority queue
 * @param dut a HeapPriorityQueue instance
 * @param size size of the heap
 * @param chCount number of children per node
 * @param debugLvl 0=no output, 1=operation reviews, 2=step-wise outputs
 * @param cWid  width of cyclic priorities
 * @param nWid  width of normal priorities
 * @param rWid  width of reference IDs
 */
private class HeapPriorityQueueWrapper(dut: HeapPriorityQueue, size: Int, chCount: Int, debugLvl: Int)(cWid : Int,nWid : Int,rWid : Int) extends PeekPokeTester(dut){
  var pipedRdAddr = 0
  var pipedWrAddr = 0
  var searchSimDelay = 0
  var stepCounter = 0
  val states = Array("idle" ,"headInsertion", "normalInsertion", "initSearch", "waitForSearch", "resetCell", "lastRemoval", "headRemoval", "tailRemoval" ,"removal", "waitForHeapifyUp", "waitForHeapifyDown")
  val heapifierStates = Array("idle", "warmUp1", "warmDown1", "warmUp2", "warmDown2", "readUp", "readDown", "wbUp1", "wbDown1", "wbUp2", "wbDown2")
  val mem = Array.fill(size-1)(Array(Math.pow(2,cWid).toInt-1,Math.pow(2,nWid).toInt-1,Math.pow(2,rWid).toInt-1)).sliding(chCount,chCount).toArray
  def stepDut(n: Int) : Unit = {
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
        if(searchSimDelay > 3){
          var idx = peek(dut.io.head.refID).toInt
          if(idx != peek(dut.io.srch.refID).toInt){
            idx = mem.flatten.map(_(2)==peek(dut.io.srch.refID)).indexOf(true) + 1
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

      if (debugLvl >= 2) {
        println(s"States: ${states(peek(dut.io.debug.state).toInt)} || ${heapifierStates(peek(dut.io.debug.heapifierState).toInt)} at index ${peek(dut.io.debug.heapifierIndex)}\n"+
          s"${peek(dut.io.debug.heapSize)} \n"+
          s"ReadPort: ${peek(dut.io.rdPort.address)} | ${mem.apply(pipedRdAddr).map(_.mkString(":")).mkString(",")}\n"+
          s"WritePort: ${peek(dut.io.wrPort.address)} | ${peek(dut.io.wrPort.data).sliding(3,3).map(_.mkString(":")).mkString(",")} | ${peek(dut.io.wrPort.write)} | ${peek(dut.io.wrPort.mask).toString(2)}\n"+
          s"Memory:\n${peek(dut.io.head.prio).values.mkString(":")}:${peek(dut.io.head.refID)}\n${mem.map(_.map(_.mkString(":")).mkString(", ")).mkString("\n")}")
      }

      // simulate synchronous memory
      pipedRdAddr = peek(dut.io.rdPort.address).toInt
      pipedWrAddr = peek(dut.io.wrPort.address).toInt

      step(1)
      stepCounter += 1
    }
  }
  def stepUntilDone(max: Int = Int.MaxValue) : Unit = {
    var iterations = 0
    while(iterations < max && (peek(dut.io.cmd.done)==0 || iterations < 1)){
      stepDut(1)
      iterations += 1
    }
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
  def insert(c: Int, n: Int, id: Int, printMemory: Boolean = false) : Int = {
    if(debugLvl >= 2){
      println(s"Inserting $c:$n:$id-------------------------")
    }
    pokePrioAndID(n,c,id)
    poke(dut.io.cmd.op, 1)
    poke(dut.io.cmd.valid,1)
    stepCounter = 0
    stepUntilDone()
    poke(dut.io.cmd.valid,0)
    if(debugLvl >= 1) println(s"Inserting $c:$n:$id ${if(peek(dut.io.cmd.result)==1)"failed" else "success"} in ${stepCounter.toString} cycles")
    if(printMemory) printMem()
    stepCounter
  }
  def remove(id: Int, printMemory: Boolean = false) : Int = {
    if(debugLvl >= 2){
      println(s"Removing $id--------------------------------")
    }
    pokeID(id)
    poke(dut.io.cmd.op, 0)
    poke(dut.io.cmd.valid,1)
    stepCounter = 0
    stepUntilDone(40)
    poke(dut.io.cmd.valid, 0)
    if(debugLvl >= 1) println(s"Remove ID=$id ${if(peek(dut.io.cmd.result)==1)"failed" else "success: "+peek(dut.io.cmd.rm_prio).values.mkString(":")} in ${stepCounter.toString} cycles")
    if(printMemory) printMem()
    stepCounter
  }
  def printMem() : Unit = {
    println(s"Memory:\n${peek(dut.io.head.prio).values.mkString(":")}:${peek(dut.io.head.refID)}\n${mem.map(_.map(_.mkString(":")).mkString(", ")).mkString("\n")}")
  }
  def printRmPrio() : Unit = {
    println(s"rm_prio: ${peek(dut.io.cmd.rm_prio).values.mkString(":")}")
  }
  def printSuccess() : Unit = {
    println(s"${if(peek(dut.io.cmd.result)==1)"failed" else "success"}")
  }
}