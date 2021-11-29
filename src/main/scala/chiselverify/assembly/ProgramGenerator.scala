package chiselverify.assembly

import chiselverify.assembly.Label.LabelRecord
import chiselverify.assembly.RandomHelpers.{rand, randomSelect}
import chiselverify.assembly.AssemblyDistributions._

import java.io.{File, PrintWriter}
import scala.collection.mutable.ListBuffer
import scala.math.BigInt
import scala.util.Random

/**
  * The generator context is used to collect information required during the generation process centrally
  * It holds all "sampler" functions which produce new values for instructions, memory addresses etc.
  * Global state like defined labels and the current PC are tracked here.
  * An instance of a generator context is passed down the function call tree which produces the final instruction sequence
  */
case class GeneratorContext(
                             isa: InstructionSet,
                             nextInstruction: Seq[Constraint] => InstructionFactory with Categorizable,
                             nextMemoryAddress: Seq[Constraint] => BigInt,
                             nextIOAddress: Seq[Constraint] => BigInt,
                             nextJumpTarget: () => LabelRecord,
                             pc: Counter,
                             labelCounter: Counter,
                             jumpTargets: ListBuffer[LabelRecord]
                           )

object GeneratorContext {
  // a generator context can be defined by an ISA and a set of constraints
  def apply(isa: InstructionSet, constraints: Seq[Constraint]): GeneratorContext ={
    val pc = new Counter
    val targets = new ListBuffer[LabelRecord]()
    GeneratorContext(
      isa,
      createInstructionGenerator(isa, constraints),
      createMemoryAddressGenerator(isa, constraints),
      createIOAddressGenerator(isa, constraints),
      createJumpTargetGenerator(pc,targets),
      pc,
      new Counter,
      targets
    )
  }

  /**
    * The instruction generator gives access to a distribution driven instruction stream.
    * Additional white and black list constraints can be added when sampling.
    */
  private def createInstructionGenerator(isa: InstructionSet, constraints: Seq[Constraint]): Seq[Constraint] => InstructionFactory with Categorizable = { additionalConstraints =>
    // get all applicable instruction constraints
    val allCons = (constraints ++ additionalConstraints).collect { case x: InstructionConstraint => x }

    // apply black and white lists to find allowed categories
    val allowedCategories: Seq[Category] = allCons.foldLeft(Category.all) {
      case (cats, CategoryBlackList(bl@_*)) => cats.filter(!bl.contains(_))
      case (cats, CategoryWhiteList(w@_*)) => cats.filter(w.contains(_))
      case (cats, _) => cats
    }

    val blacklisted = allCons.collect { case CategoryBlackList(bl@_*) => bl}.flatten

    // find all distribution constraints
    val distConstraints = allCons.collect { case x: CategoryDistribution => x }.flatMap(_.dis)

    if (distConstraints.nonEmpty) {
      // sort out black listed categories
      val filtered = distConstraints.filter { case (c, _) => allowedCategories.contains(c) }

      // add the label category, to always make random labels appear
      val withLabel = filtered ++ Seq((Category.Label,filtered.map(_._2).sum * 0.15))

      // create uniform distributions with all applicable instructions for each category
      val distributionConstraints = withLabel.map { case (c, d) =>
        val instructions = (isa.instructions ++ Seq(Label())).filter(i => i.isOfCategory(c) && !i.isOfOneOfCategories(blacklisted))
        (instructions, d)
      }.filter(_._1.nonEmpty).map { case (instr, d) =>
        (discreteUniform(instr).sample(1).head, d)
      }

      // create the top level distribution which contains the category weights
      discrete(distributionConstraints: _*).sample(1).head
    } else {
      // no distribution was supplied and all allowed instructions are distributed uniformly
      randomSelect((isa.instructions ++ Seq(Label())).filter(i => i.isOfOneOfCategories(allowedCategories) && !i.isOfOneOfCategories(blacklisted)))
    }


  }

