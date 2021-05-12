package examples.newpriorityqueue.heapifier

import chisel3._
import chisel3.util._
import examples.newpriorityqueue.MemoryPort
import examples.newpriorityqueue.util.OrderedData

import scala.reflect.ClassTag

/**
 * @param size
 * @param order
 * @tparam T
 */
class HeapifierDatapath[T <: Data with OrderedData[T]](size: Int, order: Int)(implicit tag: ClassTag[T]) extends MultiIOModule {

  //// I/O
  val mem = IO(new MemoryPort[T](order,log2Ceil(size)))
  mem.reset()
  val fsmChannel  = IO(Flipped(new HeapifierChannel))
  ////

  //// Indexing
  val indexReg    = RegInit(0.U(log2Ceil(size).W))
  //// Data registers
  val childrenReg = RegInit(0.U.asTypeOf(Vec(order, tag.runtimeClass.getConstructor().newInstance().asInstanceOf[T])))
  val parentReg   = RegInit(0.U.asTypeOf(Vec(order, tag.runtimeClass.getConstructor().newInstance().asInstanceOf[T])))
  ////


  switch(fsmChannel.ctrl.loadOp){
    is(MemoryLoad.toChildrenReg){
      childrenReg := mem.rd.data
    }
    is(MemoryLoad.toParentReg){
      parentReg := mem.rd.data
    }
  }

  switch(fsmChannel.ctrl.storeOp){
    is(MemoryWrite.fromChildrenReg){
      mem.wr.data := childrenReg
    }
    is(MemoryWrite.fromParentReg){
      mem.wr.data := parentReg
    }
  }


}
