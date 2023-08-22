package examples

import chisel3._
import chiseltest._
import chiseltest.internal.NoThreadingAnnotation
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import chiselverify.approximation._
import chiselverify.approximation.Metrics._

trait ApproxAdderTest extends AnyFlatSpec with ChiselScalatestTester {
  val NumTests: Int = 100000
  val Width: Int = 32
  val ApproxWidths: Seq[Int] = Seq(4, 8, 12, 16)
  val ExpectedResults: Seq[Boolean] = Seq(true, true, true, false)

  // Adder IO bundle
  class AdderIO(width: Int) extends Bundle {
    val a    = Input(UInt(width.W))
    val b    = Input(UInt(width.W))
    val cin  = Input(Bool())
    val s    = Output(UInt(width.W))
    val cout = Output(Bool())
  }

  /** 
    * Exact adder
    * @param width width of the adder
    */
  class Adder(width: Int) extends Module {
    val io   = IO(new AdderIO(width))
    val sum  = Wire(UInt((width+1).W))
    sum     := io.a +& io.b + io.cin
    io.s    := sum(width-1, 0)
    io.cout := sum(width)
  }

  /** 
    * Lower-part OR approximate adder
    * @param width width of the adder
    * @param approxWidth width of the approximate part (must be less than the width)
    */
  class LOA(width: Int, approxWidth: Int) extends Module {
    require(approxWidth <= width)
    val io   = IO(new AdderIO(width))
    val ors  = Wire(UInt(approxWidth.W))
    val sum  = Wire(UInt((width-approxWidth+1).W))
    ors     := io.a(approxWidth-1, 0) | io.b(approxWidth-1, 0)
    sum     := io.a(width-1, approxWidth) +& io.b(width-1, approxWidth)
    io.s    := sum(width-approxWidth-1, 0) ## ors
    io.cout := sum(width-approxWidth)
  }
}

class StandaloneDUTTest extends ApproxAdderTest with Matchers {
  behavior of "Standalone approximate adder"

  // Generate some random inputs to the adder and sample its registered outputs
  def simpleTest(dut: LOA, er: ErrorReporter): Unit = {
    val rng  = new scala.util.Random(42) // RNG for operand generation
    val mask = (BigInt(1) << Width) - 1  // bit mask for the output

    // Generate a bunch of random sums
    val aNums = Array.fill(NumTests) { BigInt(Width, rng) }
    val bNums = Array.fill(NumTests) { BigInt(Width, rng) }
    val cins  = Array.fill(NumTests) { rng.nextBoolean() }
    val res   = (0 until NumTests).map { i =>
      val sum = aNums(i) + bNums(i) + (if (cins(i)) 1 else 0)
      (sum >> Width, sum & mask)
    }

    // Apply the inputs and collect the outputs
    (0 until NumTests).foreach { i =>
      dut.io.a.poke(aNums(i).U)
      dut.io.b.poke(bNums(i).U)
      dut.io.cin.poke(cins(i).B)
      dut.clock.step()
      er.sample(Map(dut.io.cout -> (res(i)._1 & 0x1), dut.io.s -> res(i)._2))
    }
  }

  // Run tests for each specified width of the approximate adder's lower part
  it should "verify in comparison to software emulation" in {
    ApproxWidths.zip(ExpectedResults).foreach { case (approxWidth, mtrcsSatisfied) =>
      test(new LOA(Width, approxWidth)) { dut =>
        // Create a new ErrorReporter with two constraints
        val er = new ErrorReporter(
          constrain(dut.io.s, RED(.1), MRED(.025)),
          constrain(dut.io.cout, ER(.01))
        )
        simpleTest(dut, er)
        er.verify() should be (mtrcsSatisfied)
      }
    }
  }
}

class CombinedDUTTest extends ApproxAdderTest with Matchers {
  behavior of "Approximate adder in combined DUT"

  /** 
    * Custom top-level module containing both approximate and exact adders
    * @param width width of the adders
    * @param approxWidth width of the approximate adders lower part (must be less than the width)
    */
  class DUT(width: Int, approxWidth: Int) extends Module {
    val io = IO(new Bundle {
      val a    = Input(UInt(width.W))
      val b    = Input(UInt(width.W))
      val cin  = Input(Bool())
      // Approximate outputs
      val sA    = Output(UInt(width.W))
      val coutA = Output(Bool())
      // Exact outputs
      val sE    = Output(UInt(width.W))
      val coutE = Output(Bool())
    })
    val approxAdder = Module(new LOA(width, approxWidth))
    val exactAdder  = Module(new Adder(width))
    // Connect inputs
    approxAdder.io.a   := io.a
    approxAdder.io.b   := io.b
    approxAdder.io.cin := io.cin
    exactAdder.io.a    := io.a
    exactAdder.io.b    := io.b
    exactAdder.io.cin  := io.cin
    // Connect approximate outputs
    io.sA    := approxAdder.io.s
    io.coutA := approxAdder.io.cout
    // Connect exact outputs
    io.sE    := exactAdder.io.s
    io.coutE := exactAdder.io.cout
  }

  // Generate some random inputs to the adder and sample its registered outputs
  def simpleTest(dut: DUT, er: ErrorReporter): Unit = {
    val rng = new scala.util.Random(42) // RNG for operand generation

    // Generate a bunch of random inputs
    val aNums = Array.fill(NumTests) { BigInt(Width, rng) }
    val bNums = Array.fill(NumTests) { BigInt(Width, rng) }
    val cins  = Array.fill(NumTests) { rng.nextBoolean() }

    // Apply the inputs and collect the outputs
    (0 until NumTests).foreach { i =>
      dut.io.a.poke(aNums(i).U)
      dut.io.b.poke(bNums(i).U)
      dut.io.cin.poke(cins(i).B)
      dut.clock.step() // advance time for visual changes in VCD
      er.sample()
    }
  }

  // Run tests for each specified width of the approximate adder's lower part 
  // without any constraints
  it should "test designs with different approxWidths without constraints" in {
    ApproxWidths.zip(ExpectedResults).foreach { case (approxWidth, mtrcsSatisfied) =>
      test(new DUT(Width, approxWidth)).withAnnotations(Seq(NoThreadingAnnotation)) { dut =>
        // Create a new ErrorReporter with no constraints
        val er = new ErrorReporter()
        simpleTest(dut, er)
        er.verify() should be (true)
      }
    }
  }

  // Run tests for each specified width of the approximate adder's lower part 
  // with two simple constraints
  it should "verify designs with different approxWidths with constraints" in {
    ApproxWidths.zip(ExpectedResults).foreach { case (approxWidth, mtrcsSatisfied) =>
      test(new DUT(Width, approxWidth)).withAnnotations(Seq(NoThreadingAnnotation)) { dut =>
        // Create a new ErrorReporter with two constraints
        val er = new ErrorReporter(
          constrain(dut.io.sA, dut.io.sE, ED(1 << (approxWidth + 1)), RED(.1), MRED(.025)),
          constrain(dut.io.coutA, dut.io.coutE, ER(.01))
        )
        simpleTest(dut, er)
        er.verify() should be (mtrcsSatisfied)
      }
    }
  }
}
