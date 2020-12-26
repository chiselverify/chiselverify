package verifyTests.coverage

import chisel3._
import chiseltest._
import chiselverify.coverage._
import chiselverify.timing._
import verifyTests.ToyDUT._
import org.scalatest._

class FunctionalCoverageTest extends FlatSpec with ChiselScalatestTester with Matchers {

  def toUInt(i: Int): UInt = (BigInt(i) & 0x00ffffffffL).asUInt(32.W)

  /** Tests functional coverage in a generic use case
    */
  def testGeneric[T <: BasicToyDUT](dut: T): Unit = {

    val cr = new CoverageReporter(dut)
    cr.register(
      //Declare CoverPoints
      CoverPoint(dut.io.outA, "a")( //CoverPoint 1
        Bins("lo10", 0 until 10) :: Bins("First100", 0 until 100) :: Nil
      ) ::
        CoverPoint(dut.io.outB, "b")( //CoverPoint 2
          Bins("testLo10", 0 until 10) :: Nil
        ) ::
        Nil,
      //Declare cross points
      CrossPoint("aANDb", "a", "b")(CrossBin("both1", 1 to 1, 1 to 1) :: Nil) ::
        Nil
    )

    /** Basic test to see if we get the right amount of hits
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
    report.binNHits(1, "a", "lo10") should be(BigInt(10))
    report.binNHits(1, "a", "First100") should be(BigInt(50))
    report.binNHits(1, "b", "testLo10") should be(BigInt(5))
    report.binNHits(1, "aANDb", "both1") should be(BigInt(1))
  }

  /** Tests the default bins in functional coverage points
    */
  def testDefaults[T <: BasicToyDUT](dut: T): Unit = {
    val cr = new CoverageReporter(dut)
    cr.register(
      //Declare CoverPoints with default bins
      CoverPoint(dut.io.outA, "a")() ::
        CoverPoint(dut.io.outB, "b")() ::
        CoverPoint(dut.io.outAB, "aplusb")() :: Nil
    )

    /** Basic test to see if we get the right amount of hits
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
    report.binNHits(1, "a", "default") should be(BigInt(50))
    report.binNHits(1, "b", "default") should be(BigInt(5))
    report.binNHits(1, "aplusb", "default") should be(BigInt(50))
  }

  /** Tests the timed functional coverage relations
    */
  def testTimed[T <: TimedToyDUT](dut: T): Unit = {
    val cr = new CoverageReporter(dut)
    cr.register(
      //Declare CoverPoints
      CoverPoint(dut.io.outA, "a")( //CoverPoint 1
        Bins("lo10", 0 until 10) :: Nil
      ) ::
        CoverPoint(dut.io.count, "count")( //CoverPoint 2
          Bins("testLo10", 0 until 10) :: Nil
        ) ::
        CoverPoint(dut.io.outB, "b")(
          Bins("test10", 0 until 10) :: Nil
        ) ::
        CoverPoint(dut.io.outC, "c")(
          Bins("test5", 0 until 5) :: Nil
        ) :: Nil,
      //Declare timed cross points
      TimedCross("timedAB", "a", "count", Exactly(3))(CrossBin("both3", 3 to 3, 3 to 3) :: Nil) ::
        TimedCross("EventuallyTimedAB", "b", "count", Eventually(3))(CrossBin("both1", 1 to 1, 1 to 1) :: Nil) ::
        TimedCross("AlwaysTimedAB", "c", "a", Always(3))(
          CrossBin("both3", 3 to 3, 3 to 3) :: Nil
        ) :: Nil
    )

    /** Basic test to see if we get the right amount of hits
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

    report.binNHits(1, "timedAB", "both3") should be(1)
    report.binNHits(1, "EventuallyTimedAB", "both1") should be(1)
    report.binNHits(1, "AlwaysTimedAB", "both3") should be(1)
  }

  /** Tests that the timed coverage only works when stepping with the reporter
    */
  def testTimedFail[T <: TimedToyDUT](dut: T): Unit = {
    val cr = new CoverageReporter(dut)
    cr.register(
      //Declare CoverPoints
      CoverPoint(dut.io.outA, "a")( //CoverPoint 1
        Bins("lo10", 0 until 10) :: Nil
      ) ::
        CoverPoint(dut.io.outB, "b")( //CoverPoint 2
          Bins("testLo10", 0 until 10) :: Nil
        ) ::
        Nil,
      //Declare timed cross points
      TimedCross("timedAB", "a", "b", Exactly(3))(CrossBin("both1", 3 to 3, 3 to 3) :: Nil) ::
        Nil
    )

    /** Basic test to see if we get the right exception
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

  "Coverage" should "get the right amount of hits" in {
    test(new BasicToyDUT(32)) { dut => testGeneric(dut) }
  }

  "CoverageWithDefaultBins" should "pass" in {
    test(new BasicToyDUT(32)) { dut => testDefaults(dut) }
  }

  "CoverageWithDelays" should "pass" in {
    test(new TimedToyDUT(32)) { dut => testTimed(dut) }
  }

  "CoverageWithDelays" should "throw an exception" in {
    test(new TimedToyDUT(32)) { dut => testTimedFail(dut) }
  }
}
