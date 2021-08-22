package chiselverify.assembly

import scala.collection.mutable.ArrayBuffer

trait InstructionFieldType
object InstructionField {
  object RegisterField extends InstructionFieldType
  object ConstantField extends InstructionFieldType
  object AddressField extends InstructionFieldType
  object AddressOffsetField extends InstructionFieldType
  object BranchOffsetField extends InstructionFieldType
}

class FieldFactory(gen: Int => InstructionField) {
  def apply(width: Int)(implicit fieldRecord: ArrayBuffer[InstructionField]): InstructionField = {
    val field = gen(width)
    fieldRecord.append(field)
    return field
  }
}

case class Domain(lower: BigInt, upper: BigInt) {
  def sample(): BigInt = scala.util.Random.nextInt((upper - lower).toInt) + lower
  def contains(that: BigInt): Boolean = (lower until upper).contains(that)
}

class InstructionField(val width: Int, val fieldType: InstructionFieldType) {
  val domain: Domain = Domain(0, scala.math.pow(2, width).toInt)
  var value: Option[BigInt] = None
  var string = ""

  def setValue(value: BigInt): Unit = {
    if(!domain.contains(value)) throw new Exception("The given value lies outside of the field domain")
    this.value = Some(value)
    string = "0x"+this.value.get.toString(16)
  }

  override def toString: String = string
}


object RegisterField extends FieldFactory(new InstructionField(_,InstructionField.RegisterField))
object ConstantField extends FieldFactory(new InstructionField(_,InstructionField.ConstantField))
object AddressField extends FieldFactory(new InstructionField(_,InstructionField.AddressField))
object AddressOffsetField extends FieldFactory(new InstructionField(_,InstructionField.AddressOffsetField))
object BranchOffsetField extends FieldFactory(new InstructionField(_,InstructionField.BranchOffsetField))