package heappriorityqueue

import chisel3.iotesters.PeekPokeTester
import org.scalatest.{FlatSpec, Matchers}
import Behavioural._

private class HeapifyUpTest(dut: Heapifier, normalWidth: Int, cyclicWidth: Int, heapSize: Int, childrenCount: Int, debugOutput: Boolean) extends PeekPokeTester(dut) {

  // setup random memory state
  val rand = scala.util.Random
  val priorities: Array[Array[Int]] = Array.fill(heapSize)(Array(rand.nextInt(math.pow(2,cyclicWidth).toInt),rand.nextInt(math.pow(2,normalWidth).toInt)))
  val prioritiesOriginal = priorities.map(_.clone())
  // create 3d array. First index is RAM address, 2nd index selects element at RAM address, 3rd selects cyclic or normal priority value
  val Mem: Array[Array[Array[Int]]] = priorities.slice(1,priorities.length).sliding(childrenCount,childrenCount).toArray.map(_.map(_.clone()))
  // root/head element is not a part of RAM
  val root = priorities(0).clone()

  // simulate synchronous memory
  var lastReadAddr = 0
  var lastWriteAddr = 0

  // determine the last index which has children
  var lastParent = Seq.tabulate(heapSize)(i => (i * childrenCount)+1 < heapSize).lastIndexOf(true)
  // randomly set starting index
  val index = rand.nextInt(lastParent)

  // apply behavioral model to memory state
  heapifyUp(priorities, childrenCount, heapSize, index)

  // setup inputs of dut
  poke(dut.io.control.idx, index)
  poke(dut.io.control.heapifyDown,false)
  poke(dut.io.control.heapifyUp,true)
  poke(dut.io.headPort.rdData.prio.cycl,root(0))
  poke(dut.io.headPort.rdData.prio.norm,root(1))
  poke(dut.io.control.heapSize,heapSize)

  // loop variables
  var iterations = 0
  while(peek(dut.io.state).toInt!=0 || iterations < 2){

    for(i <- 0 until childrenCount){
      // ignores reads outside of array
      try {
        poke(dut.io.rdPort.data(i).prio.cycl, Mem(lastReadAddr)(i)(0))
        poke(dut.io.rdPort.data(i).prio.norm, Mem(lastReadAddr)(i)(1))
      }catch{
        case e: IndexOutOfBoundsException => {}
      }
    }
    // catch writes
    if(peek(dut.io.wrPort.write)==1){
      for(i <- 0 until childrenCount){
        Mem(lastWriteAddr)(i)(0) = peek(dut.io.wrPort.data(i).prio.cycl).toInt
        Mem(lastWriteAddr)(i)(1) = peek(dut.io.wrPort.data(i).prio.norm).toInt
      }
    }
    // catch writes to head element
    if(peek(dut.io.headPort.write).toInt == 1){
      root(0) = peek(dut.io.headPort.wrData.prio.cycl).toInt
      root(1) = peek(dut.io.headPort.wrData.prio.norm).toInt
    }
    // print states
    if(debugOutput){
      println(s"\nstate: ${peek(dut.io.state)} | index: ${peek(dut.io.indexOut)} | nextIndex: ${peek(dut.io.nextIndexOut)}\n"+
      s"ReadPort: ${peek(dut.io.rdPort.address)} | ${Mem.apply(lastReadAddr).map(_.mkString(":")).mkString(",")}\n"+
      s"WritePort: ${peek(dut.io.wrPort.address)} | ${peek(dut.io.wrPort.data).sliding(2,2).map(_.mkString(":")).mkString(",")} | ${peek(dut.io.wrPort.write)}\n"+
      s"MinInput: ${peek(dut.io.minInputs).sliding(2,2).map(_.mkString(":")).mkString(", ")} | ${peek(dut.io.out)}\n"+
      s"parentOffset: ${peek(dut.io.parentOff)}\n"+
      s"Memory:\n${root.mkString(":")}\n${Mem.map(_.map(_.mkString(":")).mkString(", ")).mkString("\n")}")
    }

    // simulate synchronous memory
    lastReadAddr = peek(dut.io.rdPort.address).toInt
    lastWriteAddr = peek(dut.io.wrPort.address).toInt

    // catch components done signal
    if(peek(dut.io.control.done).toInt == 1 && iterations > 2){
      poke(dut.io.control.heapifyUp,0)
      if(prioritiesOriginal.slice(1,prioritiesOriginal.length).deep != Mem.flatten.deep){
        expect(dut.io.control.swapped, true) // should indicate that a swap has occurred
      }

    }
    step(1)
    iterations +=1
  }
  // print out components and models results
  println(s"\nStart from index $index:\n${prioritiesOriginal.map(_.mkString(":")).mkString(", ")}\nResult:\n${root.mkString(":")}, ${Mem.flatten.map(_.mkString(":")).mkString(", ")}\nModel:\n${priorities.map(_.mkString(":")).mkString(", ")}")
  // check for equality
  expect(priorities(0).deep == root.deep, "")
  expect(priorities.slice(1,priorities.length).deep == Mem.flatten.deep,"")
}

private class HeapifyDownTest(dut: Heapifier, normalWidth: Int, cyclicWidth: Int, heapSize: Int, childrenCount: Int, debugOutput: Boolean) extends PeekPokeTester(dut) {

