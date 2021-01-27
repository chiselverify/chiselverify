package verifyTests.crv.backends.jacop
import chiselverify.crv
import chiselverify.crv.{ValueBinder, RangeBinder}
import chiselverify.crv.backends.jacop.{Constraint, ConstraintGroup, IfCon, Model, Rand, RandObj, Randc}
import org.scalatest.{FlatSpec, Matchers}


class TestRandJacop extends FlatSpec with Matchers {

  it should "be able to declare a random variable and and a constraint" in {
    class Packet extends RandObj {
      currentModel = new Model(3)

      val min = 1
      val max = 100
      var size = new Rand("size", min, max)
      var len = new Rand("len", min, max)
      len #>= size
      val x:       crv.Constraint = len #<= size
      val y:       crv.Constraint = len #> 4
      val payload: Array[Rand] = Array.tabulate(11)(i => new Rand("byte[" + i + "]", 1, 100))
      payload(0) #= (len #+ size)
    }

    val myPacket = new Packet
    myPacket.x.disable()
    assert(myPacket.randomize)
    assert(myPacket.len.value() >= myPacket.size.value())

    assert(myPacket.randomize)
    assert(myPacket.len.value() >= myPacket.size.value())

    assert(myPacket.randomize)
    assert(myPacket.len.value() >= myPacket.size.value())

    myPacket.y.disable()
    myPacket.x.enable()
    myPacket.randomize
    assert(myPacket.len.value() <= myPacket.size.value())

    myPacket.y.enable()
    myPacket.x.disable()
    myPacket.randomize
    assert(myPacket.len.value() >= myPacket.size.value())

    myPacket.y.disable()
    myPacket.x.enable()
    myPacket.randomize
    assert(myPacket.len.value() <= myPacket.size.value())

    myPacket.y.enable()
    myPacket.x.disable()
    myPacket.randomize
    assert(myPacket.len.value() >= myPacket.size.value())
  }

  it should "be able to enable disable constraint 2" in {
    class Packet extends RandObj {
      currentModel = new Model(3)
      val min = 1
      val max = 100
      val size = new Rand("size", min, max)
      val len = new Rand("len", min, max)
      val grater: Constraint = size #> len
      val smaller: Constraint = size #< len
    }
    val myPacket = new Packet
    myPacket.grater.disable()
    assert(myPacket.randomize)
    assert(myPacket.size.value() < myPacket.len.value())
    myPacket.smaller.disable()
    myPacket.grater.enable()
    assert(myPacket.randomize)
    assert(myPacket.size.value() > myPacket.len.value())
  }

  it should "be able to subtract two Rand var" in {
    class Packet extends RandObj {
      currentModel = new Model(3)
      val min = 1
      val max = 100
      val size = new Rand("size", min, max)
      val len = new Rand("len", min, max)
      val payload: Array[Rand] = Array.tabulate(11)(i => new Rand("byte[" + i + "]", 1, 100))
      payload(0) #= (len #- size)
    }
    val myPacket = new Packet
    assert(myPacket.randomize)
    assert(myPacket.payload(0).value() == myPacket.len.value - myPacket.size.value())
  }

  it should "be able to add two Rand var" in {
    class Packet extends RandObj {
      currentModel = new Model(3)
      val min = 1
      val max = 100
      val size = new Rand("size", min, max)
      val len = new Rand("len", min, max)
      val payload: Array[Rand] = Array.tabulate(11)(i => new Rand("byte[" + i + "]", 1, 100))
      payload(0) #= (len #+ size)
      payload(1) #= (len #+ 4)
    }
    val myPacket = new Packet
    assert(myPacket.randomize)
    assert(myPacket.payload(0).value() == myPacket.len.value + myPacket.size.value())
    assert(myPacket.payload(1).value() == myPacket.len.value + 4)
  }

  it should "be able to divide two Rand var" in {
    class Packet extends RandObj {
      currentModel = new Model(3)
      val min = 1
      val max = 100
      val size = new Rand("size", min, max)
      val len = new Rand("len", min, max)
      val payload: Array[Rand] = Array.tabulate(11)(i => new Rand("byte[" + i + "]", 1, 100))
      payload(0) #= len.div(size)
      payload(1) #= len.div(4)
    }
    val myPacket = new Packet
    assert(myPacket.randomize)
    assert(myPacket.payload(0).value() == myPacket.len.value / myPacket.size.value())
    assert(myPacket.payload(1).value() == myPacket.len.value / 4)
  }

