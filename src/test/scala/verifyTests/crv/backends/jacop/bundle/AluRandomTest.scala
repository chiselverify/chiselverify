package verifyTests.crv.backends.jacop.bundle

import chisel3._
import chisel3.experimental.BundleLiterals._
import chiseltest._
import chisel3.util._
import org.scalatest.flatspec.AnyFlatSpec

import chiselverify.crv.backends.jacop.experimental.RandBundle
import chiselverify.crv.backends.jacop.{Rand, RandObj, rand}

class AluInput(val size: Int) extends Bundle {
  val a = UInt(size.W)
  val b = UInt(size.W)
  val fn = UInt(2.W)

  // Ugly
  override def typeEquivalent(data: Data): Boolean = {
    data match {
      case aluInput: AluInput => aluInput.size == size
      case _ => false
    }
  }
}

class AluTransaction(val size: Int) extends RandObj {
  val a = rand(0, math.pow(2, size).toInt)
  val b = rand(0, math.pow(2, size).toInt)
  val fn = rand(0, 4)

  a + b <= 255
  a - b >= 0
  fn <= 3

  def expectedResult(): BigInt = {
    if (fn.value == 0) {
      a.value + b.value
    } else if (fn.value == 1) {
      a.value - b.value
    } else if (fn.value == 2) {
      a.value | b.value
    } else {
      a.value & b.value
    }
  }
}

class AluInputTransaction extends RandObj {
  val a = rand(0, 255)
  val b = rand(0, 255)
  val fn = rand(0, 4)

  a + b <= 255
  a - b >= 0
  fn <= 3

  def expectedResult(): AluOutput = {
    var result: BigInt = 0
    if (fn.value == 0) {
      result = a.value + b.value
    } else if (fn.value == 1) {
      result = a.value - b.value
    } else if (fn.value == 2) {
      result = a.value | b.value
    } else {
      result = a.value & b.value
    }
    new AluOutput(8).Lit(_.result -> result.U)
  }

  def randomBundle(): AluInput = {
    new AluInput(8).Lit(_.a -> a.value().U, _.b -> b.value().U, _.fn -> fn.value().U)
  }
}

class AluInputConstraint(size: Int) extends AluInput(size) with RandBundle {
  override def typeEquivalent(data: Data): Boolean = {
    data match {
      case aluInput: AluInputConstraint => aluInput.size == size
      case _ => false
    }
  }

  def expectedResult(): BigInt = {
    var result: BigInt = 0
    if (fn.litValue == 0) {
      result = a.litValue + b.litValue
    } else if (fn.litValue == 1) {
      result = a.litValue - b.litValue
    } else if (fn.litValue == 2) {
      result = a.litValue | b.litValue
    } else {
      result = a.litValue & b.litValue
    }
    result
  }

  // Constraints
  (a #+ b) #<= 255
  (a #- b) #>= 0
  fn #<= 3
}

class AluOutput(val size: Int) extends Bundle {
  val result = UInt(size.W)
}

class Alu(size: Int) extends Module {
  val input = IO(Input(new AluInput(size)))
  val output = IO(Output(new AluOutput(size)))

  val result: UInt = Wire(UInt(size.W))
  result := 0.U

  switch(input.fn) {
    is(0.U) { result := input.a + input.b }
    is(1.U) { result := input.a - input.b }
    is(2.U) { result := input.a | input.b }
    is(3.U) { result := input.a & input.b }
  }
  output.result := result
}

class AluRandomTest extends AnyFlatSpec with ChiselScalatestTester {
  it should "Test the ALU with random Transactions in form of bundle" in {
    test(new Alu(8)) { alu =>
      val transaction = new AluInputConstraint(8)

      for (i <- Range(0, 10)) {
        val currentT = transaction.randomBundle()
        alu.input.poke(currentT)
        alu.clock.step()
        alu.output.result.expect(currentT.expectedResult().U)
      }
    }
  }

  it should "Test the ALU with random Transactions in form of random objects" in {
    test(new Alu(8)) { alu =>
      val transaction = new AluInputTransaction

      for (i <- Range(0, 10)) {
        if(transaction.randomize) {
          alu.input.a.poke(transaction.a.value().U)
          alu.input.b.poke(transaction.b.value().U)
          alu.input.fn.poke(transaction.fn.value().U)
          alu.clock.step()
          alu.output.expect(transaction.expectedResult())
        }
      }
    }
  }

  it should "Test the ALU with random Transactions in form of random objects2" in {
    test(new Alu(8)) { alu =>
      val transaction = new AluInputTransaction

      for (i <- Range(0, 10)) {
        if(transaction.randomize) {
          alu.input.poke(transaction.randomBundle())
          alu.clock.step()
          alu.output.expect(transaction.expectedResult())
        }
      }
    }
  }

  it should "Test the ALU with random Transactions with literals" in {
    test(new Alu(8)) { alu =>
      val transaction = new AluTransaction(8)
      
      for (i <- Range(0, 10)) {
        transaction.randomize
        alu.input.a.poke(transaction.a.value.U)
        alu.input.b.poke(transaction.b.value.U)
        alu.input.fn.poke(transaction.fn.value.U)
        alu.clock.step()
        alu.output.result.expect(transaction.expectedResult().U)
      }
    }
  }
}
