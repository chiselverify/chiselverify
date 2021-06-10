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

  val superCycleWidth = superCycleRes
  val cycleWidth = log2Ceil(cyclesPerSuperCycle)
  val referenceIdWidth = log2Ceil(size+1)
  implicit val parameters = PriorityQueueParameters(size,order,superCycleWidth,cycleWidth,referenceIdWidth)

  val io = IO(new Bundle {

    // Interface for signaling head element to user.
    // I.e. the element with the lowest priority
    val head = Output(new HeadBundle)

    // Interface for element insertion/removal
    // Timing:
    // User must maintain input asserted until done is asserted.
    // User must deassert input when done is asserted (unless a new cmd is made).
    // User must ensure that reference ID tags are unique.
    val query = Input(new QueryBundle)
    val resp = Output(new ResponseBundle)

    val state = if (exposeState) Some(Output(UInt())) else None

  })


  require(isPow2(order), "The number of children per node needs to be a power of 2!")

  val mem = Module(new linearSearchMem(size - 1))
  val queue = Module(new QueueControl)

  mem.srch <> queue.io.srch
  mem.rd <> queue.io.rdPort
  mem.wr <> queue.io.wrPort
  io.head <> queue.io.head
  io.resp <> queue.io.resp
  io.query <> queue.io.query

  io.resp.done := mem.srch.done && queue.io.resp.done

  if (exposeState) io.state.get := queue.io.state

}
