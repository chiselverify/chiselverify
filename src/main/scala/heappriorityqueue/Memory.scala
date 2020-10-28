package heappriorityqueue

import chisel3._
import chisel3.util._
import lib._


class Memory(size: Int, chCount: Int, cWid: Int, nWid: Int, rWid: Int) extends MultiIOModule {
  val rd = IO(Flipped(new rdPort(log2Ceil(size/chCount), Vec(chCount, new PriorityAndID(cWid,nWid,rWid)))))
  val wr = IO(Flipped(new wrPort(log2Ceil(size/chCount), chCount, Vec(chCount, new PriorityAndID(cWid,nWid,rWid)))))
  val srch = IO(Flipped(new searchPort(size,rWid)))
  val pointer = IO(Output(UInt()))
  val state = IO(Output(UInt()))
  val res = IO(Output(UInt()))

  // create memory
  val mem = SyncReadMem(size/chCount, Vec(chCount, new PriorityAndID(cWid,nWid,rWid)))

  // read port
  val rdAddr = Wire(UInt(log2Ceil(size/chCount).W))
  val rdPort = mem.read(rdAddr)
  val rdData = Wire(Vec(chCount, new PriorityAndID(cWid,nWid,rWid)))
  rdData := rdPort
  rdAddr := rd.address
  rd.data := rdData

  // write port
  val wrData = Wire(Vec(chCount,new PriorityAndID(cWid,nWid,rWid)))
  val wrAddr = Wire(UInt(log2Ceil(size/chCount).W))
  val wrMask = Wire(Vec(chCount, Bool()))
  val write = Wire(Bool())
  wrData := wr.data
  wrAddr := wr.address
  wrMask := wr.mask.asBools
  write := wr.write
  when(write){
    mem.write(wrAddr,wrData,wrMask)
  }

  val lastAddr = Mux(srch.heapSize === 0.U, 0.U, ((srch.heapSize - 1.U) >> log2Ceil(chCount)).asUInt)

  // search state machine
  val idle :: search :: setup :: Nil = Enum(3)
  val stateReg = RegInit(setup)
  val pointerReg = RegInit(0.U(log2Ceil(size).W))
  val resVec = Wire(Vec(chCount, Bool()))
  val errorFlag = RegInit(false.B)

  val resetCell = WireDefault(VecInit(Seq.fill(chCount)(VecInit(Seq.fill(cWid+nWid+rWid)(true.B)).asUInt.asTypeOf(new PriorityAndID(cWid,nWid,rWid)))))

  resVec := rdData.map(_.id === srch.refID)

  srch.done := true.B
  srch.res := pointerReg
  srch.error := errorFlag
  pointer := pointerReg
  state := stateReg
  res := OHToUInt(PriorityEncoderOH(resVec))

  switch(stateReg){
    is(setup){
      srch.done := false.B
      stateReg := setup
      pointerReg := pointerReg + 1.U

      wrAddr := pointerReg
      wrData := resetCell
      wrMask := VecInit(Seq.fill(chCount)(true.B))
      write := true.B

      when(pointerReg === ((size/chCount)+1).U){
        stateReg := idle
      }
    }
    is(idle){
      stateReg := idle
      when(srch.search){
        pointerReg := 0.U
        rdAddr := 0.U
        errorFlag := false.B
        stateReg := search
      }
    }
    is(search){
      errorFlag := false.B
      srch.done := false.B
      rdAddr := pointerReg + 1.U
      pointerReg := pointerReg + 1.U
      stateReg := search
      when(resVec.asUInt =/= 0.U || !srch.search){ //beaware: deasserting "search" aborts search
        stateReg := idle
        pointerReg := (pointerReg << log2Ceil(chCount)).asUInt + OHToUInt(PriorityEncoderOH(resVec))
      }.elsewhen(pointerReg >= lastAddr){
        errorFlag := true.B
        stateReg := idle
      }
    }
  }
}
