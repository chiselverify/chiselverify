package verifyTests.assertions

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import chiselverify.assertions.AssertTimed._
import chiselverify.assertions.{AssertTimed, ExpectTimed}
import chiselverify.timing._
import chiselverify.timing.TimedOp.{Equals, Gt, GtEq, Lt, LtEq, dataToID}
import verifyTests.ToyDUT.AssertionsToyDUT

class TimedAssertionTests extends AnyFlatSpec with ChiselScalatestTester {
  def toUInt(i: Int): UInt = (BigInt(i) & 0x00ffffffffL).asUInt(32.W)

  abstract class DT
  object Evt extends DT
  object Alw extends DT
  object Nvr extends DT
  object Exct extends DT

  /**
    * Tests timed assertions in a generic use case
    */
  def testGeneric[T <: AssertionsToyDUT](dut: T, et: DT): Unit = {
    /**
      * Basic test to see if we get the right amount of hits
      */
    def testAlways(): Unit = {
      dut.io.a.poke(10.U)
      dut.io.b.poke(10.U)
      dut.clock.step(1)
      AssertTimed(dut, dut.io.aEqb.peek().litValue == 1, "aEqb timing is wrong")(Always(9)).join()
    }

    def testEventually(): Unit = {
      dut.io.a.poke(10.U)
      dut.io.b.poke(10.U)
      AssertTimed(dut, dut.io.aEvEqC.peek().litValue == 1, "a eventually isn't c")(Eventually(11)).join()
    }

    def testExactly(): Unit = {
      dut.io.a.poke(7.U)
      dut.clock.step(1)
      ExpectTimed(dut, dut.io.aEvEqC, 1.U, "aEqb expected timing is wrong")(Exactly(6)).join()
    }

    def testNever(): Unit = {
      dut.io.a.poke(10.U)
      dut.io.b.poke(20.U)
      dut.clock.step(1)
      ExpectTimed(dut, dut.io.aNevEqb, 0.U, "a is equal to b at some point")(Never(10)).join()
    }

    et match {
      case Alw => testAlways()
      case Evt => testEventually()
      case Exct => testExactly()
      case Nvr => testNever()
    }
  }

  def testGenericEqualsOp[T <: AssertionsToyDUT](dut: T, et : DT): Unit = {
    /**
      * Basic test to see if we get the right amount of hits
      */
    def testAlways(): Unit = {
      dut.io.a.poke(10.U)
      dut.io.b.poke(10.U)
      dut.clock.step(1)
      AssertTimed(dut, Equals(dut.io.aEqb, dut.io.isOne), "aEqb timing is wrong")(Always(9)).join()
    }

    def testEventually(): Unit = {
      dut.io.a.poke(10.U)
      dut.io.b.poke(10.U)
      AssertTimed(dut, Equals(dut.io.outA, dut.io.outC), "a eventually isn't c")(Eventually(11)).join()
    }

    def testExactly(): Unit = {
      dut.io.a.poke(6.U)
      AssertTimed(dut, Equals(dut.io.outA, dut.io.outC), "aEqb expected timing is wrong")(Exactly(6)).join()
    }

    def testNever(): Unit = {
      dut.io.a.poke(10.U)
      dut.io.b.poke(20.U)
      dut.clock.step(1)
      AssertTimed(dut, Equals(dut.io.outA, dut.io.outB), "a is equal to b at some point")(Never(10)).join()
    }

    et match {
      case Alw => testAlways()
      case Evt => testEventually()
      case Exct => testExactly()
      case Nvr => testNever()
    }
  }

  def testGenericGtOp[T <: AssertionsToyDUT](dut: T, et : DT): Unit = {
    /**
      * Basic test to see if we get the right amount of hits
      */
    def testAlways(): Unit = {
      dut.io.a.poke(20.U)
      dut.io.b.poke(10.U)
      dut.clock.step(1)
      AssertTimed(dut, Gt(dut.io.outA, dut.io.outB), "a isn't always superior to one")(Always(9)).join()
    }

    def testEventually(): Unit = {
      dut.io.a.poke(4.U)
      dut.io.b.poke(2.U)
      dut.clock.step()
      AssertTimed(dut, Gt(dut.io.outB, dut.io.outCNotSupB), "c isn't eventually greater than b")(Eventually(4)).join()
    }

    def testExactly(): Unit = {
      dut.io.a.poke(6.U)
      dut.io.b.poke(5.U)
      dut.clock.step(2)
      AssertTimed(dut, Gt(dut.io.outB, dut.io.outCNotSupB), "c isn't greater than b after 7 cycles")(Exactly(7)).join()
    }

    def testNever(): Unit = {
      dut.io.a.poke(10.U)
      dut.io.b.poke(20.U)
      dut.clock.step(1)
      AssertTimed(dut, Gt(dut.io.outC, dut.io.outA), "a is equal to b at some point")(Never(10)).join()
    }

    et match {
      case Alw => testAlways()
      case Evt => testEventually()
      case Exct => testExactly()
      case Nvr => testNever()
    }
  }