  // setup random memory state
  val rand = scala.util.Random
  val priorities: Array[Array[Int]] = Array.fill(heapSize)(Array(rand.nextInt(math.pow(2,cyclicWidth).toInt),rand.nextInt(math.pow(2,normalWidth).toInt)))
  val prioritiesOriginal = priorities.map(_.clone())
  // create 3d array. First index is RAM address, 2nd index selects element at RAM address, 3rd selects cyclic or normal priority value
  val Mem: Array[Array[Array[Int]]] = priorities.slice(1,priorities.length).sliding(childrenCount,childrenCount).toArray.map(_.map(_.clone()))
  // root/head element is not a part of RAM
  val root = priorities(0).clone()

  // simulate synchronous memory
  var lastReadAddr = 0
  var lastWriteAddr = 0

  // determine the last index which has children
  var lastParent = Seq.tabulate(heapSize)(i => (i * childrenCount)+1 < heapSize).lastIndexOf(true)
  // randomly set starting index
  val index = rand.nextInt(lastParent)

  // apply behavioral model to memory state
  heapifyDown(priorities, childrenCount, heapSize, index)

  // setup inputs of dut
  poke(dut.io.control.idx, index)
  poke(dut.io.control.heapifyDown,true)
  poke(dut.io.control.heapifyUp,false)
  poke(dut.io.headPort.rdData.prio.cycl,root(0))
  poke(dut.io.headPort.rdData.prio.norm,root(1))
  poke(dut.io.control.heapSize,heapSize)

  // loop variables
  var iterations = 0
  while(peek(dut.io.state).toInt!=0 || iterations < 2){

    for(i <- 0 until childrenCount){
      // ignores reads outside of array
      try {
        poke(dut.io.rdPort.data(i).prio.cycl, Mem(lastReadAddr)(i)(0))
        poke(dut.io.rdPort.data(i).prio.norm, Mem(lastReadAddr)(i)(1))
      }catch{
        case e: IndexOutOfBoundsException => {}
      }
    }
    // catch writes
    if(peek(dut.io.wrPort.write)==1){
      for(i <- 0 until childrenCount){
        Mem(lastWriteAddr)(i)(0) = peek(dut.io.wrPort.data(i).prio.cycl).toInt
        Mem(lastWriteAddr)(i)(1) = peek(dut.io.wrPort.data(i).prio.norm).toInt
      }
    }
    // catch writes to head element
    if(peek(dut.io.headPort.write).toInt == 1){
      println("head write")
      root(0) = peek(dut.io.headPort.wrData.prio.cycl).toInt
      root(1) = peek(dut.io.headPort.wrData.prio.norm).toInt
    }
    // print states
    if(debugOutput){
      println(s"\nstate: ${peek(dut.io.state)} | index: ${peek(dut.io.indexOut)} | nextIndex: ${peek(dut.io.nextIndexOut)}\n"+
        s"ReadPort: ${peek(dut.io.rdPort.address)} | ${Mem.apply(lastReadAddr).map(_.mkString(":")).mkString(",")}\n"+
        s"WritePort: ${peek(dut.io.wrPort.address)} | ${peek(dut.io.wrPort.data).sliding(2,2).map(_.mkString(":")).mkString(",")} | ${peek(dut.io.wrPort.write)}\n"+
        s"MinInput: ${peek(dut.io.minInputs).sliding(2,2).map(_.mkString(":")).mkString(", ")} | ${peek(dut.io.out)}\n"+
        s"parentOffset: ${peek(dut.io.parentOff)}\n"+
        s"Memory:\n${root.mkString(":")}\n${Mem.map(_.map(_.mkString(":")).mkString(", ")).mkString("\n")}")
    }

    // simulate synchronous memory
    lastReadAddr = peek(dut.io.rdPort.address).toInt
    lastWriteAddr = peek(dut.io.wrPort.address).toInt

    // catch components done signal
    if(peek(dut.io.control.done).toInt == 1 && iterations > 2){
      poke(dut.io.control.heapifyUp,0)
      if(prioritiesOriginal.slice(1,prioritiesOriginal.length).deep != Mem.flatten.deep){
        expect(dut.io.control.swapped, true) // should indicate that a swap has occurred
      }
    }
    step(1)
    iterations +=1
  }
  step(1)
  // print out components and models results
  println(s"\nStart from index $index:\n${prioritiesOriginal.map(_.mkString(":")).mkString(", ")}\nResult:\n${root.mkString(":")}, ${Mem.flatten.map(_.mkString(":")).mkString(", ")}\nModel:\n${priorities.map(_.mkString(":")).mkString(", ")}")

  // check for equality
  expect(priorities(0).deep == root.deep, "")
  expect(priorities.slice(1,priorities.length).deep == Mem.flatten.deep,"")
}

class HeapifierTest extends FlatSpec with Matchers {
  val nWid = 8
  val cWid = 2
  val rWid = 4
  val heapSize = 16
  val chCount = 4
  val debugOutput = false
  "Heapifier" should "heapify up" in {
    chisel3.iotesters.Driver(() => new Heapifier(heapSize,chCount,nWid,cWid,rWid)) {
      c => new HeapifyUpTest(c,nWid,cWid,heapSize,chCount,debugOutput)
    } should be(true)
  }
  "Heapifier" should "heapify down" in {
    chisel3.iotesters.Driver(() => new Heapifier(heapSize,chCount,nWid,cWid,rWid)) {
      c => new HeapifyDownTest(c,nWid,cWid,heapSize,chCount,debugOutput)
    } should be(true)
  }

}