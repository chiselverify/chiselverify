package chiselverify

import chiselverify.assembly.RandomHelpers._
import probability_monad.Distribution.{discrete, discreteUniform}

import scala.math._

/*
TODO:
  - create a function which given a set of instructions and constraints returns a distribution sampler



  change randoms to BigInts
  - need for bigInt ranges



*/

package object assembly {

  implicit def intToRangeDist(x: (Int, Double)): (Range, Double) = (x._1 until x._1, x._2)

  implicit def intToBigIntOption(x: Int): Option[BigInt] = Some(BigInt(x))

  implicit def someToOption[T](t: T): Option[T] = {
    t match {
      case None => None
      case t => Some(t)
    }
  }

  def fillDistributionGaps[T](dis: Seq[(T, Double)])(domain: Seq[T]): Seq[(T, Double)] = {
    val totalPercentage = dis.map(_._2).sum
    val newElements = domain.filter(!dis.map(_._1).contains(_))
    dis ++ newElements.map(c => c -> (1 - totalPercentage) / newElements.length)
  }

  trait Constraint

  trait InstructionConstraint extends Constraint

  trait MemoryConstraint extends Constraint

  trait InputOutputConstraint extends Constraint

  trait DistributionConstraint[T] {
    val dis: Seq[(T, Double)]

    def merge(that: DistributionConstraint[T]): Seq[(T, Double)] = {
      val coll = dis ++ that.dis
      val norm = coll.map(_._2).sum
      coll.map { case (cat, d) => (cat, d / norm) }
    }
  }

  trait InstructionFactory {
    def produce()(implicit context: GeneratorContext): Seq[Instruction]
  }

  abstract class InstructionSet {
    val instructions: Seq[InstructionFactory with Categorizable]
    val memoryAddressSpace: BigRange
    val inputOutputAddressSpace: BigRange
  }

  class ProgramCounter {
    private var pc = BigInt(0)

    def get: BigInt = pc

    def inc(): BigInt = {
      val old = pc
      pc += 1
      old
    }
  }

  case class CategoryDistribution(disIn: (Category, Double)*) extends InstructionConstraint with DistributionConstraint[Category] {
    val dis = fillDistributionGaps(disIn)(Category.all)
  }

  case class CategoryWhiteList(gr: Category*) extends InstructionConstraint

  case class CategoryBlackList(bl: Category*) extends InstructionConstraint

  case class MemoryDistribution(dis: (BigRange, Double)*) extends MemoryConstraint with DistributionConstraint[BigRange]

  case class IODistribution(dis: (BigRange, Double)*) extends InputOutputConstraint with DistributionConstraint[BigRange]

  case class GeneratorContext(
                               nextInstruction: Seq[Constraint] => InstructionFactory with Categorizable,
                               nextMemoryAddress: Seq[Constraint] => BigInt,
                               nextIOAddress: Seq[Constraint] => BigInt,
                               nextJumpTarget: () => BigInt,
                               pc: ProgramCounter
                             )



  //TODO: add pattern for label
  object Label {
    def apply()(implicit context: GeneratorContext): Pattern = {
      Pattern(implicit c => {
        Seq()
      })
    }

    def apply(id: Int): Instruction = {
      new Instruction() {
        override def apply(): Instruction = Label(id)

        override def toAsm: String = s"L$id:"
      }
    }

    def apply(id: String): Instruction = {
      new Instruction() {
        override def apply(): Instruction = Label(id)

        override def toAsm: String = s"$id:"
      }
    }

  }

  object InstructionFactory {
    def apply(fun: GeneratorContext => Seq[InstructionFactory with Categorizable]): InstructionFactory = {
      new InstructionFactory {
        override def produce()(implicit context: GeneratorContext): Seq[Instruction] = fun(context).flatMap(_.produce())
      }
    }
  }




}
