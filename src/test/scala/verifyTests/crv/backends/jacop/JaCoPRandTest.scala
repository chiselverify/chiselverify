package verifyTests.crv.backends.jacop

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import chiselverify.crv
import chiselverify.crv.backends.jacop
import chiselverify.crv.{RangeBinder, ValueBinder, backends}
import chiselverify.crv.backends.jacop.{JaCoPConstraint, JaCoPConstraintGroup, Cyclic, IfCon, Model, Rand, RandCVar, RandObj, RandVar, Randc, rand}

class JaCoPRandTest extends AnyFlatSpec with Matchers {
  it should "be able to declare a random variable and and a constraint" in {
    class Packet extends RandObj {
      currentModel = new Model(3)

      val min = 1
      val max = 100
      var size = rand(min, max)
      var len = rand(min, max)
      len >= size
      val x: crv.CRVConstraint = len <= size
      val y: crv.CRVConstraint = len > 4
      val payload = Array.tabulate(11)(_ => rand(1, 100))
      payload(0) = (len + size)
    }

    val myPacket = new Packet
    myPacket.x.disable()
    for (_ <- 0 until 3) {
      myPacket.randomize should be (true)
      (myPacket.len.value() >= myPacket.size.value()) should be (true)
    }

    myPacket.y.disable()
    myPacket.x.enable()
    myPacket.randomize should be (true)
    (myPacket.len.value() <= myPacket.size.value()) should be (true)

    myPacket.y.enable()
    myPacket.x.disable()
    myPacket.randomize should be (true)
    (myPacket.len.value() >= myPacket.size.value()) should be (true)

    myPacket.y.disable()
    myPacket.x.enable()
    myPacket.randomize should be (true)
    (myPacket.len.value() <= myPacket.size.value()) should be (true)

    myPacket.y.enable()
    myPacket.x.disable()
    myPacket.randomize should be (true)
    (myPacket.len.value() >= myPacket.size.value()) should be (true)
  }

  it should "be able to enable disable constraint 2" in {
    class Packet extends RandObj {
      currentModel = new Model(3)
      val min = 1
      val max = 100
      val size = rand(min, max)
      val len = rand(min, max)
      val greater: JaCoPConstraint = size > len
      val smaller: JaCoPConstraint = size < len
    }
    val myPacket = new Packet
    myPacket.greater.disable()
    myPacket.randomize should be (true)
    (myPacket.size.value() < myPacket.len.value()) should be (true)
    myPacket.smaller.disable()
    myPacket.greater.enable()
    myPacket.randomize should be (true)
    (myPacket.size.value() > myPacket.len.value()) should be (true)
  }

  it should "be able to add two Rand var" in {
    class Packet extends RandObj {
      currentModel = new Model(3)
      val min = 1
      val max = 100
      val size = rand(min, max)
      val len = rand(min, max)
      val payload = Array.tabulate(11)(i => rand(1, 100))
      payload(0) = (len + size)
      payload(1) = (len + 4)
    }
    val myPacket = new Packet
    myPacket.randomize should be (true)
    myPacket.payload(0).value() should equal (myPacket.len.value + myPacket.size.value())
    myPacket.payload(1).value() should equal (myPacket.len.value + 4)
  }

  it should "be able to subtract two Rand var" in {
    class Packet extends RandObj {
      currentModel = new Model(3)
      val min = 1
      val max = 100
      val size = rand(min, max)
      val len = rand(min, max)
      val payload = Array.tabulate(11)(i => rand(1, 100))
      payload(0) = (len - size)
    }
    val myPacket = new Packet
    myPacket.randomize should be (true)
    myPacket.payload(0).value() should equal (myPacket.len.value - myPacket.size.value())
  }

  it should "be able to multiply two Rand var" in {
    class Packet extends RandObj {
      currentModel = new Model(5)
      val min = 1
      val max = 100
      val size: RandVar = rand(min, max)
      val len: RandVar = rand(min, max)
      val payload: Array[RandVar] = Array.tabulate(11)(i => rand(1, 100))
      payload(0) = (len * size)
      payload(1) = (len * 4)
    }
    val myPacket = new Packet
    myPacket.randomize should be (true)
    myPacket.payload(0).value() should equal (myPacket.len.value * myPacket.size.value())
    myPacket.payload(1).value() should equal (myPacket.len.value * 4)
  }

