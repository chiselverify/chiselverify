package chiselverify

import chiselverify.assembly.Random._
import probability_monad.Distribution.{discrete, discreteUniform}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.math._
import scala.util.Random

/*
TODO:
  - create a function which given a set of instructions and constraints returns a distribution sampler
  - make random split more efficient
  - insert jump targets randomly

- each instruction set has domains:
  - register files
  - address spaces (! io)
- each domain should have a sampling method (with implicit constraints? !!!! Option?)
- domain samplers are contained in the context and thus accessible from everywhere


- create a program object
  - to string method returns assembly program
  - get category histogram

  change randoms to BigInts
  - need for bigInt ranges



*/

package object assembly {


  implicit def intToRangeDist(x: (Int,Double)): (Range,Double) = (x._1 until x._1, x._2)


  implicit def someToOption[T](t: T): Option[T] = {
    t match {
      case None => None
      case t => Some(t)
    }
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

  //def solver(constraints: Constraints, )

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

  def fillDistributionGaps[T](dis: Seq[(T,Double)])(domain: Seq[T]): Seq[(T,Double)] = {
    val totalPercentage = dis.map(_._2).sum
    val newElements = domain.filter(!dis.map(_._1).contains(_))
    dis ++ newElements.map( c => c -> (1-totalPercentage)/newElements.length)
  }


  trait Constraint

  trait InstructionConstraint extends Constraint
  trait MemoryConstraint extends Constraint
  trait InputOutputConstraint extends Constraint

  trait DistributionConstraint[T] {
    val dis: Seq[(T,Double)]
    def merge(that: DistributionConstraint[T]): Seq[(T,Double)] = {
      val coll = dis ++ that.dis
      val norm = coll.map(_._2).sum
      coll.map{ case (cat,d) => (cat,d/norm) }
    }
  }

  case class CategoryDistribution(disIn: (Category,Double)*) extends InstructionConstraint with DistributionConstraint[Category] {
    val dis = fillDistributionGaps(disIn)(Category.all)
  }
  case class CategoryWhiteList(gr: Category*) extends InstructionConstraint
  case class CategoryBlackList(bl: Category*) extends InstructionConstraint

  case class MemoryDistribution(dis: (Range,Double)*) extends MemoryConstraint with DistributionConstraint[Range]

  case class IODistribution(dis: (Range,Double)*) extends InputOutputConstraint with DistributionConstraint[Range]


  object InstructionFactory {
    def apply(fun: GeneratorContext => Seq[InstructionFactory with Categorizable]): InstructionFactory = {
      new InstructionFactory {
        override def produce()(implicit context: GeneratorContext): Seq[Instruction] = fun(context).flatMap(_.produce())
      }
    }
  }
  trait InstructionFactory {
    def produce()(implicit context: GeneratorContext): Seq[Instruction]
  }

  abstract class InstructionSet {
    val instructions: Seq[InstructionFactory with Categorizable]
    val memoryAddressSpace: Range
    val inputOutputAddressSpace: Range
  }




  case class GeneratorContext (
                              nextInstruction: Seq[Constraint] => InstructionFactory with Categorizable,
                              nextMemoryAddress: Seq[Constraint] => BigInt,
                              nextIOAddress: Seq[Constraint] => BigInt,
                              nextJumpTarget: () => BigInt,
                              pc: ProgramCounter
                              )



  object ProgramGenerator {
    def apply(isa: InstructionSet)(constraints: Constraint*): ProgramGenerator = {
      val pc = new ProgramCounter
      new ProgramGenerator(
        GeneratorContext(
          createInstructionGenerator(isa, constraints),
          createMemoryAddressGenerator(isa, constraints),
          createIOAddressGenerator(isa, constraints),
          createJumpTargetGenerator(pc),
          pc
        )
      )
    }


    private def createInstructionGenerator(isa: InstructionSet, constraints: Seq[Constraint]): Seq[Constraint] => InstructionFactory with Categorizable = { additionalConstraints =>
      val allCons = (constraints ++ additionalConstraints).collect { case x: InstructionConstraint => x}
      val allowedCategories: Seq[Category] = allCons.foldLeft(Category.all) {
        case (cats, CategoryBlackList(bl @ _*)) => cats.filter(!bl.contains(_))
        case (cats, CategoryWhiteList(w @ _*)) => cats.filter(w.contains(_))
        case (cats,_) => cats
      }

      // TODO: merge two entries with the same category
      val unnormalizedDistributionConstraints = allCons.collect { case x: CategoryDistribution => x }.flatMap(_.dis)

      val normalizationFactor = unnormalizedDistributionConstraints.map(_._2).sum
      val distributionConstraints = unnormalizedDistributionConstraints.filter{ case (c,_) => allowedCategories.contains(c)}.map { case (c,d) =>
        (isa.instructions.filter(_.isOfCategory(c)),d)
      }.filter(_._1.nonEmpty).map { case (instr,d) =>
        (discreteUniform(instr).sample(1).head,d/normalizationFactor)
      }

      discrete(distributionConstraints:_*).sample(1).head

    }

    private def createMemoryAddressGenerator(isa: InstructionSet, constraints: Seq[Constraint]): Seq[Constraint] => BigInt = { additionalConstraints =>
      val dis = constraints.collect{case x: MemoryDistribution => x} match {
        case x::_ => x.dis
        case _ => Seq(isa.memoryAddressSpace -> 1.0)
      }
      BigInt(discrete(dis.map{ case (r,d) => (rand(r),d)}:_*).sample(1).head)
    }

    private def createIOAddressGenerator(isa: InstructionSet, constraints: Seq[Constraint]): Seq[Constraint] => BigInt = { additionalConstraints =>
      val dis = constraints.collect { case x: IODistribution => x } match {
        case x::_ => x.dis
        case _ => Seq(isa.inputOutputAddressSpace -> 1.0)
      }
      BigInt(discrete(dis.map{ case (r,d) => (rand(r),d)}:_*).sample(1).head)
    }

    private def createJumpTargetGenerator(pc: ProgramCounter): () => BigInt = { () =>
      rand(pc.get.toInt)
    }

  }

  class ProgramGenerator(context: GeneratorContext) {

    def generate(n: Int): Seq[Instruction] = Seq.fill(n)(context.nextInstruction(Seq()).produce()(context)).flatten
    def generate(p: Pattern): Seq[Instruction] = p.produce()(context)

  }







}
