package verifyTests.coverage

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import chiselverify._
import chiselverify.coverage.{cover => ccover, _}
import chiselverify.timing._
import verifyTests.ToyDUT._

class FunctionalCoverageTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  def toUInt(i: Int): UInt = (BigInt(i) & 0x00ffffffffL).asUInt(32.W)

  /**
    * Tests functional coverage in a generic use case
    */
  def testGeneric[T <: BasicToyDUT](dut: T): Unit = {
    val cr = new CoverageReporter(dut)
    cr.register(
      ccover("accu", dut.io.outA)( // CoverPoint 1
        bin("lo10", 0 until 10), bin("First100", 0 until 100)),
      ccover("test", dut.io.outB)( // CoverPoint 2
        bin("testLo10", 0 until 10)),
      // Declare cross points
      ccover("accuAndTest", dut.io.outA, dut.io.outB)(
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

    // Generate report
    val report = cr.report

    // Check that the number of hits is correct
    report.binNHits(1, "accu", "lo10") should be (BigInt(10))
    report.binNHits(1, "accu", "First100") should be (BigInt(50))
    report.binNHits(1, "test", "testLo10") should be (BigInt(4))
    report.binNHits(1, "accuAndTest", "both1") should be (BigInt(1))
  }

  /**
    * Tests functional coverage in a generic use case
    */
  def testSingleGroupSample[T <: BasicToyDUT](dut: T): Unit = {
    val cr = new CoverageReporter(dut)
    // Register group 0
    cr.register(
      ccover("accu", dut.io.outA)( // CoverPoint 1
        bin("lo10", 0 until 10), bin("First100", 0 until 100)),
      ccover("test", dut.io.outB)( // CoverPoint 2
        bin("testLo10", 0 until 10)),
      // Declare cross points
      ccover("accuAndTest", dut.io.outA, dut.io.outB)(
        cross("both1", Seq(1 to 1, 1 to 1)))
    )
    // Register group 1
    cr.register(
      ccover("accu1", dut.io.outA)(
        bin("100onlyEven", 0 until 100, (x: Seq[BigInt]) => x.head % 2 == 0)
      )
    )

    /**
      * Test to see if sampling only a subgroup works
      */
    def testOne(): Unit = {
      // Set a to 0
      dut.io.a.poke(toUInt(0))
      for (fun <- 1 until 50) {
        cr.sample(2) // group id 1 should sample 0 to 48

        dut.io.a.poke(toUInt(fun))
        dut.io.b.poke(toUInt(fun % 4))

        cr.sample(1) // group id 0 should sample 1 to 49
      }
    }

    testOne()

    // Generate report
    val report = cr.report

    // Check that the number of hits is correct
    report.binNHits(1, "accu", "lo10") should be (BigInt(9))
    report.binNHits(1, "accu", "First100") should be (BigInt(49))
    report.binNHits(1, "test", "testLo10") should be (BigInt(4))
    report.binNHits(1, "accuAndTest", "both1") should be (BigInt(1))
    report.binNHits(2, "accu1", "100onlyEven") should be (BigInt(25))
  }

  /** 
    * Tests conditional coverage in a generic use case
    */
  def testCond[T <: BasicToyDUT](dut: T): Unit = {
    val cr = new CoverageReporter(dut)
    cr.register(
      // Declare CoverPoints
      ccover("accu", dut.io.outA)( // CoverPoint 1
        bin("lo10even", 0 until 10, (x : Seq[BigInt]) => x.head % 2 == 0),
        bin("First100odd", 0 until 100, (x: Seq[BigInt]) => x.head % 2 != 0 )),
      ccover("test", dut.io.outB)( // CoverPoint 2
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

    // Generate report
    val report = cr.report

    // Check that the number of hits is correct
    report.binNHits(1, "accu", "lo10even") should be (BigInt(5))
    report.binNHits(1, "accu", "First100odd") should be (BigInt(50))
    report.binNHits(1, "test", "testLo10") should be (BigInt(4))
  }

  /** 
    * Tests conditional coverage with expected hits in a generic use case
    */
  def testCovCond[T <: BasicToyDUT](dut: T): Unit = {
    val cr = new CoverageReporter(dut)
    cr.register(
      // Declare CoverPoints
      ccover("aAndB", dut.io.outA, dut.io.outB)(
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

    // Generate report
    val report = cr.report

    // Check that the number of hits is correct
    report.binNHits(1, "aAndB", "aeqb") should be (BigInt(4))
    report.binNHits(1, "aAndB", "asuptobAtLeast100") should be (BigInt(95))
  }

  /**
    * Tests the default bins in functional coverage points
    */
  def testDefaults[T <: BasicToyDUT](dut: T): Unit = {
    val cr = new CoverageReporter(dut)
    cr.register(
      // Declare CoverPoints with default bins
      ccover("a", dut.io.outA)(DefaultBin(dut.io.outA)),
      ccover("b", dut.io.outB)(DefaultBin(dut.io.outB)),
      ccover("aplusb", dut.io.outAB)(DefaultBin(dut.io.outAB))
    )

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

    // Generate report
    val report = cr.report

    // Check that the number of hits is correct
    report.binNHits(1, "a", "default") should be (BigInt(50))
    report.binNHits(1, "b", "default") should be (BigInt(5))
    report.binNHits(1, "aplusb", "default") should be (BigInt(50))
  }

  /** FOR FUTURE TESTING */
  def testDefaultVP[T <: BasicToyDUT](dut: T): Unit = {
    println("TESTING DEFAULT VERIFICATION PLAN")
    val cr = new CoverageReporter(dut)

    // Don't register anything

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

    // Generate report
    val report = cr.report

    // Check that the number of hits is correct
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
      // Declare CoverPoints
      ccover("a", dut.io.outA)( // CoverPoint 1
        bin("lo10", 0 until 10)),
      ccover("count", dut.io.count)( // CoverPoint 2
        bin("testLo10", 0 until 10)),
      ccover("b", dut.io.outB)(
        bin("test10", 0 until 10)),
      ccover("c", dut.io.outC)(
        bin("test5", 0 until 5)),
      // Declare timed cross points
      ccover("timedAB", dut.io.outA, dut.io.count)(Exactly(3))(
        cross("ExactlyBoth3", Seq(3 to 3, 3 to 3))),
      ccover("EventuallyTimedAB", dut.io.outB, dut.io.count)(Eventually(3))(
        cross("both1", Seq(1 to 1, 1 to 1))),
      ccover("AlwaysTimedAB", dut.io.outC, dut.io.outA)(Always(3))(
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

    // Generate report
    val report = cr.report

    // Check that the number of hits is correct
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
      // Declare CoverPoints
      ccover("a", dut.io.outA)( // CoverPoint 1
        bin("lo10", 0 until 10)),
      ccover("b", dut.io.outB)( // CoverPoint 2
        bin("testLo10", 0 until 10)),
      // Declare timed cross points
      ccover("timedAB", dut.io.outA, dut.io.outB)(Exactly(3))(
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

    // Generate report
    a [IllegalStateException] should be thrownBy(cr.report)
  }

  def testAll[T <: TimedToyDUT](dut: T): Unit = {
    val cr = new CoverageReporter(dut)
    cr.register(
      // Declare CoverPoints with conditional bins
      ccover("accu", dut.io.outA)(
        bin("lo10even", 0 until 10, (x: Seq[BigInt]) => x.head % 2 == 0),
        bin("First100odd", 0 until 100, (x: Seq[BigInt]) => x.head % 2 != 0)),
      // Declare CoverPoints without conditional bins
      ccover("test", dut.io.outB)(
        bin("testLo10", 0 until 10)),
      // Declare CoverConditions
      ccover("aAndB",dut.io.outA, dut.io.outB)(
        bin("aeqb", condition = (x: Seq[BigInt]) => x.head == x(1)),
        bin("asuptobAtLeast100", condition = (x: Seq[BigInt]) => x.head > x(1), expectedHits = 100)),
      // Declare cross points
      ccover("accuAndTest", dut.io.outA, dut.io.outB)(
        cross("both1", Seq(1 to 9 by 2, 1 to 1))),
      // Declare timed cross points
      ccover("timedAB", dut.io.outA, dut.io.count)(Exactly(3))(
        cross("ExactlyBoth3", Seq(3 to 3, 3 to 3))),
      ccover("EventuallyTimedAB", dut.io.outB, dut.io.count)(Eventually(3))(
        cross("EventuallyBoth1", Seq(1 to 1, 1 to 1))),
      ccover("AlwaysTimedAB", dut.io.outC, dut.io.outA)(Always(3))(
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

    // Generate report
    val report = cr.report

    // Check that the number of hits is correct
    report.binNHits(1, "timedAB", "ExactlyBoth3") should be (1)
    report.binNHits(1, "EventuallyTimedAB", "EventuallyBoth1") should be (1)
    report.binNHits(1, "AlwaysTimedAB", "AlwaysBoth3") should be (1)
  }

  "Coverage" should "get the right amount of hits" in {
    test(new BasicToyDUT(32))(testGeneric(_))
  }

  "CoverageWithDefaultBins" should "pass" in {
    test(new BasicToyDUT(32))(testDefaults(_))
  }

  "CoverageWithCovConditions" should "pass" in {
    test(new BasicToyDUT(32))(testCovCond(_))
  }

  "SamplingWithMultipleGroups" should "pass" in {
    test(new BasicToyDUT(32))(testSingleGroupSample(_))
  }

  "CoverageWithDelays" should "pass" in {
    test(new TimedToyDUT(32))(testTimed(_))
  }

  "CoverageWithDelays" should "throw an exception" in {
    test(new TimedToyDUT(32))(testTimedFail(_))
  }

  "CoverageWithConditionalBins" should "pass" in {
    test(new BasicToyDUT(32))(testCond(_))
  }

  "CoverageWithEverything" should "pass" in {
    test(new TimedToyDUT(32))(testAll(_))
  }
}
