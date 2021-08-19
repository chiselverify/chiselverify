package chiselverify.assembly

class ConstraintBuilder[ISA <: InstructionSet](isa: ISA) {
  def withRegisters(cond: isa.Registers => Seq[isa.RegisterType]): this.type = {
    this
  }

  def withMemoryAccessDistribution(dis: (Range, Double)*): this.type = {
    this
  }

  def withInstructions(cond: ISA => Seq[Instruction]): this.type = {
    this
  }

  def withCategoryDistribution(dis: (Category, Double)*): this.type = {
    this
  }

  def withNoBranches: this.type = this

  def withNoJumps: this.type = this
}