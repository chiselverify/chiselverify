package examples.priorityqueue

import chisel3._
import chisel3.experimental.ChiselEnum

package object inserter {

  object State extends ChiselEnum {
    val Idle, IssueWrite = Value
  }

  class CommandBundle[T <: Data](implicit params: SharedParameters[T]) extends Bundle {
    val index = UInt(params.indexWidth.W)
    val item = new QueueItem
    override def cloneType: CommandBundle.this.type = (new CommandBundle).asInstanceOf[this.type]

  }
}
