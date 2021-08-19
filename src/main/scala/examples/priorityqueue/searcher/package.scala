package examples.priorityqueue

import chisel3._
import chisel3.experimental.ChiselEnum

package object searcher {

  object State extends ChiselEnum {
    val Idle, Searching = Value
  }

  class CommandBundle[T <: Data](implicit params: SharedParameters[T]) extends Bundle {
    val id = UInt(params.referenceWidth.W)
    override def cloneType: this.type = (new CommandBundle).asInstanceOf[this.type]
  }

  class ResponseBundle[T <: Data](implicit params: SharedParameters[T]) extends Bundle {
    val index = UInt(params.indexWidth.W)
    override def cloneType: this.type = (new ResponseBundle).asInstanceOf[this.type]
  }

}
