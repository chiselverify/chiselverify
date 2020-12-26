package examples.leros

import chisel3._

/** Base class for Leros ALU including the accumulator register.
  *
  * @param size
  */
abstract class AluAccu(size: Int) extends Module {
  val io = IO(new Bundle {
    val op = Input(UInt(3.W))
    val din = Input(UInt(size.W))
    val ena = Input(Bool())
    val accu = Output(UInt(size.W))
  })
}
