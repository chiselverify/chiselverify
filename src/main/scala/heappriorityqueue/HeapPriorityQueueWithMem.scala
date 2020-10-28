package heappriorityqueue

import chisel3._
import chisel3.util.log2Ceil
import heappriorityqueue.lib._

class HeapPriorityQueueWithMem(size : Int, chCount : Int, cWid : Int, nWid : Int, rWid : Int) extends Module{
  val io = IO(new Bundle{
    // Interface for signaling head element to user.
    // I.e. the element with the lowest priority
    val head = new Bundle{
      val valid = Output(Bool())
      val none = Output(Bool())
      val prio = Output(new Priority(cWid,nWid))
      val refID = Output(UInt(rWid.W))
    }
    // Interface for element insertion/removal
    // Timing:
    // User must maintain input asserted until done is asserted.
    // User must deassert input when done is asserted (unless a new cmd is made).
    // User must ensure that reference ID tags are unique.
    val cmd = new Bundle{
      // inputs
      val valid = Input(Bool())
      val op = Input(Bool()) // 0=Remove, 1=Insert
      val prio = Input(new Priority(cWid,nWid))
      val refID = Input(UInt(rWid.W))
      // outputs
      val done = Output(Bool())
      val result = Output(Bool()) // 0=Success, 1=Failure
      val rm_prio = Output(new Priority(cWid,nWid))
    }
    val rdPort = new Bundle{
      val address = Output(UInt(log2Ceil(size/chCount).W))
      val data = Output(Vec(chCount, new PriorityAndID(cWid,nWid,rWid)))
    }
    val wrPort = new wrPort(log2Ceil(size/chCount),chCount,Vec(chCount,new PriorityAndID(cWid,nWid,rWid)))

    val state = Output(UInt())
    val memState = Output(UInt())
    val srchError = Output(Bool())
  })
  val mem = Module(new Memory(size-1,chCount,cWid,nWid,rWid))
  val queue = Module(new HeapPriorityQueue(size,chCount,cWid,nWid,rWid))
  mem.srch <> queue.io.srch
  mem.rd <> queue.io.rdPort
  mem.wr <> queue.io.wrPort
  io.head <> queue.io.head
  io.cmd <> queue.io.cmd
  io.head.prio.cycl := queue.io.head.prio.cycl
  io.state := queue.io.state
  io.memState := mem.state
  io.rdPort.data := mem.rd.data
  io.rdPort.address := queue.io.rdPort.address
  io.wrPort <> queue.io.wrPort
  io.srchError := mem.srch.error
}
