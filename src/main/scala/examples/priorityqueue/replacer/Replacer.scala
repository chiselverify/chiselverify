package examples.priorityqueue.replacer

import chisel3._
import chisel3.util._
import examples.priorityqueue.{HeapAccessIO, QueueItem, SharedParameters}

class Replacer[T <: Data](treeSize: Int, treeOrder: Int, typeGen: => T) extends MultiIOModule {

  implicit val params = new SharedParameters(treeSize,treeOrder,typeGen,null)

  val io = IO(new Bundle {
    val command    = Flipped(Decoupled(new CommandBundle))
    val heapAccess = new HeapAccessIO
    val error      = Output(Bool())
  })

  val stateReg            = RegInit(State.Idle)
  val sourceIndexReg      = RegInit(0.U(params.indexWidth.W))
  val destinationIndexReg = RegInit(0.U(params.indexWidth.W))
  val itemReg             = RegInit(0.U.asTypeOf(new QueueItem))
  val errorReg            = RegInit(0.B)

  io.command.ready := 0.B

  io.heapAccess.read.request.valid             := 0.B
  io.heapAccess.read.request.bits.index        := sourceIndexReg
  io.heapAccess.read.request.bits.withSiblings := 0.B

  io.heapAccess.write.request.valid            := 0.B
  io.heapAccess.write.request.bits.item        := itemReg
  io.heapAccess.write.request.bits.index       := destinationIndexReg

  io.error := errorReg

  switch(stateReg) {
    is(State.Idle) {
      io.command.ready := 1.B
      when(io.command.valid) {
        when(io.command.bits.sourceIndex < params.size.U && io.command.bits.destinationIndex < params.size.U) {
          sourceIndexReg := io.command.bits.sourceIndex
          destinationIndexReg := io.command.bits.destinationIndex
          errorReg := 0.B
          stateReg := State.IssueRead
        } otherwise {
          stateReg := State.Idle
          errorReg := 1.B
        }
      } otherwise {
        stateReg := State.Idle
      }
    }
    is(State.IssueRead) {
      stateReg := State.ReceiveRead
      io.heapAccess.read.request.valid := 1.B
    }
    is(State.ReceiveRead) {
      when(io.heapAccess.read.response.valid) {
        stateReg := State.IssueWrite
        itemReg := io.heapAccess.read.response.bits.items(0)
      } otherwise {
        stateReg := State.ReceiveRead
      }
    }
    is(State.IssueWrite) {
      stateReg := State.Idle
      io.heapAccess.write.request.valid := 1.B
    }
  }

}
