package verifyTests.crv.backends.jacop.bundle

import chisel3.tester.ChiselScalatestTester
import chisel3.{Bundle, UInt, _}
import chiselverify.crv.backends.jacop.{Constraint, IfCon}
import chiselverify.crv.backends.jacop.experimental.RandBundle
import org.scalatest.{FlatSpec, Matchers}

class RandomBunleTests extends FlatSpec with ChiselScalatestTester with Matchers {
  behavior.of("Random Bundles")

  class A extends Bundle with RandBundle {
    val x = UInt(8.W)
    val y = UInt(8.W)
    val greaterThen: Constraint = x #> y
    val lessThen:    Constraint = x #< y
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
    assert(o.x.litValue() == 8)
    assert(o.y.litValue() == 9)
    z.c.disable()
    z.o.enable()
    val t = z.randomBundle()
    assert(t.y.litValue() != 9)
  }

  it should "Randomize Bundles and enable disable constraints" in {
    val z = new A()
    val o = z.randomBundle()
    assert(o.x.litValue() > o.y.litValue())
    z.greaterThen.disable()
    z.lessThen.enable()
    val t = z.randomBundle()
    assert(t.x.litValue() < t.y.litValue())
  }
}
