package verifyTests.crv.backends.jacop.bundle

import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import chiselverify.crv.backends.jacop.{JaCoPConstraint, IfCon}
import chiselverify.crv.backends.jacop.experimental.RandBundle

class RandomBundleTests extends AnyFlatSpec with ChiselScalatestTester with Matchers {
  behavior of "Random Bundles"

  class A extends Bundle with RandBundle {
    val x = UInt(8.W)
    val y = UInt(8.W)
    val greaterThen: JaCoPConstraint = x #> y
    val lessThen:    JaCoPConstraint = x #< y
    lessThen.disable()
  }

  class F extends Bundle with RandBundle {
    val x = UInt(8.W)
    val y = UInt(8.W)
    IfCon(x #= 8) {
      y #= 9
    }
    val c = x #= 8
    val o = y #\= 9
    o.disable()
  }

  it should "Randomize with conditional constraints" in {
    val z = new F()
    val o = z.randomBundle()
    o.x.litValue should equal (8)
    o.y.litValue should equal (9)
    z.c.disable()
    z.o.enable()
    val t = z.randomBundle()
    t.y.litValue should not equal (9)
  }

  it should "Randomize Bundles and enable disable constraints" in {
    val z = new A()
    val o = z.randomBundle()
    (o.x.litValue > o.y.litValue) should be (true)
    z.greaterThen.disable()
    z.lessThen.enable()
    val t = z.randomBundle()
    (t.x.litValue < t.y.litValue) should be (true)
  }
}
