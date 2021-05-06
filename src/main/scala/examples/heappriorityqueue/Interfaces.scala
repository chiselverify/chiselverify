package examples.heappriorityqueue

import chisel3._
import chisel3.util._

/**
  * contains relevant bundle types and port types for the heap-based priority queue
  */

object Interfaces {

  class Event(implicit parameters: PriorityQueueParameters) extends Bundle {
    import parameters._
    val cycle = UInt(cycleWidth.W)
    val superCycle = UInt(superCycleWidth.W)

    def <(that: Event): Bool = superCycle < that.superCycle || (superCycle === that.superCycle && cycle < that.cycle)
    def >(that: Event): Bool = superCycle > that.superCycle || (superCycle === that.superCycle) && cycle > that.cycle
    def ===(that: Event): Bool = superCycle === that.superCycle && cycle === that.cycle

    override def cloneType = new Event().asInstanceOf[this.type]
  }

  class TaggedEvent(implicit parameters: PriorityQueueParameters) extends Bundle {
    import parameters._
    val event = new Event
    val id = UInt(referenceIdWidth.W)

    override def cloneType = new TaggedEvent().asInstanceOf[this.type]
  }

  class rdPort[T <: Data](addrWid: Int, dType: T) extends Bundle { // as seen from reader side
    val address = Output(UInt(addrWid.W))
    val data = Input(dType)

    override def cloneType = (new rdPort[T](addrWid, dType)).asInstanceOf[this.type]
  }

  class wrPort[T <: Data](addrWid: Int, maskWid: Int, dType: T) extends Bundle { // as seen from writer side
    val address = Output(UInt(addrWid.W))
    val mask = Output(UInt(maskWid.W))
    val data = Output(dType)
    val write = Output(Bool())

    override def cloneType = (new wrPort[T](addrWid, maskWid, dType)).asInstanceOf[this.type]
  }

  class searchPort(implicit parameters: PriorityQueueParameters) extends Bundle { // as seen from requester side
    import parameters._
    val refID = Output(UInt(referenceIdWidth.W))
    val heapSize = Output(UInt(log2Ceil(size + 1).W))
    val res = Input(UInt(log2Ceil(size).W))
    val search = Output(Bool())
    val error = Input(Bool())
    val done = Input(Bool())

    override def cloneType = new searchPort().asInstanceOf[this.type]
  }

}