  def testGenericLtOp[T <: AssertionsToyDUT](dut: T, et : DT): Unit = {
    /**
      * Basic test to see if we get the right amount of hits
      */
    def testAlways(): Unit = {
      dut.io.a.poke(10.U)
      dut.io.b.poke(20.U)
      dut.clock.step(1)
      AssertTimed(dut, Lt(dut.io.outA, dut.io.outB), "a isn't always less than one")(Always(9)).join()
    }

    def testEventually(): Unit = {
      dut.io.a.poke(4.U)
      dut.io.b.poke(2.U)
      dut.clock.step()
      AssertTimed(dut, Lt(dut.io.outB, dut.io.outCSupB), "c isn't eventually less than b")(Eventually(4)).join()
    }

    def testExactly(): Unit = {
      dut.io.a.poke(6.U)
      dut.io.b.poke(5.U)
      dut.clock.step(2)
      AssertTimed(dut, Lt(dut.io.outB, dut.io.outCSupB), "c isn't less than b after 7 cycles")(Exactly(7)).join()
    }

    def testNever(): Unit = {
      dut.io.a.poke(10.U)
      dut.io.b.poke(20.U)
      dut.clock.step(1)
      AssertTimed(dut, Lt(dut.io.outB, dut.io.outCNotSupB), "a is never less than c at any point")(Never(10)).join()
    }

    et match {
      case Alw => testAlways()
      case Evt => testEventually()
      case Exct => testExactly()
      case Nvr => testNever()
    }
  }

  def testGenericLtEqOp[T <: AssertionsToyDUT](dut: T, et : DT): Unit = {
    /**
      * Basic test to see if we get the right amount of hits
      */
    def testAlways(): Unit = {
      dut.io.a.poke(10.U)
      dut.io.b.poke(20.U)
      dut.clock.step(1)
      AssertTimed(dut, LtEq(dut.io.outA, dut.io.outB), "a isn't always less than one")(Always(9)).join()
    }

    def testEventually(): Unit = {
      dut.io.a.poke(4.U)
      dut.io.b.poke(2.U)
      dut.clock.step()
      AssertTimed(dut, LtEq(dut.io.outB, dut.io.outCSupB), "c isn't eventually less than b")(Eventually(4)).join()
    }

    def testExactly(): Unit = {
      dut.io.a.poke(6.U)
      dut.io.b.poke(5.U)
      dut.clock.step(2)
      AssertTimed(dut, LtEq(dut.io.outB, dut.io.outCSupB), "c isn't less than b after 7 cycles")(Exactly(7)).join()
    }

    def testNever(): Unit = {
      dut.io.a.poke(10.U)
      dut.io.b.poke(11.U)
      dut.clock.step(1)
      AssertTimed(dut, LtEq(dut.io.outB, dut.io.outC), "a is never less than c at any point")(Never(10)).join()
    }

    et match {
      case Alw => testAlways()
      case Evt => testEventually()
      case Exct => testExactly()
      case Nvr => testNever()
    }
  }

  def testGenericGtEqOp[T <: AssertionsToyDUT](dut: T, et : DT): Unit = {
    /**
      * Basic test to see if we get the right amount of hits
      */
    def testAlways(): Unit = {
      dut.io.a.poke(10.U)
      dut.io.b.poke(20.U)
      dut.clock.step(1)
      AssertTimed(dut, GtEq(dut.io.outB, dut.io.outA), "a isn't always less than one")(Always(9)).join()
    }

    def testEventually(): Unit = {
      dut.io.a.poke(4.U)
      dut.io.b.poke(2.U)
      dut.clock.step()
      AssertTimed(dut, GtEq(dut.io.outB, dut.io.outCNotSupB), "c isn't eventually less than b")(Eventually(4)).join()
    }

    def testExactly(): Unit = {
      dut.io.a.poke(6.U)
      dut.io.b.poke(5.U)
      dut.clock.step(2)
      AssertTimed(dut, GtEq(dut.io.outB, dut.io.outCNotSupB), "c isn't less than b after 7 cycles")(Exactly(7)).join()
    }

    def testNever(): Unit = {
      dut.io.a.poke(10.U)
      dut.io.b.poke(0.U)
      dut.clock.step(1)
      AssertTimed(dut, GtEq(dut.io.outB, dut.io.outC), "a is never less than c at any point")(Never(10)).join()
    }

    et match {
      case Alw => testAlways()
      case Evt => testEventually()
      case Exct => testExactly()
      case Nvr => testNever()
    }
  }

