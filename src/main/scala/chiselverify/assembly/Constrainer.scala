package chiselverify.assembly

import probability_monad.Distribution.discrete


trait InstructionProducer {
  def nextInstruction(): Instruction
}

abstract class Constrainer[ISA <: InstructionSet](constraints: ConstraintContainer[ISA])

class BasicConstrainer[ISA <: InstructionSet](isa: ISA, constraints: ConstraintContainer[ISA])
  extends Constrainer[ISA](constraints) with InstructionProducer {

  def sample[T](seq: Seq[T]): T = seq.apply(scala.util.Random.nextInt(seq.length))

  def inDis = discrete(constraints.categoryDistribution.map { case (cat,p) =>
    sample(isa.values.filter(_.categories.contains(cat))) -> p
  }:_*)

  def nextInstruction(): Instruction = {
    val instruction = inDis.sample(1).head.apply()
    instruction.fields.foreach { f =>
      f.fieldType match {
        case InstructionField.RegisterField =>
          f.setValue(sample(constraints.registers).toInt)
          f.string = "x" + f.value.get.toString(10)
        case _ => f.setValue(f.domain.sample())
      }
    }
    instruction
  }
}