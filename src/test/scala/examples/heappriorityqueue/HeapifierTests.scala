package examples.heappriorityqueue

import chisel3.iotesters.PeekPokeTester
import examples.heappriorityqueue.Behavioural._
import examples.heappriorityqueue.modules.Heapifier
import org.scalatest.{FlatSpec, Matchers}

/** Includes two test cases for the heapifier module
  *  - one tests the heapify up functionality on random memory states from random start positions
  *  - the other does the same for the heapify down functionality
  */
class HeapifierTest extends FlatSpec with Matchers {
  val nWid = 8
  val cWid = 2
  val rWid = 4
  val heapSize = 17
  val chCount = 4
  val debugLvl = 0
  "Heapifier" should "heapify up" in {
    chisel3.iotesters.Driver(() => new Heapifier(heapSize, chCount, cWid, nWid, rWid)) { c =>
      {
        val dut = new HeapifierWrapper(c, heapSize, chCount, debugLvl)(cWid, nWid, rWid)

        for (i <- 0 until 100) {
          // determine the heap size
          val size = dut.rand.nextInt(heapSize - chCount) + chCount
          dut.setHeapSize(size)

          // generate a memory state of the determined size
          val mem = dut.newMemoryState().map(_.clone())

          // determine the last parent in the heap and chose a random index between that one and root
          val lastParent = Seq.tabulate(heapSize)(i => (i * chCount) + 1 < size).lastIndexOf(true)
          val index = if (lastParent > 0) dut.rand.nextInt(lastParent + 1) else 0

          if (debugLvl > 0)
            println(s"Start from index $index with heapsize $size\nStart:\t${memToString(mem, chCount)}")

          // simulate dut and model
          heapifyUp(mem, chCount, size, index)
          dut.heapifyUp(index)

          // compare dut and model
          dut.compareWithModel(mem)

          if (debugLvl > 0) println(s"Dut:\t${dut.getMem()}\nModel:\t${memToString(mem, chCount)}")
        }
        dut
      }
    } should be(true)
  }
  "Heapifier" should "heapify down" in {
    chisel3.iotesters.Driver(() => new Heapifier(heapSize, chCount, cWid, nWid, rWid)) { c =>
      {
        val dut = new HeapifierWrapper(c, heapSize, chCount, debugLvl)(cWid, nWid, rWid)

        for (i <- 0 until 100) {
          // determine the heap size
          val size = dut.rand.nextInt(heapSize - chCount) + chCount
          dut.setHeapSize(size)

          // generate a memory state of the determined size
          val mem = dut.newMemoryState().map(_.clone())

          // determine the last parent in the heap and chose a random index between that one and root
          val lastParent = Seq.tabulate(heapSize)(i => (i * chCount) + 1 < size).lastIndexOf(true)
          val index = if (lastParent > 0) dut.rand.nextInt(lastParent + 1) else 0

          if (debugLvl > 0)
            println(s"Start from index $index with heapsize $size\nStart:\t${memToString(mem, chCount)}")

          // simulate dut and model
          heapifyDown(mem, chCount, size, index)
          dut.heapifyDown(index)

          // compare dut and model
          dut.compareWithModel(mem)

          if (debugLvl > 0) println(s"Dut:\t${dut.getMem()}\nModel:\t${memToString(mem, chCount)}")
        }
        dut
      }
    } should be(true)
  }

}

/** wrapper class to abstract interaction with heapifier component
  *
  * @param dut      the dut
  * @param size     the maximum size of the heap
  * @param chCount  the number of children per node
  * @param debugLvl debug detail: 0=muted, 1=operation debug, 2=stepwise debug
  * @param cWid     width of the cyclic priority
  * @param nWid     width of the normal priority
  * @param rWid     width of the reference ID
  */
