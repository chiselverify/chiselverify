package examples.heappriorityqueue.modules

import chisel3._
import chisel3.util._

import examples.heappriorityqueue.Interfaces._
import examples.heappriorityqueue.PriorityQueueParameters

/**
  * Component implementing the control for the priority queue
  */
class QueueControl(implicit parameters: PriorityQueueParameters) extends Module {
  import parameters._
  require(isPow2(order), "The number of children per node needs to be a power of 2!")
  
  val io = IO(new Bundle {
    val head = new Bundle {
      val valid = Output(Bool())
      val none = Output(Bool())
      val prio = Output(new Event)
      val refID = Output(UInt(referenceIdWidth.W))
    }

    val cmd = new Bundle {
      // inputs
      val valid = Input(Bool())
      val op = Input(Bool()) // 0=Remove, 1=Insert
      val prio = Input(new Event)
      val refID = Input(UInt(referenceIdWidth.W))
      // outputs
      val done = Output(Bool())
      val result = Output(Bool()) // 0=Success, 1=Failure
      val rm_prio = Output(new Event)
    }

    // ram ports to synchronous memory
    val rdPort = new rdPort(log2Ceil(size / order), Vec(order, new TaggedEvent))
    val wrPort = new wrPort(log2Ceil(size / order), order, Vec(order, new TaggedEvent))

    // search port to a component with the ability to find the positions of reference ID's in memory
    val srch = new searchPort

    // output of the current state for debug purposes
    val state = Output(UInt())
  })

  // modules
  val heapifier = Module(new Heapifier)

  // state elements
  val idle :: headInsertion :: normalInsertion :: initSearch :: waitForSearch :: resetCell :: lastRemoval :: headRemoval :: tailRemoval :: removal :: waitForHeapifyUp :: waitForHeapifyDown :: Nil = Enum(12)
  val stateReg = RegInit(idle)
  val heapSizeReg = RegInit(0.U(log2Ceil(size + 1).W))
  val headReg = RegInit(VecInit(Seq.fill(cycleWidth + superCycleWidth + referenceIdWidth)(1.U)).asTypeOf(new TaggedEvent))
  val tempReg = RegInit(VecInit(Seq.fill(order)(0.U.asTypeOf(new TaggedEvent))))
  val removalIndex = RegInit(0.U(log2Ceil(size).W))
  val removedPrio = RegInit(0.U.asTypeOf(new Event))

  // flags
  val headValid = RegInit(true.B)
  val errorReg = RegInit(false.B)

  // ram write address and offset
  val wrIndex = WireDefault(0.U(log2Ceil(size).W))
  val wrIndexToRam = ((wrIndex - 1.U) >> log2Ceil(order)).asUInt
  val wrIndexOffset = Mux(wrIndex === 0.U, 0.U, wrIndex(log2Ceil(order), 0) - 1.U(log2Ceil(size).W))

  // ram read address and offset
  val rdIndex = WireDefault(0.U(log2Ceil(size).W))
  val rdIndexToRam = ((rdIndex - 1.U) >> log2Ceil(order)).asUInt
  val rdIndexOffset = Mux(rdIndex === 0.U, 0.U, rdIndex(log2Ceil(order), 0) - 1.U(log2Ceil(size).W))

  val resetVal = VecInit(Seq.fill(cycleWidth + superCycleWidth + referenceIdWidth)(true.B)).asUInt.asTypeOf(new TaggedEvent)

  // heapSize controlling
  val incHeapsize = heapSizeReg + 1.U
  val decHeapsize = heapSizeReg - 1.U

  // connect heapifier
  heapifier.io.control.heapifyUp := false.B
  heapifier.io.control.heapifyDown := false.B
  heapifier.io.control.idx := heapSizeReg
  heapifier.io.control.heapSize := heapSizeReg
  heapifier.io.headPort.rdData := headReg
  when(heapifier.io.headPort.write) {
    headReg := heapifier.io.headPort.wrData
  }
  // default ram connections
  io.rdPort.address := heapifier.io.rdPort.address
  heapifier.io.rdPort.data := io.rdPort.data
  io.wrPort.address := heapifier.io.wrPort.address
  io.wrPort.data := heapifier.io.wrPort.data
  io.wrPort.write := heapifier.io.wrPort.write
  io.wrPort.mask := heapifier.io.wrPort.mask

