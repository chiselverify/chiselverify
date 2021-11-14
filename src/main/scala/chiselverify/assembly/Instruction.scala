package chiselverify.assembly

import chiselverify.assembly.RandomHelpers.randomSelect

object Instruction {
  // chooses one of the passed instructions or patterns at random when a new program is generated
  def select(instructions: InstructionFactory*)(implicit context: GeneratorContext): InstructionFactory = {
    InstructionFactory(c => randomSelect(instructions).produce())
  }

  // chooses a random instruction or pattern matching the given category when a new program is generated
  def ofCategory(category: Category)(implicit context: GeneratorContext): InstructionFactory = {
    InstructionFactory(c => c.nextInstruction(Seq(CategoryWhiteList(category))).produce())
  }

  // fill in n random instructions when a new program is generated
  def fill(n: Int)(implicit context: GeneratorContext): InstructionFactory = {
    InstructionFactory(c => Seq.fill(n)(c.nextInstruction(Seq())))
  }

  // fill in n random instructions, all of the given category, when a new program is generated
  def fillWithCategory(n: Int)(category: Category)(implicit context: GeneratorContext): InstructionFactory = {
    InstructionFactory(c => Seq.fill(n)(c.nextInstruction(Seq(CategoryWhiteList(category)))).flatMap(_.produce()))
  }
}

/**
  * Instructions are the leaves in the program generation call tree. They provide a copy of themselves
  * when the produce() method is invoked.
  * Instructions keep track of their address through a mutable variable for now in order to delay
  * the definition. Like this it can be hidden in the ISA definition
  */
abstract class Instruction(val categories: Category*) extends InstructionFactory with Categorizable {
  private var addr: BigInt = 0

  def getAddress: BigInt = addr

  def setAddress(newAddr: BigInt): Unit =
    addr = newAddr

  def apply(): Instruction

  def toAsm: String

  override def produce()(implicit context: GeneratorContext): Seq[Instruction] = {
    val instr = apply()
    instr.addr = context.pc.inc()
    Seq(instr)
  }
}