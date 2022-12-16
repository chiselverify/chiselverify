package examples.heappriorityqueue.modules

import chisel3._
import chisel3.util._

import examples.heappriorityqueue.Interfaces.{TaggedEvent, rdPort, wrPort}
import examples.heappriorityqueue.PriorityQueueParameters

/**
  * Component implementing the algorithm to create a min-heap. Works itself either upwards or downwards
  * through the heap in memory from a given start index and swaps elements to
  * satisfy the min-heap condition. The start index is always the parent index.
  *
  * During operation either of the control signals heapifyUp or heapifyDown need to be held high
  * until done is asserted.
  *
  * When done is asserted, the component also signalizes whether a swap has taken place
  */
class Heapifier(implicit parameters: PriorityQueueParameters) extends Module {
  import parameters._
  val io = IO(new Bundle {
    // control port
    val control = new Bundle {
      val heapifyUp = Input(Bool()) // initialize heapify up
      val heapifyDown = Input(Bool()) // initialize heapify down
      val done = Output(Bool()) // component has reached terminal conditions
      val swapped = Output(Bool()) // at least one swap has taken place
      val idx = Input(UInt(log2Ceil(size).W)) // the starting index in the heap (is sampled and thus only needs to be valid when starting)
      val heapSize = Input(UInt(log2Ceil(size + 1).W)) // the current size of the heap
    }

    // ram ports
    val rdPort = new rdPort(log2Ceil(size / order), Vec(order, new TaggedEvent))
    val wrPort = new wrPort(log2Ceil(size / order), order, Vec(order, new TaggedEvent))

    // port to the cached head element stored in a register
    val headPort = new Bundle {
      val rdData = Input(new TaggedEvent)
      val wrData = Output(new TaggedEvent)
      val write = Output(Bool())
    }

    // state output for debug purposes
    val state = Output(UInt())
  })

  val minFinder = Module(new MinFinder(order + 1)) // module to find the minimum priority among the parent and children

  // state elements
  val idle :: preUp1 :: preDown1 :: preUp2 :: preDown2 :: readUp :: readDown :: wbUp1 :: wbDown1 :: wbUp2 :: wbDown2 :: Nil = Enum(11)
  val stateReg = RegInit(idle) // state register
  val indexReg = RegInit(0.U(log2Ceil(size).W)) // register holding the index of the current parent
  val swappedReg = RegInit(false.B) // register holding a flag showing whether a swap has occurred
  val parentReg = RegInit(VecInit(Seq.fill(order)(0.U.asTypeOf(new TaggedEvent)))) // register holding the content of the RAM cell containing the parent
  val childrenReg = RegInit(VecInit(Seq.fill(order)(0.U.asTypeOf(new TaggedEvent)))) // register holding the content of the RAM cell of the children

  // ram address generation
  val addressIndex = Wire(UInt(log2Ceil(size).W)) // wire that address generation is based on. Is set to indexReg except of the last write back stage, where the next address needs to be generated
  addressIndex := indexReg
  val indexParent = addressIndex
  val ramAddressChildren = addressIndex // the RAM addres of the children equals the index of the parent
  val ramAddressParent = ((addressIndex - 1.U) >> log2Ceil(order)).asUInt() // the RAM address of the parent is calculated by (index-1)/childrenCount

  // parent selection
  val parentOffset = Mux(indexReg === 0.U, 0.U, indexReg(log2Ceil(order), 0) - 1.U(log2Ceil(size).W)) // the offset of the parent within its RAM cell
  val parent = parentReg(parentOffset) // the actual parent selected from the parent register

  // hook up the minFinder
  minFinder.io.values(0) := parent
  for (i <- 0 until order) {
    minFinder.io.values(i + 1) := childrenReg(i)
  }

  val nextIndexUp = ((indexReg - 1.U) >> log2Ceil(order)).asUInt() // index of next parent is given by (index-1)/childrenCount
  val nextIndexDown = (indexReg << log2Ceil(order)).asUInt() + RegNext(minFinder.io.index) // index of next parent is given by (index * childrenCount) + selected child
  val swapRequired = minFinder.io.index =/= 0.U // a swap is only required when the parent does not have the highest priority

  // default assignments
  io.control.done := false.B
  io.control.swapped := swappedReg
  io.rdPort.address := 0.U
  io.wrPort.address := 0.U
  io.wrPort.data := childrenReg
  io.wrPort.write := false.B
  io.wrPort.mask := VecInit(Seq.fill(order)(true.B)).asUInt
  io.headPort.write := false.B
  io.headPort.wrData := parentReg(0)
  io.state := stateReg