  it should "be able to divide two Rand var" in {
    class Packet extends RandObj {
      currentModel = new Model(3)
      val min = 1
      val max = 100
      val size = rand(min, max)
      val len = rand(min, max)
      val payload = Array.tabulate(11)(i => rand(1, 100))
      payload(0) = len.div(size)
      payload(1) = len.div(4)
    }
    val myPacket = new Packet
    myPacket.randomize should be (true)
    myPacket.payload(0).value() should equal (myPacket.len.value / myPacket.size.value())
    myPacket.payload(1).value() should equal (myPacket.len.value / 4)
  }

  it should "be able to constrain the remainder of Rand var" in {
    class Packet extends RandObj {
      currentModel = new Model(6)
      val min = 1
      val max = 100
      val size: RandVar = rand(min, max)
      val len: RandVar = rand(min, max)
      val payload: Array[RandVar] = Array.tabulate(11)(i => rand(1, 100))
      payload(0) = len.mod(size)
      payload(1) = len.mod(4)
    }
    val myPacket = new Packet
    myPacket.randomize should be (true)
    myPacket.payload(0).value() should equal (myPacket.len.value % myPacket.size.value())
    myPacket.payload(1).value() should equal (myPacket.len.value % 4)
  }

  it should "be able to constrain less than or equal to Rand var" in {
    class Packet extends RandObj {
      currentModel = new Model(6)
      val min = 1
      val max = 100
      val len: RandVar = rand(min, max)
      val payload: Array[RandVar] = Array.tabulate(11)(i => rand(1, 100))
      payload(0) <= len
      payload(1) <= 3
    }
    val myPacket = new Packet
    myPacket.randomize should be (true)
    (myPacket.payload(0).value() <= myPacket.len.value()) should be (true)
    (myPacket.payload(1).value() <= 3) should be (true)
  }

  it should "be able to constrain less than Rand var" in {
    class Packet extends RandObj {
      currentModel = new Model(6)
      val min = 1
      val max = 100
      val len: RandVar = rand(min, max)
      val payload: Array[RandVar] = Array.tabulate(11)(i => rand(1, 100))
      payload(0) < len
      payload(1) < 3
    }
    val myPacket = new Packet
    myPacket.randomize should be (true)
    (myPacket.payload(0).value() < myPacket.len.value()) should be (true)
    (myPacket.payload(1).value() < 3) should be (true)
  }

  it should "be able to constrain greater than or equal to Rand var" in {
    class Packet extends RandObj {
      currentModel = new Model(6)
      val min = 1
      val max = 10
      val len: RandVar = rand(min, max)
      val payload: Array[RandVar] = Array.tabulate(11)(i => rand(1, 10))
      payload(0) >= len
      payload(1) >= 3
    }
    val myPacket = new Packet
    myPacket.randomize should be (true)
    (myPacket.payload(0).value() >= myPacket.len.value()) should be (true)
    (myPacket.payload(1).value() >= 3) should be (true)
  }

  it should "be able to constrain greater than Rand var" in {
    class Packet extends RandObj {
      currentModel = new Model(6)
      val min = 1
      val max = 100
      val len: RandVar = rand(min, max)
      val payload: Array[RandVar] = Array.tabulate(11)(i => rand(1, 100))
      payload(0) > len
      payload(1) > 3
    }
    val myPacket = new Packet
    myPacket.randomize should be (true)
    (myPacket.payload(0).value() > myPacket.len.value()) should be (true)
    (myPacket.payload(1).value() > 3) should be (true)
  }

  it should "be able to add Constraint Groups" in {
    class Packet extends RandObj {
      currentModel = new Model(6)
      val min = 1
      val max = 100
      val len: RandVar = rand(min, max)
      val payload: Array[RandVar] = Array.tabulate(11)(i => rand(min, max))
      val cgroup: JaCoPConstraintGroup = new JaCoPConstraintGroup(
        payload(0) > len,
        payload(1) > 98
      )

      val negc: crv.CRVConstraint = payload(1) < 98
      negc.disable()
    }

    val myPacket = new Packet
    myPacket.randomize should be (true)
    (myPacket.payload(0).value() > myPacket.len.value()) should be (true)
    (myPacket.payload(1).value() > 98) should be (true)
    myPacket.cgroup.disable()
    myPacket.negc.enable()

    myPacket.randomize should be (true)
    (myPacket.payload(1).value() < 98) should be (true)
  }

