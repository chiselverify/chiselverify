package chiselverify.assembly

import chiselverify.assembly.RandomHelpers.randomSelect

object Instruction {
  def select(instructions: InstructionFactory*)(implicit context: GeneratorContext): InstructionFactory = {
    InstructionFactory(c => randomSelect(instructions).produce())
  }

  def ofCategory(category: Category)(implicit context: GeneratorContext): InstructionFactory = {
    InstructionFactory(c => c.nextInstruction(Seq(CategoryWhiteList(category))).produce())
  }

  def fill(n: Int)(implicit context: GeneratorContext): InstructionFactory = {
    InstructionFactory(c => Seq.fill(n)(c.nextInstruction(Seq())))
  }

  def fillWithCategory(n: Int)(category: Category)(implicit context: GeneratorContext): InstructionFactory = {
    InstructionFactory(c => Seq.fill(n)(c.nextInstruction(Seq(CategoryWhiteList(category)))).flatMap(_.produce()))
  }
}

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