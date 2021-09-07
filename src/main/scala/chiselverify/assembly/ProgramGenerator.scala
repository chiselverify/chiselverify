package chiselverify.assembly

class ProgramGenerator[ISA <: InstructionSet, C <: Constrainer[ISA] with InstructionProducer](isa: ISA, constrainer: C) {
  def generate(n: Int): Seq[Instruction] = Seq.fill(n)(constrainer.nextInstruction())
}



object ProgramGenerator {
  def getDefaultCategoryDist[ISA <: InstructionSet](isa: ISA): Seq[(Category, Double)] = {
    Category.values.map((_, 1.0/Category.values.length))
  }
  def apply[
    ISA <: InstructionSet,
  ](isa: ISA)(constrains: ConstraintContainer[ISA] => Unit): ProgramGenerator[ISA, Constrainer[ISA] with InstructionProducer] = {
    val con = ConstraintContainer(isa,isa.registers.values,isa.values,getDefaultCategoryDist(isa),Seq((isa.reachableMemory.toRange,1.0)))
    constrains(con)
    val constrainer = new BasicConstrainer(isa, con)
    new ProgramGenerator(isa, constrainer)
  }
}




