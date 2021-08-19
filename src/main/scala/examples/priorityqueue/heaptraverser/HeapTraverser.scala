package examples.priorityqueue.heaptraverser

import chisel3._
import chisel3.util.{is, switch}
import examples.priorityqueue.{HeapAccessIO, QueueItem, SharedParameters}

class HeapTraverser[T <: Data](treeSize: Int, treeOrder: Int, typeGen: => T)(reductionOp: (T, T) => T) extends MultiIOModule {
  /* TODO: interface?
  we need one port to control operation
   */

  implicit val params: SharedParameters[T] = new SharedParameters(treeSize, treeOrder, typeGen, reductionOp)

  val io = IO(new Bundle {
    val command = Flipped(new CommandBundle)
    val heapAccess = new HeapAccessIO
  })

  //--------------------------------------------------------------------------------------------------------------------
  // Default values
  io.command.ready := 0.B

  io.heapAccess.read.request.bits.index := 0.U
  io.heapAccess.read.request.bits.withSiblings := 1.B

  io.heapAccess.write.request.bits.index := 0.U
  io.heapAccess.write.request.bits.item := 0.U.asTypeOf(new QueueItem)


}

private class HeapTraverserFsm[T <: Data](implicit params: SharedParameters[T]) extends MultiIOModule {

  val io = IO(new Bundle {
    val command = Flipped(new CommandBundle)
    val heapAccess = new HeapAccessIO
    val channel = new DataPathChannelIO
  })

  io.command.ready := 0.B
  io.heapAccess.read.request.valid := 0.B
  io.heapAccess.read.request.bits.withSiblings := 0.B
  io.heapAccess.read.request.bits.index := DontCare
  io.heapAccess.write.request.valid := 0.B
  io.heapAccess.write.request.bits.index := DontCare
  io.heapAccess.write.request.bits.item := DontCare
  io.channel.indexOp := IndexOp.KeepValue

  val stateReg = RegInit(State.Idle)

  switch(stateReg) {

    is(State.Idle) {
      // Signal ready to host while idling
      io.command.ready := 1.B

      // React to incoming request from host
      when(io.command.valid) {
        // read in starting index of the traversal
        io.channel.indexOp := IndexOp.AcceptInput

        // fetch items according to traversal direction
        switch(io.command.bits.dir) {
          is(TraversalDirection.Up) {
            stateReg := State.PreFetchChildren
          }
          is(TraversalDirection.Down) {
            stateReg := State.PreFetchParent
          }
        }
      } otherwise {
        stateReg := State.Idle
      }
    }

  }

}


private class HeapTaverserDataPath[T <: Data](implicit params: SharedParameters[T]) extends MultiIOModule {

  val io = IO(new Bundle {
    val channel = Flipped(new DataPathChannelIO)
    val dataIO = new HeapTraverserDataPathIO
  })

  val indexReg = RegInit(1.U(params.indexWidth.W))
  val lowerItemsReg = RegInit(Vec(params.order, new QueueItem))
  val upperItemsReg = RegInit(Vec(params.order, new QueueItem))

  switch(io.channel.indexOp) {
    is(IndexOp.KeepValue) {
      indexReg := indexReg
    }
    is(IndexOp.AcceptInput) {
      indexReg := io.dataIO.index
    }
    is(IndexOp.SetToExchangedChild) {
      indexReg := (indexReg << params.order) // TODO: add minFinder offset
    }
    is(IndexOp.SetToParent) {
      indexReg := indexReg >> params.order
    }
  }

}