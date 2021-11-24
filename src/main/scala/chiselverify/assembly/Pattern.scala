package chiselverify.assembly

/**
  * Patterns allow to describe a sequence of instruction producing objects.
  * This means a pattern can include actual instructions or other nested patterns
  * which produce instruction sequences themselves.
  *
  * The pattern sequence itself is evaluated first at program generation time by passing
  * a generator context which contains constraints as an argument when calling produce() on all sequence members. This
  * creates a call tree with instructions forming the leaf nodes.
  */
object Pattern {
  // Pattern factory
  def apply(instructions: GeneratorContext => Seq[InstructionFactory]): Pattern = new Pattern(instructions)

  // Pattern factory where the pattern is associated with a category
  def apply(categories: Category*)(instructions: GeneratorContext => Seq[InstructionFactory]): InstructionFactory with Categorizable = {
    new Pattern(instructions).giveCategory(categories: _*)
  }

  // Produces n repetitions of the given instruction producing object
  def repeat(n: Int)(p: InstructionFactory): InstructionFactory = {
    InstructionFactory(c => Seq.fill(n)(p).flatMap(_.produce()(c)))
  }
}

class Pattern(instructions: GeneratorContext => Seq[InstructionFactory]) extends InstructionFactory {

  // allows to attach a set of categories to a pattern
  def giveCategory(cat: Category*): InstructionFactory with Categorizable = {
    val originalProduce = (c: GeneratorContext) => this.produce()(c)
    new InstructionFactory with Categorizable {
      override val categories: Seq[Category] = cat

      override def produce()(implicit context: GeneratorContext): Seq[Instruction] = originalProduce(context)
    }
  }
  // produce means invoking produce on all pattern elements and flattening them
  override def produce()(implicit context: GeneratorContext): Seq[Instruction] = instructions(context).flatMap(_.produce())
}
