package examples.newpriorityqueue

import chisel3._

import scala.reflect.ClassTag

class MemoryPort[T <: Data](lanes: Int, addressWidth: Int)(implicit tag: ClassTag[T]) extends Bundle {
  val wr: WriteChannel[T]   = new WriteChannel[T](lanes,addressWidth)
  val rd: ReadChannel[T]    = new ReadChannel[T](lanes,addressWidth)
  def reset(): Unit = {
    wr.reset()
    rd.reset()
  }
}

class WriteChannel[T <: Data](lanes: Int, addressWidth: Int)(implicit tag: ClassTag[T]) extends Bundle {
  val address         = Output(UInt(addressWidth.W))
  val data            = Output(Vec(lanes, tag.runtimeClass.getConstructor().newInstance().asInstanceOf[T]))
  val lanesSelector   = Output(Vec(lanes, new Bool))
  def reset(): Unit = {
    address       := 0.U
    data          := 0.U.asTypeOf(data)
    lanesSelector := 0.U.asTypeOf(lanesSelector)
  }
}


class ReadChannel[T <: Data](lanes: Int, addressWidth: Int)(implicit tag: ClassTag[T]) extends Bundle {
  val address: UInt   = Output(UInt(addressWidth.W))
  val data: Vec[T]    = Input(Vec(lanes, tag.runtimeClass.getConstructor().newInstance().asInstanceOf[T]))
  def reset(): Unit ={
    address := 0.U
  }
}
