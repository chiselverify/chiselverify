package verifyTests.coverage

import chisel3._
import chiseltest._
import chiselverify.coverage._
import chiselverify.timing.TimedOp.{Equals, Gt, GtEq, Lt, LtEq}
import chiselverify.timing._
import verifyTests.ToyDUT._
import org.scalatest._
import chiselverify.coverage.Utils.{rangeToOption, stringToOption}

class FunctionalCoverageTest extends FlatSpec with ChiselScalatestTester with Matchers {

    def toUInt(i: Int): UInt = (BigInt(i) & 0x00ffffffffL).asUInt(32.W)

    /**
      * Tests functional coverage in a generic use case
      */
    def testGeneric[T <: BasicToyDUT](dut: T): Unit = {

        val cr = new CoverageReporter(dut)
        cr.register(
            cover("accu", dut.io.outA)( //CoverPoint 1
                bin("lo10", 0 until 10), bin("First100", 0 until 100)),
            cover("test", dut.io.outB)( //CoverPoint 2
                 bin("testLo10", 0 until 10)),
            //Declare cross points
            cover("accuAndTest", dut.io.outA, dut.io.outB)(
                cross("both1", Seq(1 to 1, 1 to 1)))
        )

        /**
          * Basic test to see if we get the right amount of hits
          */
        def testOne(): Unit = {
            for (fun <- 0 until 50) {
                dut.io.a.poke(toUInt(fun))
                dut.io.b.poke(toUInt(fun % 4))

                cr.sample()
            }
        }

        testOne()

        //Generate report
        val report = cr.report
        print(report.serialize)

        //Check that the number of hits is correct
        report.binNHits(1, "accu", "lo10") should be (BigInt(10))
        report.binNHits(1, "accu", "First100") should be (BigInt(50))
        report.binNHits(1, "test", "testLo10") should be (BigInt(4))
        report.binNHits(1, "accuAndTest", "both1") should be (BigInt(1))
    }

    def testCond[T <: BasicToyDUT](dut: T): Unit = {

        val cr = new CoverageReporter(dut)
        cr.register(
            //Declare CoverPoints
            cover("accu", dut.io.outA)( //CoverPoint 1
                bin("lo10even", 0 until 10, (x : Seq[BigInt]) => x.head % 2 == 0),
                bin("First100odd", 0 until 100, (x: Seq[BigInt]) => x.head % 2 != 0 )),
            cover("test", dut.io.outB)( //CoverPoint 2
                bin("testLo10", 0 until 10))
        )

        /**
          * Basic test to see if we get the right amount of hits
          */
        def testOne(): Unit = {
            for (fun <- 0 until 100) {
                dut.io.a.poke(toUInt(fun))
                dut.io.b.poke(toUInt(fun % 4))

                cr.sample()
            }
        }

        testOne()

        //Generate report
        val report = cr.report
        print(report.serialize)

        //Check that the number of hits is correct
        report.binNHits(1, "accu", "lo10even") should be (BigInt(5))
        report.binNHits(1, "accu", "First100odd") should be (BigInt(50))
        report.binNHits(1, "test", "testLo10") should be (BigInt(4))
    }

    def testCovCond[T <: BasicToyDUT](dut: T): Unit = {

        val cr = new CoverageReporter(dut)
        cr.register(
            //Declare CoverPoints
            cover("aAndB", dut.io.outA, dut.io.outB)(
                bin("aeqb", condition = (x: Seq[BigInt]) => x.head == x(1)),
                bin("asuptobAtLeast100", condition = (x: Seq[BigInt]) => x.head > x(1), expectedHits = 100)
            )
        )

        /**
          * Basic test to see if we get the right amount of hits
          */
        def testOne(): Unit = {
            for (fun <- 0 until 100) {
                dut.io.a.poke(toUInt(fun % 95))
                dut.io.b.poke(toUInt(fun % 4))

                cr.sample()
            }
        }

        testOne()

        //Generate report
        val report = cr.report
        print(report.serialize)

        //Check that the number of hits is correct
        report.binNHits(1, "aAndB", "aeqb") should be (BigInt(4))
        report.binNHits(1, "aAndB", "asuptobAtLeast100") should be (BigInt(95))
    }



