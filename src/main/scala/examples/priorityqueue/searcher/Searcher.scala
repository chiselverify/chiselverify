package examples.priorityqueue.searcher

import chisel3._
import chisel3.util._
import examples.priorityqueue.{HeapAccessIO, QueueItem, SharedParameters}

class Searcher[T <: Data](treeSize: Int, treeOrder: Int, typeGen: => T) extends MultiIOModule {

  implicit val params = new SharedParameters(treeSize,treeOrder,typeGen,null)

  val io = IO(new Bundle {
    val command = Flipped(Decoupled(new CommandBundle))
    val response = Valid(new ResponseBundle)
    val heapAccess = new HeapAccessIO
    val error = Output(Bool())
  })

  val stateReg = RegInit(State.Idle)
  val searchedIdReg = RegInit(0.U(params.referenceWidth.W))
  val indexReg = RegInit(1.U(params.indexWidth.W)) // TODO: is the 1 initial index used everywhere?
  val idVecReg = RegInit(VecInit(Seq.fill(params.order)(0.U(params.indexWidth.W))))
  val errorReg = RegInit(0.B)
  val foundIdVecReg = RegInit(VecInit(Seq.fill(params.order)(0.B)))
  val indicesReg = RegInit(VecInit(Seq.fill(params.order)(0.U(params.indexWidth.W))))

  io.command.ready := 0.B

  io.heapAccess.read.request.valid             := 0.B
  io.heapAccess.read.request.bits.index        := indexReg
  io.heapAccess.read.request.bits.withSiblings := 1.B

  io.heapAccess.write.request.valid            := 0.B
  io.heapAccess.write.request.bits.item        := 0.U.asTypeOf(new QueueItem)
  io.heapAccess.write.request.bits.index       := 0.U

  io.response.valid := 1.B
  io.response.bits.index := indexReg

  io.error := errorReg

  switch(stateReg) {
    is(State.Idle) {
      io.command.ready := 1.B
      when(io.command.valid) {
        stateReg := State.Searching
        indexReg := 1.U
      } otherwise {
        stateReg := State.Idle
      }
    }
    is(State.Searching) {

      io.response.valid := 0.B
      stateReg := State.Searching

      // Stage 1: generate read requests
      io.heapAccess.read.request.valid := 1.B
      indexReg := indexReg + params.order.U

      // Stage 2: receive id vector
      when(io.heapAccess.read.response.valid) {
        idVecReg := io.heapAccess.read.response.bits.items.map(_.id)
      }

      // Stage 3: calculate indices and hits
      when(RegNext(io.heapAccess.read.response.valid)) {
        foundIdVecReg := VecInit(idVecReg.map(id => id === searchedIdReg))
        // TODO: indexReg contains new index, we need to save the index connected to the ids
        indicesReg := VecInit(Seq.tabulate(params.order)(i => indexReg + i.U))
      }

      // Stage 4: check for a hit and range exceeding
      when(RegNext(RegNext(io.heapAccess.read.response.valid))) {
        when(foundIdVecReg.reduceTree((l, r) => l || r)) {
          stateReg := State.Idle
          indexReg := MuxCase(indicesReg(0),foundIdVecReg.zip(indicesReg))
        } .elsewhen(indicesReg.last > params.size.U) {
          stateReg := State.Idle
          errorReg := 1.B
        }
      }

    }
  }

}
