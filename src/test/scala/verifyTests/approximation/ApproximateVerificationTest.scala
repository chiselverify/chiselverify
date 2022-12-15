package verifyTests.approximation

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import chiselverify.approximation._
import chiselverify.approximation.Metrics._
import verifyTests.ToyDUT.ApproximateExactToyDUT

class ApproximateVerificationTest extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  val Size: Int = 32

  // Extract the name of a port
  def portName(port: Data): String = port.pathName.split('.').last
  
  // Generate some random inputs to the DUT and sample its registered outputs
  def simpleTest(dut: ApproximateExactToyDUT, er: ErrorReporter): Unit = {
    val rng = new scala.util.Random(42)
    val (as, bs) = (Seq.fill(1000){ BigInt(Size, rng) }, Seq.fill(1000){ BigInt(Size, rng) })
    as.zip(bs).foreach { case (a, b) =>
      dut.io.a.poke(a.U)
      dut.io.b.poke(b.U)
      dut.clock.step()
      er.sample()
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
    test(new ApproximateExactToyDUT(Size)) { dut =>
      val er = new ErrorReporter(
        track(dut.io.outAA, dut.io.outA)
      )
      er.report().split('\n') should contain (s"Tracker on ports ${portName(dut.io.outAA)} and ${portName(dut.io.outA)} has no metrics!")
    }
  }

  it should "generate and sample without metrics" in {
    test(new ApproximateExactToyDUT(Size)) { dut =>
      val er = new ErrorReporter(
        track(dut.io.outAA, dut.io.outA)
      )
      simpleTest(dut, er)
      er.report().split('\n') should contain (s"Tracker on ports ${portName(dut.io.outAA)} and ${portName(dut.io.outA)} has no metrics!")
    }
  }

  it should "generate and sample with metrics" in {
    test(new ApproximateExactToyDUT(Size)) { dut =>
      val er = new ErrorReporter(
        track(dut.io.outAA, dut.io.outA),
        track(dut.io.outBB, dut.io.outB, ER(), ED())
      )
      simpleTest(dut, er)
      er.report().split('\n') should (
        contain (s"Tracker on ports ${portName(dut.io.outAA)} and ${portName(dut.io.outA)} has no metrics!")
        and
        contain (s"Tracker on ports ${portName(dut.io.outBB)} and ${portName(dut.io.outB)} has results:")
        and
        contain (s"- History-based ER(None) metric has value 0.000!")
        and
        contain (s"- Instantaneous ED(None) metric has mean 0.000 and maximum 0.000!")
      )
    }
  }

  behavior of "ErrorReporter with Constraints"

  it should "generate without metrics" in {
    test(new ApproximateExactToyDUT(Size)) { dut =>
      val er = new ErrorReporter(
        track(dut.io.outAA, dut.io.outA),
        constrain(dut.io.outBB, dut.io.outB)
      )
      er.report().split('\n') should (
        contain (s"Tracker on ports ${portName(dut.io.outAA)} and ${portName(dut.io.outA)} has no metrics!")
        and
        contain (s"Constraint on ports ${portName(dut.io.outBB)} and ${portName(dut.io.outB)} has no metrics!")
      )
    }
  }

  it should "verify without metrics" in {
    test(new ApproximateExactToyDUT(Size)) { dut =>
      val er = new ErrorReporter(
        track(dut.io.outAA, dut.io.outA),
        constrain(dut.io.outBB, dut.io.outB)
      )
      er.verify() should be (true)
    }
  }

  it should "fail to generate with unconstrained metric" in {
    the [IllegalArgumentException] thrownBy(
      test(new ApproximateExactToyDUT(Size)) { dut =>
        val er = new ErrorReporter(
          track(dut.io.outAA, dut.io.outA),
          constrain(dut.io.outBB, dut.io.outB, ED())
        )
      }
    ) should have message (s"requirement failed: cannot create ED(None) constraint without maximum value")
  }

  it should "fail to verify without samples" in {
    the [AssertionError] thrownBy(
      test(new ApproximateExactToyDUT(Size)) { dut =>
        val er = new ErrorReporter(
          track(dut.io.outAA, dut.io.outA),
          constrain(dut.io.outBB, dut.io.outB, ED(0)),
          constrain(dut.io.outAABB, dut.io.outAB, MRED(.1))
        )
        er.verify()
      }
    ) should have message ("assumption failed: cannot compute metrics without samples")
  }

  it should "verify with samples" in {
    test(new ApproximateExactToyDUT(Size)) { dut =>
      val er = new ErrorReporter(
        track(dut.io.outAA, dut.io.outA),
        constrain(dut.io.outBB, dut.io.outB, ED(0)),
        constrain(dut.io.outAABB, dut.io.outAB, MRED(.1))
      )
      simpleTest(dut, er)
      er.verify() should be (false)
      er.report().split('\n').map { ln =>
        ln.startsWith("- History-based MRED(Some(0.1)) metric is violated by")
      }.reduce(_ || _) should be (true)
    }
  }
}
