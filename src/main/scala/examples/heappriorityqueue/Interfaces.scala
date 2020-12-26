package examples.heappriorityqueue

import chisel3._
import chisel3.util._

/** contains relevant bundle types and port types for the heap-based priority queue
  */

object Interfaces {

  class Priority(val cWid: Int, val nWid: Int) extends Bundle {
    val norm = UInt(nWid.W)
    val cycl = UInt(cWid.W)

    override def cloneType = new Priority(cWid, nWid).asInstanceOf[this.type]
  }

  class PriorityAndID(cWid: Int, nWid: Int, rWid: Int) extends Bundle {
    val prio = new Priority(cWid, nWid)
    val id = UInt(rWid.W)

    override def cloneType = (new PriorityAndID(cWid, nWid, rWid)).asInstanceOf[this.type]
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

  class searchPort(size: Int, rWid: Int) extends Bundle { // as seen from requester side
    val refID = Output(UInt(rWid.W))
    val heapSize = Output(UInt(log2Ceil(size + 1).W))
    val res = Input(UInt(log2Ceil(size).W))
    val search = Output(Bool())
    val error = Input(Bool())
    val done = Input(Bool())

    override def cloneType = (new searchPort(size, rWid)).asInstanceOf[this.type]
  }

}
