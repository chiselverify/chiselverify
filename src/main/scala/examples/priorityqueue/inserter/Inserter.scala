package examples.priorityqueue.inserter

import chisel3._
import chisel3.util.{Decoupled, is, switch}
import examples.priorityqueue.{HeapAccessIO, QueueItem, SharedParameters}

class Inserter[T <: Data](treeSize: Int, treeOrder: Int, typeGen: => T) extends MultiIOModule {

  implicit val params = new SharedParameters(treeSize,treeOrder,typeGen,null)

  val io = IO(new Bundle {
    val command    = Flipped(Decoupled(new CommandBundle))
    val heapAccess = new HeapAccessIO
    val error      = Output(Bool()) // TODO: should an error type be introduced?
  })

  val stateReg  = RegInit(State.Idle)
  val errorReg = RegInit(0.B)
  val itemReg   = RegInit(0.U.asTypeOf(new QueueItem))
  val indexReg  = RegInit(0.U(params.indexWidth.W))

  io.command.ready := 0.B

  io.heapAccess.read.request.valid             := 0.B
  io.heapAccess.read.request.bits.index        := 0.U
  io.heapAccess.read.request.bits.withSiblings := 0.B

  io.heapAccess.write.request.valid            := 0.B
  io.heapAccess.write.request.bits.item        := itemReg
  io.heapAccess.write.request.bits.index       := indexReg

  io.error := errorReg

  switch(stateReg) {
    is(State.Idle) {
      io.command.ready := 1.B
      when(io.command.valid) {
        when(io.command.bits.index < params.size.U) {
          itemReg := io.command.bits.item
          indexReg := io.command.bits.index
          errorReg := 0.B
          stateReg := State.IssueWrite
        } otherwise {
          stateReg := State.Idle
          errorReg := 1.B
        }
      } otherwise {
        stateReg := State.Idle
      }
    }
    is(State.IssueWrite) {
      stateReg := State.Idle
      io.heapAccess.write.request.valid := 1.B
    }
  }


}
