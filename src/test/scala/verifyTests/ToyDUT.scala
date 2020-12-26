package verifyTests

import chisel3._
import chisel3.util.Counter
import chisel3.{Bundle, Input, Module, Output, UInt}

object ToyDUT {

  //Used for functional coverage testing
  class TimedToyDUT(size: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt(size.W))
      val b = Input(UInt(size.W))
      val c = Input(UInt(size.W))
      val outA = Output(UInt(size.W))
      val count = Output(UInt(size.W))
      val outB = Output(UInt(size.W))
      val outC = Output(UInt(size.W))
    })
    val regA = Counter(5)

    regA.inc()

    io.outA := io.a
    io.count := regA.value
    io.outB := io.b
    io.outC := io.c
  }

  class BasicToyDUT(size: Int) extends Module {
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

  class AssertionsToyDUT(size: Int) extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt(size.W))
      val b = Input(UInt(size.W))
      val aEqb = Output(UInt(1.W))
      val aNevEqb = Output(UInt(1.W))
      val aEvEqC = Output(UInt(1.W))
    })

    //Create a counter that should eventually reach a
    val c = Counter(size + 10)
    val aWire = RegInit(io.a)

    c.inc()

    aWire := io.a
    when(c.value > io.a) {
      aWire := c.value
    }

    //Set outputs
    io.aEqb := (aWire === io.b).asUInt()
    io.aNevEqb := (aWire =/= io.b).asUInt()
    io.aEvEqC := (aWire === c.value).asUInt()
  }
}
