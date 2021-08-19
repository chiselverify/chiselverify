package examples
import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.util.{Decoupled, DecoupledIO, Valid, ValidIO, log2Ceil}

package object priorityqueue {

  class SharedParameters[T <: Data](val size: Int, val order: Int, gen: => T, reductionOp: (T,T) => T) {
    val addressWidth = log2Ceil(size+1)
    val referenceWidth = log2Ceil(size+1)
    val indexWidth = log2Ceil(size+1)
    def typeGen(): T = gen
    def reduce(left: T, right: T): T = reductionOp(left,right)
  }

  object PriorityQueueOperation extends ChiselEnum {
    val Remove = Value(0.U)
    val Insert = Value(1.U)
  }

  class QueryBundle[T <: Data](implicit params: SharedParameters[T]) extends Bundle {
    val op = PriorityQueueOperation()
    val item = new QueueItem
    override def cloneType: QueryBundle.this.type = (new QueryBundle).asInstanceOf[this.type]
  }

  class ResponseBundle[T <: Data](implicit params: SharedParameters[T]) extends Bundle {
    val error = Bool()
    val lastRemovedItem = params.typeGen()
    override def cloneType: ResponseBundle.this.type = (new ResponseBundle).asInstanceOf[this.type]
  }

  class HeadBundle[T <: Data](implicit params: SharedParameters[T]) extends Bundle {
    val none = Bool()
    val item = new QueueItem
    override def cloneType: HeadBundle.this.type = (new HeadBundle).asInstanceOf[this.type]
  }

  // TODO: should an item be marked active or should a check be made before it is fed into the min finder? Latter is more scalable
  class QueueItem[T <: Data](implicit params: SharedParameters[T]) extends Bundle {
    val value = params.typeGen()
    val id = UInt(params.referenceWidth.W)
    override def cloneType: QueueItem.this.type = (new QueueItem).asInstanceOf[this.type]
  }

  class HeapReadRequestIO[T <: Data](implicit params: SharedParameters[T]) extends Bundle {
    val index        = UInt(params.indexWidth.W)
    val withSiblings = Bool()
    override def cloneType: HeapReadRequestIO.this.type = (new HeapReadRequestIO).asInstanceOf[this.type]
  }

  class HeapReadResponseIO[T <: Data](implicit params: SharedParameters[T]) extends Bundle {
    val items = Vec(params.order,new QueueItem)
    override def cloneType: HeapReadResponseIO.this.type = (new HeapReadResponseIO).asInstanceOf[this.type]
  }

  class HeapReadIO[T <: Data](implicit params: SharedParameters[T]) extends Bundle {
    val request = Decoupled(new HeapReadRequestIO)
    val response = Flipped(Valid(new HeapReadResponseIO))
    override def cloneType: HeapReadIO.this.type = (new HeapReadIO).asInstanceOf[this.type]
  }

  class HeapWriteRequestIO[T <: Data](implicit params: SharedParameters[T]) extends Bundle {
    val index = UInt(params.indexWidth.W)
    val item = new QueueItem
    override def cloneType: HeapWriteRequestIO.this.type = (new HeapWriteRequestIO).asInstanceOf[this.type]
  }

  class HeapWriteIO[T <: Data](implicit params: SharedParameters[T]) extends Bundle {
    val request = Decoupled(new HeapWriteRequestIO)
    override def cloneType: HeapWriteIO.this.type = (new HeapWriteIO).asInstanceOf[this.type]
  }

  class HeapAccessIO[T <: Data](implicit params: SharedParameters[T]) extends Bundle {
    val read = new HeapReadIO
    val write = new HeapWriteIO
    override def cloneType: HeapAccessIO.this.type = (new HeapAccessIO).asInstanceOf[this.type]
  }
}