  /**
    * The memory address generator can be sampled to produce new memory addresses.
    * A distribution can be defined when the sampler is created.
    */
  private def createMemoryAddressGenerator(isa: InstructionSet, constraints: Seq[Constraint]): Seq[Constraint] => BigInt = { additionalConstraints =>
    val dis = constraints.collect { case x: MemoryDistribution => x }.toList match {
      case x :: _ => x.dis
      case _ => Seq(isa.memoryAddressSpace -> 1.0)
    }
    discrete(dis.map { case (r, d) => (rand(r), d) }: _*).sample(1).head
  }

  /**
    * The I/O address generator can be sampled to produce new I/O port addresses.
    * A distribution can be defined when the samples is created.
    */
  private def createIOAddressGenerator(isa: InstructionSet, constraints: Seq[Constraint]): Seq[Constraint] => BigInt = { additionalConstraints =>
    val dis = constraints.collect { case x: IODistribution => x }.toList match {
      case x :: _ => x.dis
      case _ => Seq(isa.inputOutputAddressSpace -> 1.0)
    }
    discrete(dis.map { case (r, d) => (rand(r), d) }: _*).sample(1).head
  }

  /**
    * The jump target generator produces references to defined symbolic labels in the produced assembly code
    */
  private def createJumpTargetGenerator(pc: Counter, targets: ListBuffer[LabelRecord]): () => LabelRecord = { () =>
    if(targets.nonEmpty) randomSelect(targets) else LabelRecord("RANDOM_LABEL_0")
  }
}

object ProgramGenerator {
  def apply(isa: InstructionSet)(constraints: Constraint*): ProgramGenerator = {
    new ProgramGenerator(GeneratorContext(isa, constraints))
  }
}

/**
  * Wrapper for the generated instruction sequence.
  *
  * Gives access to some distribution statistics and printing as well as saving to file methods
  */
case class Program(instructions: Seq[Instruction], isa: InstructionSet) {
  // returns the assembly code
  override def toString: String = instructions.map(_.toAsm).map { str =>
    if (str.contains(':')) str else "  " + str
  }.mkString("\n")

  // assembly code + instruction and category histograms
  def pretty: String = {
    s"$this\n\nInstruction histogram:\n${this.histogram.map(_.toString()).mkString(", ")}\nCategory histogram:\n${this.categoryHistogram.map(_.toString()).mkString(", ")}\n"
  }

  // the number of occurrences for each instruction in the program
  def histogram: Seq[(String, Int)] = {
    instructions
      .map(_.toAsm.split(" ").head)
      .filter(!_.contains(':'))
      .distinct.map { instr =>
      instr -> instructions.count(_.toAsm.split(" ").head == instr)
    }
  }

  // the number of instructions matching the different categories
  def categoryHistogram: Seq[(String, Int)] = {
    Category.all.map { c =>
      (c.toString, instructions.count(_.categories.contains(c)))
    }
  }

  // export the assembly code to a file
  def saveToFile(fileName: String): Unit = {
    val writer = new PrintWriter(new File(fileName))
    writer.write(toString+"\n")
    writer.close()
  }
}

/**
  * A program generator object is characterized by its constraints which are applied to every
  * generation call. A specific seed can be provided when generating a new sequence.
  */
class ProgramGenerator(context: GeneratorContext) {

  // generate approximately n instructions using the passed seed
  def generate(n: Int, seed: Long): Program = {
    Random.setSeed(seed)
    Program(Seq.fill(n)(context.nextInstruction(Seq()).produce()(context)).flatten, context.isa)
  }

  // generate approximately n instructions using a random seed
  def generate(n: Int): Program = {
    generate(n,Random.nextLong())
  }

  // generate an instruction sequence based on a pattern using the passed seed
  def generate(p: Pattern, seed: Long): Program = {
    Random.setSeed(seed)
    Program(p.produce()(context), context.isa)
  }

  // generate an instruction sequence based on a pattern with a random seed
  def generate(p: Pattern): Program = {
    generate(p,Random.nextLong())
  }

}