  it should "be possible to add Randc variables" in {
    class Packet extends RandObj {
      currentModel = new Model(6)
      val min = 1
      val max = 100
      val len: RandVar = rand(min, max)
      val randc: RandVar = rand(min, max, Cyclic)
      val payload: Array[RandVar] = Array.tabulate(11)(i => rand(min, max))

      payload(0) > len
      payload(1) > 98

      val negc: crv.CRVConstraint = payload(1) < 98
      negc.disable()
    }

    val myPacket = new Packet
    myPacket.randomize should be (true)
    val z: BigInt = myPacket.randc.value()
    myPacket.randomize should be (true)
    val x: BigInt = if (z == myPacket.max) myPacket.min else z + 1
    myPacket.randc.value() should equal (x)
  }

  it should "be able to declare nested random classes" in {
    class Packet1(model: Model) extends RandObj {
      currentModel = model
      override def toString: String = "Packet1"
      val len:               RandVar = rand(10, 100)
    }

    class Packet2(model: Model) extends RandObj {
      currentModel = model
      override def toString: String = "Packet2"
      val nestedPacket = new Packet1(model)
      val size: RandVar = rand(10, 100)
      size == nestedPacket.len
    }

    val myPacket = new Packet2(new Model)
    myPacket.currentModel.id should equal (myPacket.nestedPacket.currentModel.id)
    myPacket.randomize should be (true)
    myPacket.size.value() should equal (myPacket.nestedPacket.len.value())
  }

  it should "be able to declare conditional constraint" in {
    class Packet(model: Model) extends RandObj {
      currentModel = new Model(3)
      override def toString: String = "Packet1"
      val len:               RandVar = rand(1, 3)
      val c:                 RandVar = rand(1, 100)

      IfCon(len == 1) {
        c == 50
      }

      IfCon(len == 2) {
        c == 40
      }

      IfCon(len == 3) {
        c == 70
      }

    }
    val myPacket = new Packet(new Model)
    myPacket.randomize should be (true)
    if (myPacket.len.value() == 1) {
      myPacket.c.value() should equal (50)
    } else if (myPacket.len.value() == 2) {
      myPacket.c.value() should equal (40)
    } else {
      myPacket.c.value() should equal (70)
    }
  }

  it should "be able to declare ifThenElse constraint" in {
    class Packet(model: Model) extends RandObj {
      currentModel = new Model(5)
      override def toString: String = "Packet1"
      val len:               RandVar = rand(1, 3)
      val c:                 RandVar = rand(1, 10)
      // TODO: Fixme
      val conditional: JaCoPConstraint = IfCon(len == 1) {
        c == 50
      } ElseC {
        c == 10
      }
    }

    val myPacket = new Packet(new Model)
    myPacket.randomize should be (true)

    if (myPacket.len.value() == 1) {
      myPacket.c.value() should equal (50)
    } else {
      myPacket.c.value() should equal (10)
    }
    myPacket.conditional.disable()
    myPacket.randomize should be (true)
    if (myPacket.len.value() == 1) {
      myPacket.c.value() should not equal (50)
    }
    myPacket.randomize should be (true)
    if (myPacket.len.value() != 1) {
      myPacket.c.value() should not equal (100)
    }
  }

  it should "be possible to assign a value to a random variable" in {
    class Packet(model: Model) extends RandObj {
      currentModel = new Model(7)
      val len: RandVar = rand(1, 3)
    }

    val myPacket = new Packet(new Model)
    myPacket.len.setVar(10)
    myPacket.len.value() should be (10)
  }

  it should "be possible to create dist" in {
    class Packet extends RandObj {
      currentModel = new Model(7)
      val len: RandVar = rand(0, 1000)
      len dist (
        (1 to 10) := 5,
        (0 to 10) :=  1,
        (10 to 100) :=  1,
        (100 to 1000) := 4,
        5 := 10
      )
    }

    val myPacket = new Packet
    (0 until 100).map(_ => myPacket.randomize).forall(s => s) should be (true)
  }
}
