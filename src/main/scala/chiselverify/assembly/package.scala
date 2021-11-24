package chiselverify

import chiselverify.assembly.RandomHelpers._
import probability_monad.Distributions

import scala.language.implicitConversions
import scala.math._

package object assembly {

  /**
    * possible expansions:
    *   - create a randomly initialized static data memory segment
    *   - wrap the generated program with a ELF file format
    */

  // makes the passing of int initializers in patterns less verbose by converting
  // an int directly to a BigInt option
  implicit def intToBigIntOption(x: Int): Option[BigInt] = Some(BigInt(x))

  // makes the passing of other initializers in patterns less verbose
  // Everything else than None can be seen as Some
  implicit def someToOption[T](t: T): Option[T] = {
    t match {
      case None => None
      case t => Some(t)
    }
  }


  object AssemblyDistributions extends Distributions(scala.util.Random)

  /**
    * This class has to be extended by any instruction set.
    * The instruction sequence field needs to be overwritten manually
    */
  abstract class InstructionSet {
    val instructions: Seq[InstructionFactory with Categorizable]
    val memoryAddressSpace: BigRange
    val inputOutputAddressSpace: BigRange
  }

  class Counter {
    private var value = BigInt(0)

    def get: BigInt = value

    def inc(): BigInt = {
      val old = value
      value += 1
      old
    }
  }


  trait Constraint

  trait InstructionConstraint extends Constraint

  trait MemoryConstraint extends Constraint

  trait InputOutputConstraint extends Constraint

  trait DistributionConstraint[T] {
    val dis: Seq[(T, Double)]
  }

  case class CategoryDistribution(dis: (Category, Double)*) extends InstructionConstraint with DistributionConstraint[Category]

  case class CategoryWhiteList(gr: Category*) extends InstructionConstraint

  case class CategoryBlackList(bl: Category*) extends InstructionConstraint

  case class MemoryDistribution(dis: (BigRange, Double)*) extends MemoryConstraint with DistributionConstraint[BigRange]

  case class IODistribution(dis: (BigRange, Double)*) extends InputOutputConstraint with DistributionConstraint[BigRange]

  /**
    * All nodes in a program template call tree are instruction factories
    * At generation time the [[produce()]] function invokes the transformation into a flat
    * instruction sequence
    */
  trait InstructionFactory {
    def produce()(implicit context: GeneratorContext): Seq[Instruction]
  }

  object InstructionFactory {
    // create a new anonymous instruction factory
    def apply(fun: GeneratorContext => Seq[InstructionFactory with Categorizable]): InstructionFactory = {
      new InstructionFactory {
        override def produce()(implicit context: GeneratorContext): Seq[Instruction] = fun(context).flatMap(_.produce())
      }
    }
  }


}
