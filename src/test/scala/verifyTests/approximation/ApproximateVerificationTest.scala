package verifyTests.approximation

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import chiselverify.approximation._
import chiselverify.approximation.Metrics._
import verifyTests.ToyDUT.{ApproximateBasicToyDUT, ApproximateExactToyDUT}

class ApproximateVerificationTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  val Size: Int = 32

  // Extract the name of a port
  def portName(port: Data): String = port.pathName.split('.').last

  // Generate some random inputs to the basic DUT and sample its registered outputs
  def simpleRefTest(dut: ApproximateBasicToyDUT, er: ErrorReporter): Unit = {
    val rng = new scala.util.Random(42)
    val (as, bs) = (Seq.fill(1000){ BigInt(Size, rng) }, Seq.fill(1000){ BigInt(Size, rng) })
    as.zip(bs).foreach { case (a, b) =>
      dut.io.a.poke(a.U)
      dut.io.b.poke(b.U)
      dut.clock.step()
      er.sample(Map(dut.io.outAA -> a, dut.io.outBB -> b, dut.io.outAABB -> (a + b)))
    }
  }

  // Generate some random inputs to the DUT and sample its registered outputs
  def simpleTest(dut: ApproximateExactToyDUT, ers: ErrorReporter*): Unit = {
    val rng = new scala.util.Random(42)
    val (as, bs) = (Seq.fill(1000){ BigInt(Size, rng) }, Seq.fill(1000){ BigInt(Size, rng) })
    as.zip(bs).foreach { case (a, b) =>
      dut.io.a.poke(a.U)
      dut.io.b.poke(b.U)
      dut.clock.step()
      ers.foreach(_.sample())
    }
  }

  behavior of "Metric"

  it should "verify with values" in {
    val ed = ED(0)
    val rng = new scala.util.Random(42)
    val nums  = Seq.fill(1000) {
      val num = BigInt(Size, rng)
      (num, num)
    }
    val zeros = Seq.fill(1000) {0.0}
    ed.check(zeros.head) should be (true)
    zeros.map(ed.check(_)).forall(s => s) should be (true)
    ed.check(nums.head._1, nums.head._2) should be (true)
    ed.check(nums) should be (true)
  }

  behavior of "ErrorReporter"

  it should "generate without watchers" in {
    val er = new ErrorReporter()
  }

  it should "produce an empty report" in {
    val er = new ErrorReporter()
    er.report().split('\n') should contain ("Error reporter is empty!")
  }

  it should "verify without watchers" in {
    val er = new ErrorReporter()
    er.verify() should be (true)
  }

  behavior of "ErrorReporter with Trackers"

  it should "fail to generate with mismatched ports" in {
    the [IllegalArgumentException] thrownBy(
      test(new ApproximateExactToyDUT(Size)) { dut =>
        val er = new ErrorReporter(
          track(dut.io.outAA, dut.io.a) // direction mismatch
        )
      }
    ) should have message ("requirement failed: pairs of watched ports must have the same direction")
    the [IllegalArgumentException] thrownBy(
      test(new ApproximateExactToyDUT(Size)) { dut =>
        val er = new ErrorReporter(
          track(dut.io.outAA, dut.io.outAABB) // type/width mismatch
        )
      }
    ) should have message ("requirement failed: pairs of watched ports must have the same type")
  }

  it should "generate without metrics" in {
    test(new ApproximateBasicToyDUT(Size)) { dut =>
      val er = new ErrorReporter(
        track(dut.io.outAA)
      )
      er.report().split('\n') should contain (s"Tracker on port ${portName(dut.io.outAA)} has no metrics!")
    }
    test(new ApproximateExactToyDUT(Size)) { dut =>
      val er = new ErrorReporter(
        track(dut.io.outAA, dut.io.outA)
      )
      er.report().split('\n') should contain (s"Tracker on port ${portName(dut.io.outAA)} has no metrics!")
    }
  }

  it should "generate but fail to sample on missing reference value" in {
    the [AssertionError] thrownBy(
      test(new ApproximateBasicToyDUT(Size)) { dut =>
        val er = new ErrorReporter(
          track(dut.io.outAA)
        )
        dut.io.a.poke(14.U)
        dut.io.b.poke(28.U)
        dut.clock.step()
        er.sample()
      }
    ) should have message (s"watcher on port outAA needs a reference value but none was provided")
  }

  it should "generate and sample without metrics" in {
    test(new ApproximateBasicToyDUT(Size)) { dut =>
      val er = new ErrorReporter(
        track(dut.io.outAA)
      )
      simpleRefTest(dut, er)
      er.report().split('\n') should contain (s"Tracker on port ${portName(dut.io.outAA)} has no metrics!")
    }
    test(new ApproximateExactToyDUT(Size)) { dut =>
      val er = new ErrorReporter(
        track(dut.io.outAA, dut.io.outA, maxCacheSize=Int.MaxValue)
      )
      simpleTest(dut, er)
      er.report().split('\n') should contain (s"Tracker on port ${portName(dut.io.outAA)} has no metrics!")
    }
  }

  it should "generate and sample with metrics" in {
    test(new ApproximateBasicToyDUT(Size)) { dut =>
      val er = new ErrorReporter(
        track(dut.io.outAA),
        track(dut.io.outBB, ER(), ED())
      )
      simpleRefTest(dut, er)
      er.report().split('\n') should (
        contain (s"Tracker on port ${portName(dut.io.outAA)} has no metrics!")
        and
        contain (s"Tracker on port ${portName(dut.io.outBB)} has results:")
        and
        contain (s"- History-based ER(None) metric has value 0.0!")
        and
        contain (s"- Instantaneous ED(None) metric has mean 0.0 and maximum 0.0!")
      )
    }
    test(new ApproximateExactToyDUT(Size)) { dut =>
      val er = new ErrorReporter(
        track(dut.io.outAA, dut.io.outA, maxCacheSize=Int.MaxValue),
        track(dut.io.outBB, dut.io.outB, maxCacheSize=Int.MaxValue, ER(), ED())
      )
      simpleTest(dut, er)
      er.report().split('\n') should (
        contain (s"Tracker on port ${portName(dut.io.outAA)} has no metrics!")
        and
        contain (s"Tracker on port ${portName(dut.io.outBB)} has results:")
        and
        contain (s"- History-based ER(None) metric has value 0.0!")
        and
        contain (s"- Instantaneous ED(None) metric has mean 0.0 and maximum 0.0!")
      )
    }
  }

  behavior of "ErrorReporter with Constraints"

  it should "generate without metrics" in {
    test(new ApproximateBasicToyDUT(Size)) { dut =>
      val er = new ErrorReporter(
        track(dut.io.outAA),
        constrain(dut.io.outBB)
      )
      er.report().split('\n') should (
        contain (s"Tracker on port ${portName(dut.io.outAA)} has no metrics!")
        and
        contain (s"Constraint on port ${portName(dut.io.outBB)} has no metrics!")
      )
    }
    test(new ApproximateExactToyDUT(Size)) { dut =>
      val er = new ErrorReporter(
        track(dut.io.outAA, dut.io.outA, maxCacheSize=Int.MaxValue),
        constrain(dut.io.outBB, dut.io.outB, maxCacheSize=Int.MaxValue)
      )
      er.report().split('\n') should (
        contain (s"Tracker on port ${portName(dut.io.outAA)} has no metrics!")
        and
        contain (s"Constraint on port ${portName(dut.io.outBB)} has no metrics!")
      )
    }
  }

  it should "verify without metrics" in {
    test(new ApproximateBasicToyDUT(Size)) { dut =>
      val er = new ErrorReporter(
        track(dut.io.outAA),
        constrain(dut.io.outBB)
      )
      er.verify() should be (true)
    }
    test(new ApproximateExactToyDUT(Size)) { dut =>
      val er = new ErrorReporter(
        track(dut.io.outAA, dut.io.outA, maxCacheSize=Int.MaxValue),
        constrain(dut.io.outBB, dut.io.outB, maxCacheSize=Int.MaxValue)
      )
      er.verify() should be (true)
    }
  }

  it should "fail to generate with unconstrained metric" in {
    the [IllegalArgumentException] thrownBy(
      test(new ApproximateBasicToyDUT(Size)) { dut =>
        val er = new ErrorReporter(
          track(dut.io.outAA),
          constrain(dut.io.outBB, ED())
        )
      }
    ) should have message (s"requirement failed: cannot create ED(None) constraint without maximum value")
    the [IllegalArgumentException] thrownBy(
      test(new ApproximateExactToyDUT(Size)) { dut =>
        val er = new ErrorReporter(
          track(dut.io.outAA, dut.io.outA, maxCacheSize=Int.MaxValue),
          constrain(dut.io.outBB, dut.io.outB, maxCacheSize=Int.MaxValue, ED())
        )
      }
    ) should have message (s"requirement failed: cannot create ED(None) constraint without maximum value")
  }

  it should "fail to verify without samples" in {
    the [AssertionError] thrownBy(
      test(new ApproximateBasicToyDUT(Size)) { dut =>
        val er = new ErrorReporter(
          track(dut.io.outAA),
          constrain(dut.io.outBB, ED(0)),
          constrain(dut.io.outAABB, MRED(.1))
        )
        er.verify()
      }
    ) should have message ("assumption failed: cannot compute metrics without samples")
    the [AssertionError] thrownBy(
      test(new ApproximateExactToyDUT(Size)) { dut =>
        val er = new ErrorReporter(
          track(dut.io.outAA, dut.io.outA, maxCacheSize=Int.MaxValue),
          constrain(dut.io.outBB, dut.io.outB, maxCacheSize=Int.MaxValue, ED(0)),
          constrain(dut.io.outAABB, dut.io.outAB, maxCacheSize=Int.MaxValue, MRED(.1))
        )
        er.verify()
      }
    ) should have message ("assumption failed: cannot compute metrics without samples")
  }

  it should "verify with samples" in {
    test(new ApproximateBasicToyDUT(Size)) { dut =>
      val er = new ErrorReporter(
        track(dut.io.outAA),
        constrain(dut.io.outBB, dut.io.outBB, ED(0)), // redundant but also always exact
        constrain(dut.io.outAABB, MRED(.1))
      )
      simpleRefTest(dut, er)
      er.verify() should be (false)
      er.report().split('\n').map { ln =>
        ln.startsWith("- History-based MRED(Some(0.1)) metric is violated by")
      }.reduce(_ || _) should be (true)
    }
    test(new ApproximateExactToyDUT(Size)) { dut =>
      val er = new ErrorReporter(
        track(dut.io.outAA, dut.io.outA, maxCacheSize=Int.MaxValue),
        constrain(dut.io.outBB, dut.io.outB, maxCacheSize=Int.MaxValue, ED(0)),
        constrain(dut.io.outAABB, dut.io.outAB, maxCacheSize=Int.MaxValue, MRED(.1))
      )
      simpleTest(dut, er)
      er.verify() should be (false)
      er.report().split('\n').map { ln =>
        ln.startsWith("- History-based MRED(Some(0.1)) metric is violated by")
      }.reduce(_ || _) should be (true)
    }
  }

  it should "correctly compute metrics with caching" in {
    // Helper functions to extract the error value of a particular metric
    // from an error report, if present. If the metric is reported multiple
    // times, only its first error value is returned
    def extract[T](mtrc: T, report: String): Option[Double] = {
      val lines = report.split("\n").filter(_.contains(s"$mtrc"))
      if (lines.isEmpty) {
        None
      } else {
        val first = lines.head.split(" ")
        Some(mtrc match {
          case _: Instantaneous =>
            if (first.contains("violated")) first.dropRight(2).last.toDouble
            else first.last.dropRight(1).toDouble
          case _: HistoryBased =>
            first.last.dropRight(1).toDouble
          case _ => throw new IllegalArgumentException(s"cannot process argument $mtrc")
        })
      }
    }

    test(new ApproximateExactToyDUT(Size)) { dut =>
      val bMtrcs  = Seq(ED(0))
      val abMtrcs = Seq(RED(.25), MRED(.1), ER(.5), MSE(42.0))
      val nonCachedEr = new ErrorReporter(
        track(dut.io.outAA, dut.io.outA),
        constrain(dut.io.outBB, dut.io.outB, maxCacheSize=Int.MaxValue, bMtrcs:_*),
        constrain(dut.io.outAABB, dut.io.outAB, maxCacheSize=Int.MaxValue, abMtrcs:_*)
      )
      val cachedEr = new ErrorReporter(
        track(dut.io.outAA, dut.io.outA),
        constrain(dut.io.outBB, dut.io.outB, bMtrcs:_*),
        constrain(dut.io.outAABB, dut.io.outAB, abMtrcs:_*)
      )

      // Repeat the test some times to make sure the cache is used
      (0 until 10).foreach { _ => simpleTest(dut, nonCachedEr, cachedEr) }
      nonCachedEr.verify() should be (false)
      cachedEr   .verify() should be (false)
      val ncReport = nonCachedEr.report()
      val cReport  = cachedEr.report()
      (bMtrcs ++ abMtrcs).foreach { mtrc =>
        val ncRes = extract(mtrc, ncReport)
        val cRes  = extract(mtrc, cReport)
        ncRes shouldBe defined
        cRes  shouldBe defined
        cRes.get should (be >= ncRes.get*.95 and be <= ncRes.get*1.05)
      }
    }
  }

  it should "correctly compute maximum indices with caching" in {
    // Helper functions to extract the maximum error index of a particular metric
    // from an error report, if present. If the metric is reported multiple
    // times, only its first index is returned
    def extract[T](mtrc: T, report: String): Option[Int] = mtrc match {
      case _: Instantaneous =>
        val lines = report.split("\n").filter(_.contains(s"$mtrc"))
        if (lines.isEmpty) {
          None
        } else {
          val first = lines.head.split(" ")
          Some(first.last.drop(1).dropRight(2).toInt)
        }
      case _: HistoryBased => None
      case _ => throw new IllegalArgumentException(s"cannot process argument $mtrc")
    }

    test(new ApproximateExactToyDUT(Size)) { dut =>
      val cacheSize = 10
      val abMtrc = ED(0)
      val nonCachedEr = new ErrorReporter(
        constrain(dut.io.outAABB, dut.io.outAB, maxCacheSize=Int.MaxValue, abMtrc)
      )
      val cachedEr = new ErrorReporter(
        constrain(dut.io.outAABB, dut.io.outAB, maxCacheSize=cacheSize, abMtrc)
      )
      val ers = Seq(nonCachedEr, cachedEr)

      // Deliberately insert the maximum error after a round of caching
      dut.io.a.poke(0.U)
      dut.io.b.poke(0.U)
      (0 until (cacheSize + cacheSize / 2 - 1)).foreach { _ =>
        dut.clock.step()
        ers.foreach(_.sample())
      }
      dut.io.a.poke(1.U)
      dut.io.b.poke(1.U)
      dut.clock.step()
      ers.foreach(_.sample())
      nonCachedEr.verify() should be (false)
      cachedEr.verify()    should be (false)
      val ncRes = extract(abMtrc, nonCachedEr.report())
      val cRes  = extract(abMtrc, cachedEr.report())
      ncRes shouldBe defined
      cRes  shouldBe defined
      cRes.get should equal (ncRes.get)

      // Now do the same but after a round of cache collapsing
      dut.io.a.poke(0.U)
      dut.io.b.poke(0.U)
      (0 until (cacheSize * (cacheSize - 1))).foreach { _ =>
        dut.clock.step()
        ers.foreach(_.sample())
      }
      dut.io.a.poke(2.U)
      dut.io.b.poke(2.U)
      dut.clock.step()
      ers.foreach(_.sample())
      nonCachedEr.verify() should be (false)
      cachedEr.verify()    should be (false)
      val fNcRes = extract(abMtrc, nonCachedEr.report())
      val fCRes  = extract(abMtrc, cachedEr.report())
      fNcRes shouldBe defined
      fCRes  shouldBe defined
      fCRes.get should equal (fNcRes.get)

      // And again after another round of cache collapsing and sample collapsing
      dut.io.a.poke(0.U)
      dut.io.b.poke(0.U)
      (0 until (cacheSize * (cacheSize + 1))).foreach { _ =>
        dut.clock.step()
        ers.foreach(_.sample())
      }
      dut.io.a.poke(4.U)
      dut.io.b.poke(4.U)
      dut.clock.step()
      ers.foreach(_.sample())
      nonCachedEr.verify() should be (false)
      cachedEr.verify()    should be (false)
      val cFNcRes = extract(abMtrc, nonCachedEr.report())
      val cFCRes  = extract(abMtrc, cachedEr.report())
      cFNcRes shouldBe defined
      cFCRes  shouldBe defined
      cFCRes.get should equal (cFNcRes.get)
    }
  }
}