  it should "be able to multiply two Rand var" in {
    class Packet extends RandObj {
      currentModel = new Model(5)
      val min = 1
      val max = 100
      val size = new Rand("size", min, max)
      val len = new Rand("len", min, max)
      val payload: Array[Rand] = Array.tabulate(11)(i => new Rand("byte[" + i + "]", 1, 100))
      payload(0) #= (len #* size)
      payload(1) #= (len #* 4)
    }
    val myPacket = new Packet
    assert(myPacket.randomize)
    assert(myPacket.payload(0).value() == myPacket.len.value * myPacket.size.value())
    assert(myPacket.payload(1).value() == myPacket.len.value * 4)
  }

  it should "be able to constraint the reminder of Rand var" in {
    class Packet extends RandObj {
      currentModel = new Model(6)
      val min = 1
      val max = 100
      val size = new Rand("size", min, max)
      val len = new Rand("len", min, max)
      val payload: Array[Rand] = Array.tabulate(11)(i => new Rand("byte[" + i + "]", 1, 100))
      payload(0) #= len.mod(size)
      payload(1) #= len.mod(4)
    }
    val myPacket = new Packet
    assert(myPacket.randomize)
    assert(myPacket.payload(0).value() == myPacket.len.value % myPacket.size.value())
    assert(myPacket.payload(1).value() == myPacket.len.value % 4)
  }

  it should "be able to constraint the exponential of Rand var" in {
    class Packet extends RandObj {
      currentModel = new Model(6)
      val min = 1
      val max = 100
      val size = new Rand("size", 2, 3)
      val len = new Rand("len", min, max)
      val payload: Array[Rand] = Array.tabulate(11)(i => new Rand("byte[" + i + "]", 1, 100))
      payload(0) #= (len #^ size)
      payload(1) #= (len #^ 3)
    }
    val myPacket = new Packet
    assert(myPacket.randomize)
    assert(myPacket.payload(0).value() == math.pow(myPacket.len.value.toDouble, myPacket.size.value().toDouble).toInt)
    assert(myPacket.payload(1).value() == math.pow(myPacket.len.value.toDouble, 3).toInt)
  }

  it should "be able to constraint less or equal then  Rand var" in {
    class Packet extends RandObj {
      currentModel = new Model(6)
      val min = 1
      val max = 100
      val len = new Rand("len", min, max)
      val payload: Array[Rand] = Array.tabulate(11)(i => new Rand("byte[" + i + "]", 1, 100))
      payload(0) #<= len
      payload(1) #<= 3
    }
    val myPacket = new Packet
    myPacket.randomize
    assert(myPacket.payload(0).value() <= myPacket.len.value())
    assert(myPacket.payload(1).value() <= 3)
  }

  it should "be able to constraint less then  Rand var" in {
    class Packet extends RandObj {
      currentModel = new Model(6)
      val min = 1
      val max = 100
      val len = new Rand("len", min, max)
      val payload: Array[Rand] = Array.tabulate(11)(i => new Rand("byte[" + i + "]", 1, 100))
      payload(0) #< len
      payload(1) #< 3
    }
    val myPacket = new Packet
    assert(myPacket.randomize)
    assert(myPacket.payload(0).value() < myPacket.len.value())
    assert(myPacket.payload(1).value() < 3)
  }

  it should "be able to constraint gather or equal than of Rand var" in {
    class Packet extends RandObj {
      currentModel = new Model(6)
      val min = 1
      val max = 10
      val len = new Rand("len", min, max)
      val payload: Array[Rand] = Array.tabulate(11)(i => new Rand("byte[" + i + "]", 1, 10))
      payload(0) #>= len
      payload(1) #>= 3
    }
    val myPacket = new Packet
    myPacket.randomize
    assert(myPacket.payload(0).value() >= myPacket.len.value())
    assert(myPacket.payload(1).value() >= 3)
  }

  it should "be able to constraint gather than of Rand var" in {
    class Packet extends RandObj {
      currentModel = new Model(6)
      val min = 1
      val max = 100
      val len = new Rand("len", min, max)
      val payload: Array[Rand] = Array.tabulate(11)(i => new Rand("byte[" + i + "]", 1, 100))
      payload(0) #> len
      payload(1) #> 3
    }
    val myPacket = new Packet
    myPacket.randomize
    assert(myPacket.payload(0).value() > myPacket.len.value())
    assert(myPacket.payload(1).value() > 3)
  }