private class HeapifierWrapper(dut: Heapifier, size: Int, chCount: Int, debugLvl: Int)(cWid: Int, nWid: Int, rWid: Int)
    extends PeekPokeTester(dut) {
  var pipedRdAddr = 0
  var pipedWrAddr = 0
  var stepCounter = 0
  var totalSteps = 0
  val states = Array(
    "idle",
    "warmUp1",
    "warmDown1",
    "warmUp2",
    "warmDown2",
    "readUp",
    "readDown",
    "wbUp1",
    "wbDown1",
    "wbUp2",
    "wbDown2"
  )
  val rand = scala.util.Random
  var mem = Array.tabulate(size - 1)(i => Array(i, i, i)).sliding(chCount, chCount).toArray
  var root = Array(0, 0, 0)
  var heapSize = size

  // initialize inputs
  poke(dut.io.control.heapifyDown, 0)
  poke(dut.io.control.heapifyUp, 0)
  poke(dut.io.headPort.rdData.prio.cycl, root(0))
  poke(dut.io.headPort.rdData.prio.norm, root(1))
  poke(dut.io.control.heapSize, size)

  def newMemoryState(): Array[Array[Int]] = {
    // fill memory with random numbers up to heap size, remaining memory is filled with maxed out numbers
    val state = Array.tabulate(size)(i =>
      if (i < heapSize) Array(rand.nextInt(math.pow(2, cWid).toInt), rand.nextInt(math.pow(2, nWid).toInt), i)
      else Array(Math.pow(2, cWid).toInt - 1, Math.pow(2, nWid).toInt - 1, Math.pow(2, rWid).toInt - 1)
    )
    mem = state.slice(1, state.length).sliding(chCount, chCount).toArray
    root = state(0)
    return state
  }

  def stepDut(n: Int): Unit = {
    for (i <- 0 until n) {
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
          if ((peek(dut.io.wrPort.mask) & (BigInt(1) << i)) != 0) {
            mem(pipedWrAddr)(i)(0) = peek(dut.io.wrPort.data(i).prio.cycl).toInt
            mem(pipedWrAddr)(i)(1) = peek(dut.io.wrPort.data(i).prio.norm).toInt
            mem(pipedWrAddr)(i)(2) = peek(dut.io.wrPort.data(i).id).toInt
          }
        }
      }
      poke(dut.io.headPort.rdData.prio.cycl, root(0))
      poke(dut.io.headPort.rdData.prio.norm, root(1))
      poke(dut.io.headPort.rdData.id, root(2))
      if (peek(dut.io.headPort.write).toInt == 1) {
        root(0) = peek(dut.io.headPort.wrData.prio.cycl).toInt
        root(1) = peek(dut.io.headPort.wrData.prio.norm).toInt
        root(2) = peek(dut.io.headPort.wrData.id).toInt
      }

      if (debugLvl >= 2) {
        println(
          s"\nstate: ${states(peek(dut.io.state).toInt)}\n" +
            s"ReadPort: ${peek(dut.io.rdPort.address)} | ${mem.apply(pipedRdAddr).map(_.mkString(":")).mkString(",")}\n" +
            s"WritePort: ${peek(dut.io.wrPort.address)} | ${peek(dut.io.wrPort.data).sliding(3, 3).map(_.mkString(":")).mkString(",")} | ${peek(dut.io.wrPort.write)}\n" +
            getMem()
        )
      }

      // simulate synchronous memory
      pipedRdAddr = peek(dut.io.rdPort.address).toInt
      pipedWrAddr = peek(dut.io.wrPort.address).toInt

      step(1)
      stepCounter += 1
      totalSteps += 1
    }
  }

  def stepUntilDone(max: Int = Int.MaxValue): Unit = {
    var iterations = 0
    while (iterations < max && (peek(dut.io.control.done) == 0 || iterations < 1)) {
      stepDut(1)
      iterations += 1
    }
  }

  def setHeapSize(size: Int): Unit = {
    heapSize = size
    poke(dut.io.control.heapSize, heapSize)
  }

  def heapifyUp(startIdx: Int): Unit = {
    poke(dut.io.control.heapifyUp, 1)
    poke(dut.io.control.idx, startIdx)
    stepCounter = 0
    stepUntilDone()
    poke(dut.io.control.heapifyUp, 0)
    if (debugLvl > 0) println(s"heapify up took $stepCounter cycles")
  }

  def heapifyDown(startIdx: Int): Unit = {
    poke(dut.io.control.heapifyDown, 1)
    poke(dut.io.control.idx, startIdx)
    stepCounter = 0
    stepUntilDone()
    poke(dut.io.control.heapifyUp, 0)
    if (debugLvl > 0) println(s"heapify up took $stepCounter cycles")
  }

  def getMem(): String = {
    return s"${root.mkString(":")} | ${mem.map(_.map(_.mkString(":")).mkString(", ")).mkString(" | ")}"
  }

  def compareWithModel(arr: Array[Array[Int]]): Unit = {
    expect(arr(0).deep == root.deep, "")
    expect(arr.slice(1, arr.length).deep == mem.flatten.deep, "")
  }
}
