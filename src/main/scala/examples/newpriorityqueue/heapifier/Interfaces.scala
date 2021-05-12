package examples.newpriorityqueue.heapifier

import chisel3._

class HeapifierChannel extends Bundle {
  val ctrl = Output(new HeapifierControlInterface)
  val resp = Input(new HeapifierResponseInterface)
}

class HeapifierControlInterface extends Bundle {
  val loadOp  = MemoryLoad()
  val storeOp = MemoryWrite()
}

class HeapifierResponseInterface extends Bundle {
}