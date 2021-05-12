package examples.newpriorityqueue.heapifier

import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util._


object MemoryLoad extends ChiselEnum {
  val none, toChildrenReg, toParentReg = Value
}

object MemoryWrite extends ChiselEnum {
  val none, fromChildrenReg, fromParentReg = Value
}