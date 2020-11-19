import chisel3._
import chiseltest._
import org.scalatest._

import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.VerilatorBackendAnnotation

import examples.leros._

// Creating an enumerated type since the "Types" Enum in examples.leros package is a
// Chisel enum, so it return UInts, but we need Ints. Enum to int conversion is done
// with <EnumVariable>.id
object LerosTypes extends Enumeration {
	type LerosTypes = Int
	val NOP, ADD, SUB, AND, OR, XOR, LD, SHR = Value
}
import LerosTypes._

class AluAccuTesterJNI extends FlatSpec with ChiselScalatestTester with Matchers {

  def testFun[T <: AluAccu](dut: T): Unit = {
	  val ns = new NativeScoreboard

    def toUInt(i: Int) = (BigInt(i) & 0x00ffffffffL).asUInt(32.W)

    def testOne(a: Int, b: Int, fun: Int): Unit = {
      dut.io.op.poke(toUInt(LD.id))
      dut.io.ena.poke(true.B)
      dut.io.din.poke(toUInt(a))
      dut.clock.step()
		//Check that load was correctly executed
		dut.io.accu.expect(toUInt(ns.calc(a, LD.id, 0))) 


      dut.io.op.poke(fun.asUInt())
      dut.io.din.poke(toUInt(b))
      dut.clock.step()
      // dut.io.accu.expect(toUInt(ns.calc))
		dut.io.accu.expect(toUInt(ns.calc(b, fun, 0)))
    }

    def test(values: Seq[Int]) = {
      for (fun <- 0 until 8) {
        for (a <- values) {
          for (b <- values) {
            testOne(a, b, fun)
          }
        }
      }
    }

    // Some interesting corner cases
    val interesting = Array(1, 2, 4, -123, 123, 0, -1, -2, 0x80000000, 0x7fffffff)
    test(interesting)

    val randArgs = Seq.fill(20)(scala.util.Random.nextInt)
    test(randArgs)

    println("Hello tester")
  }
	def toUInt(i: Int) = (BigInt(i) & 0x00ffffffffL).asUInt(32.W)

  	def myTest(dut: AluAccuChisel): Unit = {
	 	val ns = new NativeScoreboard

		//poke values
		dut.io.op.poke(toUInt(LD.id))
		dut.io.din.poke(toUInt(3))
		dut.io.ena.poke(true.B)
		dut.clock.step()

		//Assertions
		dut.io.accu.expect(toUInt(ns.calc(3, LD.id, 0)))

		dut.io.op.poke(toUInt(ADD.id))
		dut.io.din.poke(toUInt(5))
		dut.clock.step()

		dut.io.accu.expect(toUInt(ns.calc(5, ADD.id, 0)))
		// assert(ns.check(3, ld, 0, dut.io.accu.peek.litValue.asInstanceOf(Int)), 1)
  }


  "AluAccuChisel" should "pass" in {
    test(new AluAccuChisel(32)){ dut => testFun(dut)}
  }

  "AluAccuGenerated" should "pass" in {
    test(new AluAccuGenerated(32)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut => testFun(dut) }
  }
}