  // default assignments
  io.head.prio := headReg.event
  io.head.none := heapSizeReg === 0.U
  io.head.valid := headValid
  io.head.refID := headReg.id
  io.cmd.done := false.B
  io.cmd.result := errorReg
  io.cmd.rm_prio := removedPrio
  io.srch.refID := io.cmd.refID
  io.srch.search := false.B
  io.srch.heapSize := heapSizeReg
  io.state := stateReg

  //////////////////////////////////////////////////State Machine///////////////////////////////////////////////////////
  switch(stateReg) {
    is(idle) {

      io.cmd.done := true.B

      wrIndex := heapSizeReg // prepare write port for insertion
      rdIndex := decHeapsize // prepare read port for removal
      io.wrPort.address := wrIndexToRam
      io.rdPort.address := rdIndexToRam

      when(heapSizeReg =/= 0.U) { // reset valid flag if queue not empty
        headValid := true.B
      }

      when(io.cmd.valid) {
        errorReg := false.B
        when(io.cmd.op) { // insertion
          headValid := false.B
          stateReg := normalInsertion
          when(heapSizeReg === size.U) { // queue is full
            errorReg := true.B
            stateReg := idle
          }.elsewhen(heapSizeReg === 0.U) {
            stateReg := headInsertion
          }
        }.otherwise { // removal
          headValid := false.B
          when(heapSizeReg =/= 0.U) {
            when(heapSizeReg === 1.U && headReg.id =/= io.cmd.refID) { //catch non matching reference ID on last removal
              errorReg := true.B
              stateReg := idle
              removedPrio := 0.U.asTypeOf(new Event)
            }.elsewhen(heapSizeReg === 1.U && headReg.id === io.cmd.refID) {
              stateReg := lastRemoval
            }.otherwise {
              stateReg := initSearch
            }
          }.otherwise {
            removedPrio := 0.U.asTypeOf(new Event)
            stateReg := idle
            errorReg := true.B
          }
        }
      }
    }
    is(headInsertion) { // insertion into empty queue
      headReg.event := io.cmd.prio
      headReg.id := io.cmd.refID
      heapSizeReg := incHeapsize

      stateReg := idle
    }
    is(normalInsertion) { // insertion into already filled queue
      // write new priority
      wrIndex := heapSizeReg
      io.wrPort.data(wrIndexOffset).event := io.cmd.prio
      io.wrPort.data(wrIndexOffset).id := io.cmd.refID
      io.wrPort.mask := UIntToOH(wrIndexOffset, order)
      io.wrPort.write := true.B

      // increase heap size
      heapSizeReg := incHeapsize

      // initiate heapify up
      heapifier.io.control.idx := Mux(heapSizeReg < (order + 1).U, 0.U, ((heapSizeReg - 1.U) >> log2Ceil(order)).asUInt)
      heapifier.io.control.heapifyUp := true.B
      stateReg := waitForHeapifyUp
    }
    is(initSearch) { // start search and prepare for following steps
      io.srch.search := true.B

      // decrease size of heap
      heapSizeReg := decHeapsize

      // read last queue element from ram into temporary register
      rdIndex := decHeapsize
      tempReg := io.rdPort.data

      // prepare ram write port
      wrIndex := decHeapsize
      io.wrPort.address := wrIndexToRam

      stateReg := waitForSearch
      headValid := true.B // head is valid as long as we are not removing the head element
      when(headReg.id === io.cmd.refID) { // compare head and tail with desired refID
        stateReg := headRemoval
        headValid := false.B
        removedPrio := headReg.event
      }.elsewhen(io.rdPort.data(rdIndexOffset).id === io.cmd.refID) {
        stateReg := tailRemoval
        removedPrio := io.rdPort.data(rdIndexOffset).event
      }
    }
    is(waitForSearch) { // wait for memory to look up the index corresponding to the reference ID

      // prepare ram write port
      wrIndex := heapSizeReg
      io.wrPort.address := wrIndexToRam

      // prepare ram read port to read the element to be removed
      rdIndex := io.srch.res
      io.rdPort.address := rdIndexToRam

      io.srch.search := true.B

      stateReg := waitForSearch
      when(io.srch.error) { // detect a none existing reference ID
        removedPrio := 0.U.asTypeOf(new Event)
        errorReg := true.B
        stateReg := idle
        heapSizeReg := incHeapsize // reset heapsize
        io.srch.search := false.B
      }.elsewhen(io.srch.done) {
        removalIndex := io.srch.res // save index of removal
        stateReg := resetCell
        io.srch.search := false.B
      }
    }
    is(resetCell) {
      // reset the cleared queue position to max values
      rdIndex := heapSizeReg
      io.wrPort.data(rdIndexOffset) := resetVal
      io.wrPort.mask := UIntToOH(rdIndexOffset, order)
      io.wrPort.write := true.B

      // prepare write port
      wrIndex := removalIndex
      io.wrPort.address := wrIndexToRam

      // store the element to be removed
      removedPrio := io.rdPort.data(wrIndexOffset).event

      stateReg := removal
    }
    is(lastRemoval) { // last element is removed from queue
      removedPrio := headReg.event
      heapSizeReg := decHeapsize
      stateReg := idle
    }
    is(headRemoval) { // remove head from queue
      rdIndex := heapSizeReg
      wrIndex := removalIndex

      headReg := tempReg(rdIndexOffset)

      io.wrPort.data(rdIndexOffset) := resetVal
      io.wrPort.mask := UIntToOH(rdIndexOffset, order)
      io.wrPort.write := true.B

      // initiate heapify down
      heapifier.io.control.heapifyDown := true.B
      heapifier.io.control.idx := 0.U
      stateReg := waitForHeapifyDown
    }
    is(tailRemoval) {
      // set tail to max values
      wrIndex := heapSizeReg
      io.wrPort.data(wrIndexOffset) := resetVal
      io.wrPort.mask := UIntToOH(wrIndexOffset, order)
      io.wrPort.write := true.B

      stateReg := idle
    }
    is(removal) {
      // overwrite the element to be deleted with cached tail
      wrIndex := removalIndex
      rdIndex := heapSizeReg
      io.wrPort.data(wrIndexOffset) := tempReg(rdIndexOffset)
      io.wrPort.mask := UIntToOH(wrIndexOffset, order)
      io.wrPort.write := true.B

      // initiate heapify up
      heapifier.io.control.heapifyUp := true.B
      heapifier.io.control.idx := Mux(removalIndex < (order + 1).U, 0.U, ((removalIndex - 1.U) >> log2Ceil(order)).asUInt)
      stateReg := waitForHeapifyUp
    }
    is(waitForHeapifyUp) { // wait for the heapifier to complete one up pass
      stateReg := waitForHeapifyUp
      when(heapifier.io.control.done) {
        stateReg := idle
        when(io.cmd.op === 0.U && !heapifier.io.control.swapped && ((removalIndex << log2Ceil(order)).asUInt + 1.U) < size.U) { // when no swap occurred during removal -> heapify down
          heapifier.io.control.idx := removalIndex
          heapifier.io.control.heapifyDown := true.B
          stateReg := waitForHeapifyDown
        }
      }.otherwise {
        heapifier.io.control.heapifyUp := true.B
      }
    }
    is(waitForHeapifyDown) { // wait for the heapifier to complete one down pass
      stateReg := waitForHeapifyDown
      when(heapifier.io.control.done) {
        stateReg := idle
      }.otherwise {
        heapifier.io.control.heapifyDown := true.B
      }
    }
  }
}
