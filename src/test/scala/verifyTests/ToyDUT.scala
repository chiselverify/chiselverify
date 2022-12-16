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
      val outSum = Output(UInt((size + 1).W))
    })
    val regA: Counter = Counter(size + 10)
    regA.inc()

    io.outA := io.a
    io.count := regA.value
    io.outB := io.b
    io.outC := io.c
    io.outSum := io.a + regA.value
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
      val isOne = Output(UInt(1.W))
      val outA = Output(UInt(size.W))
      val outB = Output(UInt(size.W))
      val outC = Output(UInt(size.W))
      val outCSupB = Output(UInt((size + 1).W))
      val outCNotSupB = Output(UInt(size.W))
    })

    //Create a counter that should eventually reach a
    val c = Counter(size + 10)
    val aWire = RegInit(io.a)
    val aHasEqb = RegInit(0.U)
    val cSupb = RegInit(0.U)

    c.inc()

    aWire := io.a
    when(c.value > io.a) {
      aWire := c.value
    }

    io.aNevEqb := 1.U
    when(aWire === io.b) {
      aHasEqb := 1.U
    }

    when(c.value > io.b) {
      cSupb := 1.U
    }

    //Set outputs
    io.aEqb := (aWire === io.b).asUInt()
    io.aNevEqb := (aHasEqb === 0.U).asUInt()
    io.aEvEqC := (aWire === c.value).asUInt()
    io.isOne := 1.U
    io.outA := io.a
    io.outB := io.b
    io.outC := c.value
    io.outCSupB := io.b + cSupb
    io.outCNotSupB := io.b - cSupb
  }
}
