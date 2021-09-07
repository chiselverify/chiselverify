package chiselverify.assembly

case class ConstraintContainer[ISA <: InstructionSet](isa: ISA,
  registers: Seq[Register],
  instructions: Seq[Instruction],
  categoryDistribution: Seq[(Category, Double)],
  memoryAccessDistribution: Seq[(Range, Double)]
) {

  def withRegisters(cond: isa.Registers => Seq[isa.RegisterType]): ConstraintContainer[ISA] =
    ConstraintContainer(isa,
      cond(isa.registers.asInstanceOf[isa.Registers]),
      instructions,
      categoryDistribution,
      memoryAccessDistribution)

  def withMemoryAccessDistribution(dis: (Range, Double)*): ConstraintContainer[ISA] =
    ConstraintContainer(isa,
      registers,
      instructions,
      categoryDistribution,
      dis)

  def withInstructions(cond: ISA => Seq[Instruction]): ConstraintContainer[ISA] = {
    ConstraintContainer(isa,
      registers,
      cond(isa),
      categoryDistribution,
      memoryAccessDistribution)
  }

  def withCategoryDistribution(dis: (Category, Double)*): ConstraintContainer[ISA] = {
    ConstraintContainer(isa,
      registers,
      instructions,
      dis,
      memoryAccessDistribution)
  }

  def withNoBranches: ConstraintContainer[ISA] =
    ConstraintContainer(isa,
      registers,
      instructions.filter(!_.categories.contains(Category.BranchInstruction)),
      categoryDistribution,
      memoryAccessDistribution)

  def withNoJumps: ConstraintContainer[ISA] =
    ConstraintContainer(isa,
      registers,
      instructions.filter(!_.categories.contains(Category.JumpInstruction)),
      categoryDistribution,
      memoryAccessDistribution)
}