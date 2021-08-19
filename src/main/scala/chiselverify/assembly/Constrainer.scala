package chiselverify.assembly


trait InstructionProducer {
  def nextInstruction(): Instruction
}

abstract class Constrainer[ISA <: InstructionSet](constraints: ConstraintBuilder[ISA])

class BasicConstrainer[ISA <: InstructionSet](isa: ISA, constraints: ConstraintBuilder[ISA])
  extends Constrainer[ISA](constraints) with InstructionProducer {

  def nextInstruction(): Instruction = {
    val instruction = isa.pick().apply()
    instruction.registerFields.foreach(f => f.setValue(isa.registers.pick()))
    instruction.addressOffsetFields.foreach(f => f.setValue(f.domain.randInRange()))
    instruction.constantFields.foreach(f => f.setValue(f.domain.randInRange()))
    instruction.branchOffsetFields.foreach(f => f.setValue(f.domain.randInRange()))
    instruction
  }
}