  // state machine flow
  switch(stateReg) {
    is(idle) { // in idle we wait for a control signal, update out index register, and hold the swapped flag low
      io.control.done := true.B
      indexReg := io.control.idx
      when(io.control.heapifyUp) {
        swappedReg := false.B
        stateReg := preUp1
      }.elsewhen(io.control.heapifyDown) {
        swappedReg := false.B
        stateReg := preDown1
      }.otherwise {
        stateReg := idle
      }
    }
    is(preUp1) {
      stateReg := preUp2
    }
    is(preUp2) {
      stateReg := readUp
    }
    is(readUp) {
      stateReg := wbUp1
    }
    is(wbUp1) {
      stateReg := wbUp2
      when(!swapRequired) { // when no swap is required we go into idle state
        //io.control.done := true.B
        stateReg := idle
      }.otherwise { // we have swapped
        swappedReg := true.B
      }
    }
    is(wbUp2) { // update the index register and apply new index to address generation
      stateReg := readUp
      indexReg := nextIndexUp
      addressIndex := nextIndexUp
      when(indexReg === 0.U) { // we have reached the root and can go to idle
        stateReg := idle
      }
    }
    is(preDown1) {
      stateReg := preDown2
    }
    is(preDown2) {
      stateReg := readDown
    }
    is(readDown) {
      stateReg := wbDown1
    }
    is(wbDown1) {
      stateReg := wbDown2
      when(!swapRequired) { // when no swap is required we go into idle state
        stateReg := idle
      }.otherwise { // we have swapped
        swappedReg := true.B
      }
    }
    is(wbDown2) { // update the index register and apply new index to address generation
      stateReg := readDown
      indexReg := nextIndexDown
      addressIndex := nextIndexDown
      when((nextIndexDown << log2Ceil(order)).asUInt >= io.control.heapSize) { // we have reached a childless index and can go to idle
        stateReg := idle
      }
    }
  }

  // data and bus control
  switch(stateReg) {
    /////////////////////////////// up control
    is(preUp1) { // apply childrens RAM address to read port
      io.rdPort.address := ramAddressChildren
    }
    is(preUp2) { // apply parents RAM address to read port and save children
      io.rdPort.address := ramAddressParent
      childrenReg := io.rdPort.data
    }
    is(readUp) { // apply childrens RAM address to write port
      io.wrPort.address := ramAddressChildren
      when(indexReg === 0.U) { // if parent is head -> use head port
        parentReg := parentReg
        parentReg(0.U) := io.headPort.rdData
      }.otherwise { // if not read from RAM
        parentReg := io.rdPort.data
      }
    }
    is(wbUp1) { // write back the updated children RAM cell if a swap is required and update the parent register
      io.wrPort.address := ramAddressParent
      when(swapRequired) {
        io.wrPort.data := childrenReg
        io.wrPort.data(minFinder.io.index - 1.U) := parent
        io.wrPort.write := true.B
      }
    }
    is(wbUp2) { // write back the parent register and transfer the parent RAM cell to the children register
      io.rdPort.address := ramAddressParent
      childrenReg := parentReg
      childrenReg(parentOffset) := minFinder.io.res
      when(swapRequired) {
        when(indexReg === 0.U) { // write via head port if parent is head
          io.headPort.wrData := minFinder.io.res
          io.headPort.write := true.B
        }.otherwise { // else use the RAM port
          io.wrPort.data := parentReg
          io.wrPort.data(parentOffset) := minFinder.io.res
          io.wrPort.write := true.B
        }
      }
    }
    ////////////////////////////// down control
    is(preDown1) { // apply parents RAM address to read port
      io.rdPort.address := ramAddressParent
    }
    is(preDown2) { // apply childrens RAM address to read port and save parent
      io.rdPort.address := ramAddressChildren
      when(indexReg === 0.U) { // if parent is head -> use head port
        parentReg := parentReg
        parentReg(0.U) := io.headPort.rdData
      }.otherwise { // if not read from RAM
        parentReg := io.rdPort.data
      }
    }
    is(readDown) { // apply parents RAM address to write port and save children
      io.wrPort.address := ramAddressParent
      childrenReg := io.rdPort.data
    }
    is(wbDown1) { // write back the updated parent RAM cell if a swap is required and update the children register
      io.wrPort.address := ramAddressChildren
      when(swapRequired) {
        when(indexReg === 0.U) {
          io.headPort.wrData := minFinder.io.res
          io.headPort.write := true.B
        }.otherwise {
          io.wrPort.data := parentReg
          io.wrPort.data(parentOffset) := minFinder.io.res
          io.wrPort.write := true.B
        }
      }
    }
    is(wbDown2) { // write back the children register and transfer the children RAM cell to the parent register
      io.rdPort.address := ramAddressChildren
      parentReg := childrenReg
      parentReg(minFinder.io.index - 1.U) := parent
      when(swapRequired) {
        io.wrPort.data := childrenReg
        io.wrPort.data(minFinder.io.index - 1.U) := parent
        io.wrPort.write := true.B
      }
    }
  }
}
