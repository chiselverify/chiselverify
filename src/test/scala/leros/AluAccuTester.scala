package leros

import chisel3._
import chisel3.iotesters._
import org.scalatest._

import leros.Types._

class AluAccuTester(dut: AluAccu) extends PeekPokeTester(dut) {

  // TODO: this is not the best way look at functions defined as Enum.
  // Workaround would be defining constants

  def alu(a: Int, b: Int, op: Int): Int = {

    op match {
      case 1 => a + b
      case 2 => a - b
      case 3 => a & b
      case 4 => a | b
      case 5 => a ^ b
      case 6 => b
      case 7 => a >>> 1
      case _ => -123 // This shall not happen
    }
  }


  def testOne(a: Int, b: Int, fun: BigInt): Unit = {
    poke(dut.io.op, ld)
    poke(dut.io.ena, 1)
    poke(dut.io.din, a.toLong & 0x00ffffffffL)
    step(1)
    poke(dut.io.op, fun)
    poke(dut.io.din, b.toLong & 0x00ffffffffL)
    step(1)
    expect(dut.io.accu, alu(a, b, fun.toInt).toLong & 0x00ffffffffL)
  }

  def test(values: Seq[Int]) = {
    for (fun <- add to shr) {
      for (a <- values) {
        for (b <- values) {
          testOne(a, b, fun)
        }
      }
    }
  }

  // Some interesting corner cases
  val interesting = Array(1, 2, 4, 123, 0, -1, -2, 0x80000000, 0x7fffffff)
  test(interesting)

  val randArgs = Seq.fill(100)(scala.util.Random.nextInt)
  test(randArgs)

}

class AluTesterSpec extends FlatSpec with Matchers {

  "AluAccu" should "pass" in {
    iotesters.Driver(() => new AluAccu(32)) {
      c => new AluAccuTester(c)
    }
  }
}
