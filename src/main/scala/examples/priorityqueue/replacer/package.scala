package examples.priorityqueue

import chisel3._
import chisel3.experimental.ChiselEnum

package object replacer {

  object State extends ChiselEnum {
    val Idle, IssueRead, ReceiveRead, IssueWrite = Value
  }

  class CommandBundle[T <: Data](implicit params: SharedParameters[T]) extends Bundle {
    val sourceIndex = UInt(params.indexWidth.W)
    val destinationIndex = UInt(params.indexWidth.W)
    override def cloneType: CommandBundle.this.type = (new CommandBundle).asInstanceOf[this.type]
  }

}
