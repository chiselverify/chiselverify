package verifyTests.coverage

import chisel3._
import chiseltest._
import chiselverify.coverage._
import org.scalatest._

class FunctionalCoverageTest extends FlatSpec with ChiselScalatestTester with Matchers {

    def toUInt(i: Int): UInt = (BigInt(i) & 0x00ffffffffL).asUInt(32.W)

    /**
      * Tests functional coverage in a generic use case
      */
    def testGeneric[T <: ToyDUT](dut: T): Unit = {

        val cr = new CoverageReporter
        cr.register(
            //Declare CoverPoints
            CoverPoint(dut.io.outA , "a")( //CoverPoint 1
                Bins("lo10", 0 until 10)::Bins("First100", 0 until 100)::Nil)::
            CoverPoint(dut.io.outB, "b")( //CoverPoint 2
                 Bins("testLo10", 0 until 10)::Nil)::
            Nil,
        //Declare cross points
        Cross("aANDb", "a", "b")(
            CrossBin("both1", 1 to 1, 1 to 1)::Nil)::
        Nil)

        /**
          * Basic test to see if we get the right amount of hits
          */
        def testOne(): Unit = {
            for (fun <- 0 until 50) {
                dut.io.a.poke(toUInt(fun))
                dut.io.b.poke(toUInt(fun % 5))

                cr.sample()
            }
        }

        testOne()

        //Generate report
        val report = cr.report

        //Check that the number of hits is correct
        report.binNHits(1, "a", "lo10") should be (BigInt(10))
        report.binNHits(1, "a", "First100") should be (BigInt(50))
        report.binNHits(1, "b", "testLo10") should be (BigInt(5))
        report.binNHits(1, "aANDb", "both1") should be (BigInt(1))
    }

    /**
      * Tests the default bins in functional coverage points
      */
    def testDefaults[T <: ToyDUT](dut: T): Unit = {
        val cr = new CoverageReporter
        cr.register(
            //Declare CoverPoints with default bins
            CoverPoint(dut.io.outA , "a")()::
            CoverPoint(dut.io.outB, "b")()::
            CoverPoint(dut.io.outAB, "aplusb")():: Nil
        )

        /**
          * Basic test to see if we get the right amount of hits
          */
        def testing(): Unit = {
            for (fun <- 0 until 50) {
                dut.io.a.poke(toUInt(fun))
                dut.io.b.poke(toUInt(fun % 5))

                cr.sample()
            }
        }

        testing()

        //Generate report
        val report = cr.report

        //Check that the number of hits is correct
        report.binNHits(1, "a", "default") should be (BigInt(50))
        report.binNHits(1, "b", "default") should be (BigInt(5))
        report.binNHits(1, "aplusb", "default") should be (BigInt(50))
    }

    "Coverage" should "get the right amount of hits" in {
        test(new ToyDUT(32)){ dut => testGeneric(dut) }
    }

    "CoverageWithDefaultBins" should "pass" in {
        test(new ToyDUT(32)){ dut => testDefaults(dut) }
    }
}