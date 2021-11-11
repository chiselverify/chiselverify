package chiselverify.assembly

import chiselverify.assembly.RandomHelpers.{rand, randomSelect}
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
        isa,
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
    val distConstraints = allCons.collect { case x: CategoryDistribution => x }.flatMap(_.dis)

    if (distConstraints.nonEmpty) {
      val distributionConstraints = distConstraints.filter { case (c, _) => allowedCategories.contains(c) }.map { case (c, d) =>
        (isa.instructions.filter(_.isOfCategory(c)), d)
      }.filter(_._1.nonEmpty).map { case (instr, d) =>
        (discreteUniform(instr).sample(1).head, d)
      }

      discrete(distributionConstraints: _*).sample(1).head
    } else {
      randomSelect(isa.instructions.filter(_.isOfOneOfCategories(allowedCategories)))
    }


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

object DummyGeneratorContext {
  def apply(isa_arg: InstructionSet) = GeneratorContext(isa_arg, c => Label(""), c => BigInt(0), c => BigInt(0), () => BigInt(0), new ProgramCounter)
}

case class Program(instructions: Seq[Instruction], isa: InstructionSet) {
  override def toString: String = instructions.map(_.toAsm).map { str =>
    if (str.contains(':')) str else "  " + str
  }.mkString("\n")

  def pretty: String = {
    s"$this\n\nInstruction histogram:\n${this.histogram.map(_.toString()).mkString(", ")}\nCategory histogram:\n${this.categoryHistogram.map(_.toString()).mkString(", ")}\n"
  }

  def histogram: Seq[(String, Int)] = {
    isa.instructions.flatMap(_.produce()(DummyGeneratorContext(isa))).map(_.toAsm.split(" ").head).distinct.map { instr =>
      instr -> instructions.count(_.toAsm.split(" ").head == instr)
    }
  }

  def categoryHistogram: Seq[(String, Int)] = {
    Category.all.map { c =>
      (c.toString, instructions.count(_.categories.contains(c)))
    }
  }
}

class ProgramGenerator(context: GeneratorContext) {

  def generate(n: Int): Program = Program(Seq.fill(n)(context.nextInstruction(Seq()).produce()(context)).flatten, context.isa)

  def generate(p: Pattern): Program = Program(p.produce()(context), context.isa)

}
