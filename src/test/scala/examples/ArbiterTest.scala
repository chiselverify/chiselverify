package examples

import chisel3._
import chiseltest._
import org.scalatest.FlatSpec
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.WriteVcdAnnotation

class ArbiterTest extends FlatSpec with ChiselScalatestTester {

  behavior of "Arbiter"

  it should "pass" in {
    test(new Arbiter(4, UInt(8.W))).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // test(new Arbiter(4, UInt(8.W))) { dut =>
      for (i <- 0 until 4) {
        dut.io.in(i).valid.poke(false.B)
      }
      dut.io.out.ready.poke(false.B) // Keep the output till we read it
      dut.io.in(2).valid.poke(true.B)
      dut.io.in(2).bits.poke(2.U)
      while(!dut.io.in(2).ready.peek().litToBoolean) {
        dut.clock.step()
      }
      dut.clock.step()
      dut.io.in(2).valid.poke(false.B)
      dut.clock.step(10)
      // disable for now to avoid Travis issue.
      dut.io.out.bits.expect(2.U)
    }
  }

  // TODO: finish the test
  it should "be fair" in {
    test(new Arbiter(5, UInt(16.W))).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      for (i <- 0 until 5) {
        dut.io.in(i).valid.poke(true.B)
        dut.io.in(i).bits.poke((i*100).U)
      }
      // println("Result should be " + List(0, 100, 200, 300, 400).sum)
      dut.io.out.ready.poke(true.B)
      dut.clock.step()
      for (i <- 0 until 40) {
        if (dut.io.out.valid.peek().litToBoolean) {
          println(dut.io.out.bits.peek().litValue())
        }
        dut.clock.step()
      }
    }
  }
}