    /**
      * Tests the default bins in functional coverage points
      */
    def testDefaults[T <: BasicToyDUT](dut: T): Unit = {
        val cr = new CoverageReporter(dut)
        cr.register(
            //Declare CoverPoints with default bins
            cover("a", dut.io.outA)(DefaultBin(dut.io.outA)),
            cover("b", dut.io.outB)(DefaultBin(dut.io.outB)),
            cover("aplusb", dut.io.outAB)(DefaultBin(dut.io.outAB))
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

    /** FOR FUTURE TESTING */
    def testDefaultVP[T <: BasicToyDUT](dut: T): Unit = {
        println("TESTING DEFAULT VERIFICATION PLAN")
        val cr = new CoverageReporter(dut)

        //Don't register anything

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

    /**
      * Tests the timed functional coverage relations
      */
    def testTimed[T <: TimedToyDUT](dut: T): Unit = {
        val cr = new CoverageReporter(dut)
        cr.register(
            //Declare CoverPoints
            cover("a", dut.io.outA)( //CoverPoint 1
                bin("lo10", 0 until 10)),
            cover("count", dut.io.count)( //CoverPoint 2
                bin("testLo10", 0 until 10)),
            cover("b", dut.io.outB)(
                bin("test10", 0 until 10)
            ),
            cover("c", dut.io.outC)(
                bin("test5", 0 until 5)
            ),
            //Declare timed cross points
            cover("timedAB", dut.io.outA, dut.io.count)(Exactly(3))(
                cross("ExactlyBoth3", Seq(3 to 3, 3 to 3))),
            cover("EventuallyTimedAB", dut.io.outB, dut.io.count)(Eventually(3))(
                cross("both1", Seq(1 to 1, 1 to 1))),
            cover("AlwaysTimedAB", dut.io.outC, dut.io.outA)(Always(3))(
                cross("AlwaysBoth3", Seq(3 to 3, 3 to 3))
            )
        )


        /**
          * Basic test to see if we get the right amount of hits
          */
        def testTime(): Unit = {
            dut.io.a.poke(3.U)
            dut.io.b.poke(1.U)
            dut.io.c.poke(3.U)
            cr.step(3)
            dut.io.c.poke(0.U)
            cr.sample()
        }

        testTime()

        //Generate report
        val report = cr.report
        cr.printReport()

        report.binNHits(1, "timedAB", "ExactlyBoth3") should be (1)
        report.binNHits(1, "EventuallyTimedAB", "both1") should be (1)
        report.binNHits(1, "AlwaysTimedAB", "AlwaysBoth3") should be (1)
    }

    /**
      * Tests that the timed coverage only works when stepping with the reporter
      */
    def testTimedFail[T <: TimedToyDUT](dut: T): Unit = {
        val cr = new CoverageReporter(dut)
        cr.register(
            //Declare CoverPoints
            cover("a", dut.io.outA)( //CoverPoint 1
                bin("lo10", 0 until 10)),
            cover("b", dut.io.outB)( //CoverPoint 2
                bin("testLo10", 0 until 10)),
            //Declare timed cross points
            cover("timedAB", dut.io.outA, dut.io.outB)(Exactly(3))(
                cross("both1", Seq(3 to 3, 3 to 3)))
        )

        /**
          * Basic test to see if we get the right exception
          */
        def testFail(): Unit = {
            dut.io.a.poke(3.U)
            dut.clock.step(3)
            cr.sample()
        }

        testFail()

        //Generate report
        assertThrows[IllegalStateException](cr.report)
    }

    def testAll[T <: TimedToyDUT](dut: T): Unit = {
        val cr = new CoverageReporter(dut)
        cr.register(
            //Declare CoverPoints with conditional bins
            cover("accu", dut.io.outA)(
                bin("lo10even", 0 until 10, (x: Seq[BigInt]) => x.head % 2 == 0),
                bin("First100odd", 0 until 100, (x: Seq[BigInt]) => x.head % 2 != 0)),
            //Declare CoverPoints without conditional bins
            cover("test", dut.io.outB)(
                bin("testLo10", 0 until 10)),
            //Declare CoverConditions
            cover("aAndB",dut.io.outA, dut.io.outB)(
                bin("aeqb", condition = (x: Seq[BigInt]) => x.head == x(1)),
                bin("asuptobAtLeast100", condition = (x: Seq[BigInt]) => x.head > x(1), expectedHits = 100)),
            //Declare cross points
            cover("accuAndTest", dut.io.outA, dut.io.outB)(
                cross("both1", Seq(1 to 9 by 2, 1 to 1))),
            //Declare timed cross points
            cover("timedAB", dut.io.outA, dut.io.count)(Exactly(3))(
                cross("ExactlyBoth3", Seq(3 to 3, 3 to 3))),
            cover("EventuallyTimedAB", dut.io.outB, dut.io.count)(Eventually(3))(
                cross("EventuallyBoth1", Seq(1 to 1, 1 to 1))),
            cover("AlwaysTimedAB", dut.io.outC, dut.io.outA)(Always(3))(
                cross("AlwaysBoth3", Seq(3 to 3, 3 to 3)))
        )


        /**
          * Basic test to see if we get the right amount of hits
          */
        def test(): Unit = {
            dut.io.a.poke(3.U)
            dut.io.b.poke(1.U)
            dut.io.c.poke(3.U)
            cr.step(3)
            dut.io.c.poke(0.U)
            cr.sample()
            cr.step()
            dut.io.c.expect(0.U)
        }

        test()

        //Generate report
        val report = cr.report
        cr.printReport()

        report.binNHits(1, "timedAB", "ExactlyBoth3") should be (1)
        report.binNHits(1, "EventuallyTimedAB", "EventuallyBoth1") should be (1)
        report.binNHits(1, "AlwaysTimedAB", "AlwaysBoth3") should be (1)
    }

    "Coverage" should "get the right amount of hits" in {
        test(new BasicToyDUT(32)){ dut => testGeneric(dut) }
    }

    "CoverageWithDefaultBins" should "pass" in {
        test(new BasicToyDUT(32)) {
            dut => testDefaults(dut)
        }
    }

    "CoverageWithCovConditions" should "pass" in {
        test(new BasicToyDUT(32)) {
            dut => testCovCond(dut)

        }
    }

    "CoverageWithDelays" should "pass" in {
        test(new TimedToyDUT(32)) { dut => testTimed(dut) }
    }

    "CoverageWithDelays" should "throw an exception" in {
        test(new TimedToyDUT(32)) { dut => testTimedFail(dut) }
    }

    "CoverageWithConditionalBins" should "pass" in {
        test(new BasicToyDUT(32)) { dut => testCond(dut) }
    }

    "CoverageWithEverything" should "pass" in {
        test(new TimedToyDUT(32)) {dut => testAll(dut)}
    }
}

