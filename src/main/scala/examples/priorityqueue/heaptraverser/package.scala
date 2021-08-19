package examples.priorityqueue

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.{Decoupled, DecoupledIO, Valid}
import examples.priorityqueue.heaptraverser.TraversalDirection


package object heaptraverser {

  object State extends ChiselEnum {
    val Idle, PreFetchParent, PreFetchChildren, FetchParent, FetchChildren, FindMinUp, FindMinDown, EpilogueUp, EpilogueDown = Value
  }

  object TraversalDirection extends ChiselEnum {
    val Up, Down = Value
  }

  class CommandBundle[T <: Data](implicit params: SharedParameters[T]) extends DecoupledIO(new Bundle {
    val dir = TraversalDirection()
    val index = UInt(params.indexWidth.W)
  })

  object IndexOp extends ChiselEnum {
    val KeepValue, AcceptInput, SetToExchangedChild, SetToParent = Value
  }

  class DataPathChannelIO extends Bundle {
    val indexOp = IndexOp()
  }

  class HeapTraverserDataPathIO[T <: Data](implicit params: SharedParameters[T]) extends Bundle {
    val index = UInt(params.indexWidth.W)
    val read = new Bundle {
      val index = Output(UInt(params.indexWidth.W))
      val items = Input(Vec(params.order,new QueueItem))
    }
    val write = new Bundle {
      val index = Output(UInt(params.indexWidth.W))
      val item = Output(new QueueItem)
    }

  }


}
