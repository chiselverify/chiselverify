package examples.leros

import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chisel3.tester._
import chiseltest.ChiselScalatestTester
import chiselverify.coverage.{Bins, CoverPoint, CoverageCollector, CoverageReporter, CrossBin, CrossPoint}
import chiselverify.crv.{RangeBinder, ValueBinder}
import chiselverify.crv.backends.jacop._
import org.scalatest.{BeforeAndAfterAll, FlatSpec}

import scala.math.pow

case class AluAccumulator(value: BigInt)

class AluTransaction(seed: Int, size: Int) extends RandObj {
  currentModel = new Model(seed)
  val max = pow(2, size).toInt

  val op = new Rand("op", 0, 7)
  val din = new Rand("din", 0, max)
  val ena = new Rand("ena", 0, 1)

  val values = din dist (
    0 to 0xF := 1,
    0xF to 0xFF := 1,
    0xFF to 0xFFF := 1,
    0xFFF to 0xFFFF := 1,
  )

  val onlyHigh = din dist (
    0xFF to 0xFFF := 1,
    0xFFF to 0xFFFF := 10,
  )

  val enaHigh = ena dist (
    1 := 99,
    0 := 1
  )

  onlyHigh.disable()

  def toBundle: AluAccuInput = {
    new AluAccuInput(size).Lit(_.op -> op.value().U, _.din -> din.value().U, _.ena -> ena.value().B)
  }
}

object AluGoldenModel {

  def genNextState(transaction: AluAccuInput, accu: AluAccumulator): AluAccumulator = {
    val mask: Int = (1 << transaction.din.getWidth) - 1
    if (transaction.ena.litToBoolean) {
      transaction.op.litValue().toInt match {
        case 0 => accu
        case 1 => if ((accu.value + transaction.din.litValue()) > mask) {
          AluAccumulator((accu.value + transaction.din.litValue()) & mask)
        } else {
          AluAccumulator(accu.value + transaction.din.litValue())
        }
        case 2 => if ((accu.value - transaction.din.litValue()) < 0) {
          AluAccumulator((accu.value - transaction.din.litValue()) & mask)
        } else {
          AluAccumulator(accu.value - transaction.din.litValue())
        }
        case 3 => AluAccumulator(accu.value & transaction.din.litValue())
        case 4 => AluAccumulator(accu.value | transaction.din.litValue())
        case 5 => AluAccumulator(accu.value ^ transaction.din.litValue)
        case 6 => AluAccumulator(transaction.din.litValue())
        case 7 => AluAccumulator(accu.value.toInt >>> 1)
      }
    } else {
      accu
    }
  }

  def genOutputStates(listT: List[AluAccuInput], accu: AluAccumulator): List[AluAccumulator] = {
    listT match {
      case Nil => Nil
      case x :: xs =>
        val nexAccu = genNextState(x, accu)
        nexAccu :: genOutputStates(xs, nexAccu)
    }
  }

  def serializeAccu(accu: AluAccumulator, size: Int): AluAccuOutput =
    new AluAccuOutput(size).Lit(_.accu -> accu.value.U)

  def generateAluAccuTransactions(listT: List[AluAccuInput], accu: AluAccumulator): List[AluAccuOutput] = {
    genOutputStates(listT, accu).map(serializeAccu(_, listT.head.din.getWidth))
  }

  def compareSingle(transaction: (AluAccuOutput, AluAccuOutput)): Boolean = {
    val (dutT, genT) = transaction
     equals(dutT.accu.litValue() == genT.accu.litValue())
    dutT.accu.litValue() == genT.accu.litValue()
  }

  def compare(transactions: List[(AluAccuOutput, AluAccuOutput)]): List[Boolean] = {
    transactions map compareSingle
  }
}

trait AluBehavior {
  this: FlatSpec with ChiselScalatestTester =>
  val coverageCollector = new CoverageCollector

  def compute(name: String, size: Int, inputT: List[AluAccuInput]): Unit = {
    it should s"test alu with $name" in {
      test(new AluAccuMultiChisel(size)) { dut =>
        val cr = new CoverageReporter(dut)
        cr.register(
          List(
          CoverPoint(dut.input.op , "op")(
            List(
              Bins("nop", 0 to 0), Bins("add", 1 to 1),
              Bins("sub", 2 to 2), Bins("and", 3 to 3),
              Bins("or", 4 to 4), Bins("xor", 5 to 5),
              Bins("ld", 6 to 6), Bins("shr", 7 to 7))),
          CoverPoint(dut.input.din, "din")(
            List(
              Bins("0xF", 0 to 0xF), Bins("0xFF", 0xF to 0xFF),
              Bins("0xFFF", 0xFF to 0xFFF), Bins("0xFFFF", 0xFFF to 0xFFFF),
            )
          ),
            CoverPoint(dut.output.accu, "accu")(
              List(
                Bins("0xF", 0 to 0xF), Bins("0xFF", 0xF to 0xFF),
                Bins("0xFFF", 0xFF to 0xFFF), Bins("0xFFFF", 0xFFF to 0xFFFF),
              )
            ),
            CoverPoint(dut.input.ena, "ena")(
              List(
                Bins("disabled", 0 to 0),
                Bins("enabled", 1 to 1)
              )
            )), List(
            CrossPoint("operations cross enable", "op", "ena")(
              List(
                CrossBin("operation enable", 0 to 7, 1 to 1),
                CrossBin("operation disabled", 0 to 7, 0 to 0),
            )))
        )

        val computedTransactions: List[AluAccuOutput] = inputT.map { T =>
          dut.input.poke(T)
          dut.clock.step()
          cr.sample()
          dut.output.peek
        }

        val goldenTransactions = AluGoldenModel.generateAluAccuTransactions(inputT, AluAccumulator(0))
        AluGoldenModel.compare(computedTransactions zip goldenTransactions)
        coverageCollector.collect(cr.report)
      }
    }
  }
}

class AluVerification extends FlatSpec with AluBehavior with ChiselScalatestTester with BeforeAndAfterAll {
  behavior of "AluAccumulator"
  val size = 16
  val tnumber = 5000
  val seeds = List(30, 104 , 60, 90, 200, 50, 22, 2000, 40, 900, 70, 23)

  println(s"Testing ALU with ${tnumber * seeds.size * 2} random transactions")
  // General test
  seeds.map { seed =>
    val length = tnumber
    val masterT = new AluTransaction(seed, size)
    val transactions: Seq[AluAccuInput] = (0 to length) map { _ =>
      masterT.randomize
      masterT.toBundle
    }
    it should behave like compute(s"seed = $seed, and normal values", size, transactions.toList)
  }

  // Only transactions with values between 0xFF to 0xFFFF
  seeds.map { seed =>
    val length = tnumber
    val masterT = new AluTransaction(seed, size)
    val onlyHighTransactions: Seq[AluAccuInput] = (0 to length) map { _ =>
      masterT.onlyHigh.enable()
      masterT.values.disable()
      masterT.randomize
      masterT.toBundle
    }
    it should behave like compute(s"seed = $seed, and high values", size, onlyHighTransactions.toList)
  }


  override def afterAll(): Unit = {
    println(coverageCollector.report)
  }
}
