package examples.heappriorityqueue.modules

import chisel3._
import chisel3.util._

import examples.heappriorityqueue.Interfaces.{TaggedEvent, rdPort, searchPort, wrPort}
import examples.heappriorityqueue.PriorityQueueParameters

/**
  * Abstract interface for a sequential memory
  *   - ZBT -> write address is as well supplied on clock cycle in advance of a write
  *   - CAM functionality -> when a reference ID is provided, the module should find the first occurrence in memory
  *   - should be initialized to all 1's
  */
abstract class SearchSeqMem(capacity: Int)(implicit parameters: PriorityQueueParameters) extends Module {
  import parameters._
  val rd = IO(Flipped(new rdPort(log2Ceil(size / order), Vec(order, new TaggedEvent))))
  val wr = IO(Flipped(new wrPort(log2Ceil(size / order), order, Vec(order, new TaggedEvent))))
  val srch = IO(Flipped(new searchPort))
}

/**
  * A implementation of SearchSeqMem, where the CAM-functionality is provided by a linear search through the memory
  */
class linearSearchMem(capacity: Int)(implicit parameters: PriorityQueueParameters) extends SearchSeqMem(capacity) {
  import parameters._

  // create memory
  val mem = SyncReadMem(capacity / order, Vec(order, new TaggedEvent))

  // read port
  val rdAddr = Wire(UInt(log2Ceil(size / order).W))
  val rdPort = mem.read(rdAddr)
  val rdData = Wire(Vec(order, new TaggedEvent))
  rdData := rdPort
  rdAddr := rd.address
  rd.data := rdData

  // write port
  val wrData = Wire(Vec(order, new TaggedEvent))
  val wrAddr = Wire(UInt(log2Ceil(size / order).W))
  val wrMask = Wire(Vec(order, Bool()))
  val write = Wire(Bool())
  wrData := wr.data
  wrAddr := wr.address
  wrMask := wr.mask.asBools
  write := wr.write
  when(write) {
    mem.write(RegNext(wrAddr), wrData, wrMask)
  }

  val lastAddr = Mux(srch.heapSize === 0.U, 0.U, ((srch.heapSize - 1.U) >> log2Ceil(order)).asUInt)

  // search state machine
  val idle :: search :: setup :: Nil = Enum(3)
  val stateReg = RegInit(setup)
  val pointerReg = RegInit(0.U(log2Ceil(size).W))
  val resVec = Wire(Vec(order, Bool()))
  val errorFlag = RegInit(false.B)

  val resetCell = WireDefault(VecInit(Seq.fill(order)(VecInit(Seq.fill(superCycleWidth + cycleWidth + referenceIdWidth)(true.B)).asUInt.asTypeOf(new TaggedEvent))))

  resVec := rdData.map(_.id === srch.refID)

  srch.done := true.B
  srch.res := pointerReg
  srch.error := errorFlag

  switch(stateReg) {
    is(setup) {
      srch.done := false.B
      stateReg := setup
      pointerReg := pointerReg + 1.U

      wrAddr := pointerReg
      wrData := resetCell
      wrMask := VecInit(Seq.fill(order)(true.B))
      write := true.B

      when(pointerReg === ((size / order) + 1).U) {
        stateReg := idle
      }
    }
    is(idle) {
      stateReg := idle
      when(srch.search) {
        pointerReg := 0.U
        rdAddr := 0.U
        errorFlag := false.B
        stateReg := search
      }
    }
    is(search) {
      errorFlag := false.B
      srch.done := false.B
      rdAddr := pointerReg + 1.U
      pointerReg := pointerReg + 1.U
      stateReg := search
      when(resVec.asUInt =/= 0.U || !srch.search) { // deasserting "search" aborts search
        stateReg := idle
        pointerReg := (pointerReg << log2Ceil(order)).asUInt + OHToUInt(PriorityEncoderOH(resVec)) + 1.U
      }.elsewhen(pointerReg >= lastAddr) {
        errorFlag := true.B
        stateReg := idle
      }
    }
  }
}
