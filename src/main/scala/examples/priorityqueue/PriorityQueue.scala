package examples.priorityqueue


import chisel3._
import chisel3.util._

//TODO: build a connection to AXI and test using bus functional model?



class PriorityQueue[T <: Data](treeSize: Int, treeOrder: Int, typeGen: => T)(reductionOp: (T,T) => T) extends MultiIOModule {

  implicit val params: SharedParameters[T] = new SharedParameters(treeSize,treeOrder,typeGen,reductionOp)

  val io = IO(new Bundle{
    val query    = Flipped(Decoupled(new QueryBundle))
    val response = Output(new ResponseBundle)
    val head     = Valid(new HeadBundle)
  })

  io.query.ready := 1.B
  io.head.bits.none := 1.B
  io.head.bits.item := 0.U.asTypeOf(new QueueItem)
  io.head.valid := 1.B
  io.response.error := 0.B
  io.response.lastRemovedItem := 0.U.asTypeOf(typeGen)
}


object PriorityQueue {
  def apply[T <: Data](treeSize: Int, treeOrder: Int, typeGen: => T)(reductionOp: (T,T) => T): PriorityQueue[T] = new PriorityQueue[T](treeSize,treeOrder,typeGen)(reductionOp)
  //def apply[T <: Data with Num[T]](treeSize: Int, treeOrder: Int, typeGen: => T)(reductionOp: (T,T) => T = (a: T,b: T) => Mux(a<b,a,b)): PriorityQueue[T] = new PriorityQueue[T](treeSize,treeOrder,typeGen)(reductionOp)
}


object Test extends App {
  stage.ChiselStage.emitVerilog(PriorityQueue(32,2,UInt(2.W)){ (l, r) =>
    val res = Wire(chiselTypeOf(l))
    when(l =/= r){
      res := Mux(l < r, l, r)
    }otherwise{
      res := l
    }
    res
  })
}