  "Timed Assertions Alw" should "pass" in {
    test(new AssertionsToyDUT(32))(testGeneric(_, Alw))
  }
  "Timed Assertions Eventually" should "pass" in {
    test(new AssertionsToyDUT(32))(testGeneric(_, Evt))
  }
  "Timed Assertions Exactly" should "pass" in {
    test(new AssertionsToyDUT(32))(testGeneric(_, Exct))
  }
  "Timed Assertions Never" should "pass" in {
    test(new AssertionsToyDUT(32))(testGeneric(_, Nvr))
  }

  "Timed Assertions Alw with Equals Op" should "pass" in {
    test(new AssertionsToyDUT(32))(testGenericEqualsOp(_, Alw))
  }
  "Timed Assertions Eventually with Equals Op" should "pass" in {
    test(new AssertionsToyDUT(32))(testGenericEqualsOp(_, Evt))
  }
  "Timed Assertions Exct with Equals Op" should "pass" in {
    test(new AssertionsToyDUT(32))(testGenericEqualsOp(_, Exct))
  }
  "Timed Assertions Nvr with Equals Op" should "pass" in {
    test(new AssertionsToyDUT(32))(testGenericEqualsOp(_, Nvr))
  }

  "Timed Assertions Alw with GreaterThan Op" should "pass" in {
    test(new AssertionsToyDUT(32))(testGenericGtOp(_, Alw))
  }
  "Timed Assertions Evt with GreaterThan Op" should "pass" in {
    test(new AssertionsToyDUT(32))(testGenericGtOp(_, Evt))
  }
  "Timed Assertions Exct with GreaterThan Op" should "pass" in {
    test(new AssertionsToyDUT(32))(testGenericGtOp(_, Exct))
  }
  "Timed Assertions Nvr with GreaterThan Op" should "pass" in {
    test(new AssertionsToyDUT(32))(testGenericGtOp(_, Nvr))
  }

  "Timed Assertions Alw with GreaterThan or Equal to Op" should "pass" in {
    test(new AssertionsToyDUT(32))(testGenericGtEqOp(_, Alw))
  }
  "Timed Assertions Evt with GreaterThan or Equal to Op" should "pass" in {
    test(new AssertionsToyDUT(32))(testGenericGtEqOp(_, Evt))
  }
  "Timed Assertions Exct with GreaterThan or Equal to Op" should "pass" in {
    test(new AssertionsToyDUT(32))(testGenericGtEqOp(_, Exct))
  }
  "Timed Assertions Nvr with GreaterThan or Equal to Op" should "pass" in {
    test(new AssertionsToyDUT(32))(testGenericGtEqOp(_, Nvr))
  }

  "Timed Assertions Alw with LessThan Op" should "pass" in {
    test(new AssertionsToyDUT(32))(testGenericLtOp(_, Alw))
  }
  "Timed Assertions Evt with LessThan Op" should "pass" in {
    test(new AssertionsToyDUT(32))(testGenericLtOp(_, Evt))
  }
  "Timed Assertions Exct with LessThan Op" should "pass" in {
    test(new AssertionsToyDUT(32))(testGenericLtOp(_, Exct))
  }
  "Timed Assertions Nvr with LessThan Op" should "pass" in {
    test(new AssertionsToyDUT(32))(testGenericLtOp(_, Nvr))
  }

  "Timed Assertions Alw with LessThan or Equal to Op" should "pass" in {
    test(new AssertionsToyDUT(32))(testGenericLtEqOp(_, Alw))
  }
  "Timed Assertions Evt with LessThan or Equal to Op" should "pass" in {
    test(new AssertionsToyDUT(32))(testGenericLtEqOp(_, Evt))
  }
  "Timed Assertions Exct with LessThan or Equal to Op" should "pass" in {
    test(new AssertionsToyDUT(32))(testGenericLtEqOp(_, Exct))
  }
  "Timed Assertions Nvr with LessThan or Equal to Op" should "pass" in {
    test(new AssertionsToyDUT(32))(testGenericLtEqOp(_, Nvr))
  }
}
