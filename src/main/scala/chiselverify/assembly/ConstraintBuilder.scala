package chiselverify.assembly

class ConstraintBuilder[ISA <: InstructionSet](isa: ISA) {

  var registers = isa.registers.values
  var instructions = isa.values
  var categoryDis: Seq[(Category, Double)] = Seq()

  def withRegisters(cond: isa.Registers => Seq[isa.RegisterType]): this.type = {
    registers = cond(isa.registers.asInstanceOf[isa.Registers])
    this
  }

  def withMemoryAccessDistribution(dis: (Range, Double)*): this.type = {
    this
  }

  def withInstructions(cond: ISA => Seq[Instruction]): this.type = {
    instructions = cond(isa)
    this
  }

  def withCategoryDistribution(dis: (Category, Double)*): this.type = {
    categoryDis = dis
    this
  }

  def withNoBranches: this.type = this

  def withNoJumps: this.type = this
}