  it should "be able to add Constraint Groups" in {

    class Packet extends RandObj {
      currentModel = new Model(6)
      val min = 1
      val max = 100
      val len = new Rand("len", min, max)
      val payload: Array[Rand] = Array.tabulate(11)(i => new Rand("byte[" + i + "]", min, max))
      val cgroup: ConstraintGroup = new ConstraintGroup(
        payload(0) #> len,
        payload(1) #> 98
      )

      val negc: crv.Constraint = payload(1) #< 98
      negc.disable()
    }

    val myPacket = new Packet
    assert(myPacket.randomize)
    assert(myPacket.payload(0).value() > myPacket.len.value())
    assert(myPacket.payload(1).value() > 98)
    myPacket.cgroup.disable()
    myPacket.negc.enable()

    assert(myPacket.randomize)
    assert(myPacket.payload(1).value() < 98)
  }

  it should "be possible to add Randc variables" in {

    class Packet extends RandObj {
      currentModel = new Model(6)
      val min = 1
      val max = 100
      val len = new Rand("len", min, max)
      val randc = new Randc(min, max)
      val payload: Array[Rand] = Array.tabulate(11)(i => new Rand("byte[" + i + "]", min, max))

      payload(0) #> len
      payload(1) #> 98

      val negc: crv.Constraint = payload(1) #< 98
      negc.disable()
    }

    val myPacket = new Packet
    assert(myPacket.randomize)
    val z: BigInt = myPacket.randc.value()
    assert(myPacket.randomize)
    val x: BigInt = if (z == myPacket.max) myPacket.min else z + 1
    assert(myPacket.randc.value() == x)
  }

  it should "be able to declare nested random classes" in {
    class Packet1(model: Model) extends RandObj {
      currentModel = model
      override def toString: String = "Packet1"
      val len:               Rand = new Rand(10, 100)
    }

    class Packet2(model: Model) extends RandObj {
      currentModel = model
      override def toString: String = "Packet2"
      val nestedPacket = new Packet1(model)
      val size = new Rand(10, 100)
      size #= nestedPacket.len
    }

    val myPaket = new Packet2(new Model)
    assert(myPaket.currentModel.id == myPaket.nestedPacket.currentModel.id)
    assert(myPaket.randomize)
    assert(myPaket.size.value() == myPaket.nestedPacket.len.value())
  }

  it should "be able to declare conditional constraint" in {
    class Packet(model: Model) extends RandObj {
      currentModel = new Model(3)
      override def toString: String = "Packet1"
      val len:               Rand = new Rand(1, 3)
      val c:                 Rand = new Rand(1, 100)

      IfCon(len #= 1) {
        c #= 50
      }

      IfCon(len #= 2) {
        c #= 40
      }

      IfCon(len #= 3) {
        c #= 70
      }

    }
    val myPacket = new Packet(new Model)
    assert(myPacket.randomize)
    if (myPacket.len.value() == 1) {
      assert(myPacket.c.value() == 50)
    } else if (myPacket.len.value() == 2) {
      assert(myPacket.c.value() == 40)
    } else {
      assert(myPacket.c.value() == 70)
    }
  }

  it should "be able to declare ifThenElse constraint" in {
    class Packet(model: Model) extends RandObj {
      currentModel = new Model(5)
      override def toString: String = "Packet1"
      val len:               Rand = new Rand(1, 3)
      val c:                 Rand = new Rand(1, 10)
      // TODO: Fixme
      val conditional: Constraint = IfCon(len #= 1) {
        c #= 50
      } ElseC {
        c #= 10
      }
    }

    val myPacket = new Packet(new Model)
    assert(myPacket.randomize)

    if (myPacket.len.value() == 1) {
      assert(myPacket.c.value() == 50)
    } else {
      assert(myPacket.c.value() == 10)
    }
    myPacket.conditional.disable()
    assert(myPacket.randomize)
    if (myPacket.len.value() == 1) {
      assert(myPacket.c.value() != 50)
    }
    assert(myPacket.randomize)
    if (myPacket.len.value() != 1) {
      assert(myPacket.c.value() != 100)
    }
  }

  it should "be possible to assign a value to a random variable" in {
    class Packet(model: Model) extends RandObj {
      currentModel = new Model(7)
      val len: Rand = new Rand(1, 3)
    }

    val myPacket = new Packet(new Model)
    myPacket.len.setVar(10)
  }

  it should "be possible to create dist" in {
    class Packet extends RandObj {
      currentModel = new Model(7)
      val len: Rand = new Rand("len", 0, 1000)
      len dist (
        (1 to 10) := 5,
        (0 to 10) :=  1,
        (10 to 100) :=  1,
        (100 to 1000) := 4,
        5 := 10
      )
    }

    val myPacket = new Packet
    for (i <- Range(1, 100)) {
       if (myPacket.randomize) {
          myPacket.debug()
       } else {
         println("Noo")
       }
    }

  }
}
