package chiselverify.assembly

object Pattern {
  def apply(instructions: GeneratorContext => Seq[InstructionFactory]): Pattern = new Pattern(instructions)
  def apply(categories: Category*)(instructions: GeneratorContext => Seq[InstructionFactory]): InstructionFactory with Categorizable = {
    new Pattern(instructions).giveCategory(categories:_*)
  }

  def repeat(n: Int)(p: Pattern): InstructionFactory = {
    InstructionFactory(c => Seq.fill(n)(p).flatMap(_.produce()(c)))
  }
}
class Pattern(instructions: GeneratorContext => Seq[InstructionFactory]) extends InstructionFactory {
  override def produce()(implicit context: GeneratorContext): Seq[Instruction] = instructions(context).flatMap(_.produce())

  def giveCategory(cat: Category*): InstructionFactory with Categorizable = {
    val originalProduce = (c: GeneratorContext) => this.produce()(c)
    new InstructionFactory with Categorizable {
      override val categories: Seq[Category] = cat
      override def produce()(implicit context: GeneratorContext): Seq[Instruction] = originalProduce(context)
    }
  }
}
