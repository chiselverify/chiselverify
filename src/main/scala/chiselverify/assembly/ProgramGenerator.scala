package chiselverify.assembly

import chiselverify.assembly.RandomHelpers.{BigRange, rand}
import probability_monad.Distribution.{discrete, discreteUniform}

import scala.math.BigInt

object ProgramGenerator {
  /**
    *
    */
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
    val allCons = (constraints ++ additionalConstraints).collect { case x: InstructionConstraint => x }
    val allowedCategories: Seq[Category] = allCons.foldLeft(Category.all) {
      case (cats, CategoryBlackList(bl@_*)) => cats.filter(!bl.contains(_))
      case (cats, CategoryWhiteList(w@_*)) => cats.filter(w.contains(_))
      case (cats, _) => cats
    }

    // TODO: merge two entries with the same category
    val unnormalizedDistributionConstraints = allCons.collect { case x: CategoryDistribution => x }.flatMap(_.dis)

    val normalizationFactor = unnormalizedDistributionConstraints.map(_._2).sum
    val distributionConstraints = unnormalizedDistributionConstraints.filter { case (c, _) => allowedCategories.contains(c) }.map { case (c, d) =>
      (isa.instructions.filter(_.isOfCategory(c)), d)
    }.filter(_._1.nonEmpty).map { case (instr, d) =>
      (discreteUniform(instr).sample(1).head, d / normalizationFactor)
    }

    discrete(distributionConstraints: _*).sample(1).head

  }

  private def createMemoryAddressGenerator(isa: InstructionSet, constraints: Seq[Constraint]): Seq[Constraint] => BigInt = { additionalConstraints =>
    val dis = constraints.collect { case x: MemoryDistribution => x } match {
      case x :: _ => x.dis
      case _ => Seq(isa.memoryAddressSpace -> 1.0)
    }
    discrete(dis.map { case (r, d) => (rand(r), d) }: _*).sample(1).head
  }

  private def createIOAddressGenerator(isa: InstructionSet, constraints: Seq[Constraint]): Seq[Constraint] => BigInt = { additionalConstraints =>
    val dis = constraints.collect { case x: IODistribution => x } match {
      case x :: _ => x.dis
      case _ => Seq(isa.inputOutputAddressSpace -> 1.0)
    }
    discrete(dis.map { case (r, d) => (rand(r), d) }: _*).sample(1).head
  }

  private def createJumpTargetGenerator(pc: ProgramCounter): () => BigInt = { () =>
    rand(pc.get.toInt)
  }

}

class ProgramGenerator(context: GeneratorContext) {

  def generate(n: Int): Seq[Instruction] = Seq.fill(n)(context.nextInstruction(Seq()).produce()(context)).flatten

  def generate(p: Pattern): Seq[Instruction] = p.produce()(context)

}
