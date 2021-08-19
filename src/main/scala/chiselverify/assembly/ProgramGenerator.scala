package chiselverify.assembly

class ProgramGenerator[ISA <: InstructionSet, C <: Constrainer[ISA] with InstructionProducer](isa: ISA, constrainer: C) {
  def generate(n: Int): Seq[Instruction] = Seq.fill(n)(constrainer.nextInstruction())
}

object ProgramGenerator {
  def apply[
    ISA <: InstructionSet,
  ](isa: ISA)(constrains: ConstraintBuilder[ISA] => Unit): ProgramGenerator[ISA, Constrainer[ISA] with InstructionProducer] = {
    val con = new ConstraintBuilder(isa)
    constrains(con)
    val constrainer = new BasicConstrainer(isa, con)
    new ProgramGenerator(isa, constrainer)
  }
}




