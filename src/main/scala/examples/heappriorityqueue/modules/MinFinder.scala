package examples.heappriorityqueue.modules

import chisel3._
import chisel3.util._

import examples.heappriorityqueue.Interfaces.TaggedEvent
import examples.heappriorityqueue.PriorityQueueParameters

/**
  * Determines the highest priority (smallest number) with the lowest index among the input values.
  * Outputs both the value and index
  *
  * @param n    number of priorities to compare
  * @param cycleWidth width of the normal priority field
  * @param superCycleWidth width of the cyclic priority field
  * @param referenceIdWidth width of the reference ID
  */
class MinFinder(n: Int)(implicit parameters: PriorityQueueParameters) extends Module {
  import parameters._
  val io = IO(new Bundle {
    val values = Input(Vec(n, new TaggedEvent))
    val res = Output(new TaggedEvent)
    val index = Output(UInt(log2Ceil(n).W))
  })
  
  // lowest cyclic priority wins
  // if cyclic priorities are equal the normal priority decides
  // if both are equal the lowest index wins
  val res = io.values.zipWithIndex.map{ case (d, i) => (d,i.U) }.reduce{ (left,right) =>
    val selectLeft = left._1.event < right._1.event || (left._1.event === right._1.event && left._2 < right._2)
    (Mux(selectLeft,left._1,right._1),Mux(selectLeft,left._2,right._2))
  }

  io.res := res._1
  io.index := res._2
}
