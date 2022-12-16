package examples.heappriorityqueue

import chisel3._
import chisel3.util._

import examples.heappriorityqueue.Interfaces._
import examples.heappriorityqueue.modules.{QueueControl, linearSearchMem}

case class PriorityQueueParameters(size: Int, order: Int, superCycleWidth: Int, cycleWidth: Int, referenceIdWidth: Int)

/**
  * Component implementing a priority queue, where the minimum value gets to the head of the queue
  *  - The sorting is based on heap sort
  *  - inserted priorities are made up of 3 fields:
  *    - cyclic priority which is most important
  *    - normal priority which decides in the case of equal cyclic priorities
  *    - a user generated reference ID given at insertion which is used to remove elements from the queue
  *  - the component needs to be provided with the following:
  *    - a synchronous memory of appropriate size
  *      - the memory has to be initialized to all 1s
  *      - thus the reference ID with all 1s is reserved for empty cells
  *    - a component which is able to search through the reference ID fields and return the index where a given reference ID is present
  *      - the queue waits until the search result is given
  *  - the head element is kept locally in a FF for symmetry and access speed reasons
  *  - the head element is furthermore presented at a dedicated port
  *
  * @param size    the maximum size of the heap
  * @param order the number of children per node in the tree. Must be power of 2
  * @param nWid    width of the normal priority value
  * @param cWid    width of the cyclic priority value
  * @param rWid    width of the reference ID tags
  */
class PriorityQueue(size: Int, order: Int, superCycleRes: Int, cyclesPerSuperCycle: Int, exposeState: Boolean = false) extends Module {
  require(isPow2(order), "The number of children per node needs to be a power of 2!")

  val superCycleWidth = superCycleRes
  val cycleWidth = log2Ceil(cyclesPerSuperCycle)
  val referenceIdWidth = log2Ceil(size+1)
  implicit val parameters = PriorityQueueParameters(size, order, superCycleWidth, cycleWidth, referenceIdWidth)

  val io = IO(new Bundle {
    // Interface for signaling head element to user.
    // I.e. the element with the lowest priority
    val head = new Bundle {
      val valid = Output(Bool())
      val none = Output(Bool())
      val prio = Output(new Event)
      val refID = Output(UInt(referenceIdWidth.W))
    }

    // Interface for element insertion/removal
    // Timing:
    // User must maintain input asserted until done is asserted.
    // User must deassert input when done is asserted (unless a new cmd is made).
    // User must ensure that reference ID tags are unique.
    val cmd = new Bundle {
      // inputs
      val valid = Input(Bool())
      val op = Input(Bool()) // 0=Remove, 1=Insert
      val prio = Input(new Event)
      val refID = Input(UInt(referenceIdWidth.W))
      // outputs
      val done = Output(Bool())
      val result = Output(Bool()) // 0=Success, 1=Failure
      val rm_prio = Output(new Event)
    }

    val state = if (exposeState) Some(Output(UInt())) else None
  })

  val mem = Module(new linearSearchMem(size - 1))
  val queue = Module(new QueueControl)

  mem.srch <> queue.io.srch
  mem.rd <> queue.io.rdPort
  mem.wr <> queue.io.wrPort
  io.head <> queue.io.head
  io.cmd <> queue.io.cmd

  io.cmd.done := mem.srch.done && queue.io.cmd.done

  if (exposeState) io.state.get := queue.io.state
}
