package verifyTests.coverage

import chisel3._

//Used for functional coverage testing
class ToyDUT(size: Int) extends Module {
    val io = IO(new Bundle {
        val a = Input(UInt(size.W))
        val b = Input(UInt(size.W))
        val outA = Output(UInt(size.W))
        val outB = Output(UInt(size.W))
        val outAB = Output(UInt((size + 1).W))
    })

    io.outA := io.a
    io.outB := io.b
    io.outAB := io.a + io.b
}

