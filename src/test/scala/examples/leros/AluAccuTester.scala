package examples.leros

import chisel3._
import chiseltest._
import org.scalatest._
import chiseltest.experimental.TestOptionBuilder._
import chiseltest.internal.VerilatorBackendAnnotation
import chiselverify.coverage.CoverageReporter
import chiselverify.coverage.{Bins, CoverPoint}
import examples.leros.Types._

class AluAccuTester extends FlatSpec with ChiselScalatestTester with Matchers {

  def testFun[T <: AluAccu](dut: T): Unit = {

    val cr = new CoverageReporter(dut)
    cr.register(
      //Declare CoverPoints
      CoverPoint(dut.io.accu, "accu")( //CoverPoint 1
        Bins("lo10", 0 to 10) :: Bins("First100", 0 to 100) :: Nil
      ) ::
        // CoverPoint(dut.io.test, "test", //CoverPoint 2
        //     Bins("testLo10", 0 to 10)::Nil)::
        Nil
    )
    //Declare cross points
    /*Cross("accuAndTest", "accu", "test",
            CrossBin("both1", 1 to 1, 1 to 1)::Nil)::
        Nil)*/

    def alu(a: Int, b: Int, op: Int): Int = {

      op match {
        case 0 => a
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

    def toUInt(i: Int) = (BigInt(i) & 0x00ffffffffL).asUInt(32.W)

    def testOne(a: Int, b: Int, fun: Int): Unit = {
      dut.io.op.poke(ld)
      dut.io.ena.poke(true.B)
      dut.io.din.poke(toUInt(a))
      dut.clock.step()
      dut.io.op.poke(fun.asUInt())
      dut.io.din.poke(toUInt(b))
      dut.clock.step()
      dut.io.accu.expect(toUInt(alu(a, b, fun)))

      cr.sample()
    }

    def test(values: Seq[Int]): Unit = {
      // for (fun <- add to shr) {
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

    //Print coverage report
    cr.printReport()
  }

  "AluAccuChisel" should "pass" in {
    test(new AluAccuChisel(32)) { dut => testFun(dut) }
  }

  "AluAccuGenerated" should "pass" in {
    test(new AluAccuGenerated(32)).withAnnotations(Seq(VerilatorBackendAnnotation)) { dut => testFun(dut) }
  }
}
