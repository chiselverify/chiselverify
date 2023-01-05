package verifyTests.coverage

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import chiselverify.coverage.GlobalCoverage._
import verifyTests.ToyDUT._

class GlobalCoverageTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {

  def toUInt(i: Int): UInt = (BigInt(i) & 0x00ffffffffL).asUInt(32.W)

  /**
    * Tests functional coverage in a generic use case
    */
  def testGeneric[T <: BasicToyDUT](dut: T): Unit = {
    val coverage = new QueryableCoverage(dut)

    /**
      * Basic test to see if we get the right amount of hits
      */
    def testOne(): Unit = {
      for (fun <- 0 until 50) {
        dut.io.a.poke(toUInt(fun))
        dut.io.b.poke(toUInt(fun % 4))

        coverage.sample()
      }
    }

    testOne()

    coverage.get(dut.io.outA, 50).coverage should be (100)
    coverage.get(dut.io.outB).hits should be(4)
    coverage.get(dut.io.outAB).hits should be(26)

    coverage.get(dut.io.outA, range = Some(0 to 1)).hits should be(2)
  }

  "Coverage" should "get the right amount of hits" in {
    test(new BasicToyDUT(32)){ dut => testGeneric(dut) }
  }
}
