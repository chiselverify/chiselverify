package examples.leros

import chisel3._
import chisel3.util._
import chisel3.experimental._

class accualu extends BlackBox with HasBlackBoxResource {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())
    val op = Input(UInt(3.W))
    val din = Input(UInt(32.W))
    val ena = Input(Bool())
    val accu = Output(UInt(32.W))
  })

  //Points to the Verilog file, which is located under src/main/resources
  setResource("/accualu_converted.v")
}

class AluAccuGenerated(size: Int) extends AluAccu(size) {

  // TODO: can we parameterize the generated Verilog?
  val AluAccu = Module(new accualu())

  //Needs to be done with explicit clock and reset
  // because blackbox does not have this defined.
  AluAccu.io.op := io.op
  AluAccu.io.din := io.din
  AluAccu.io.ena := io.ena
  io.accu := AluAccu.io.accu
  AluAccu.io.clock := clock
  AluAccu.io.reset := reset
}
