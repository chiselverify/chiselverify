package chiselverify.assembly

import scala.collection.mutable.ArrayBuffer

class FieldFactory[IF <: InstructionField[T], T](gen: Int => IF) {
  def apply(width: Int)(implicit record: ArrayBuffer[IF]): IF = {
    val field = gen(width)
    record.append(field)
    return field
  }
}

case class Domain(lower: BigInt, upper: BigInt) {
  def randInRange(): BigInt = scala.util.Random.nextInt((upper - lower).toInt) + lower
}

abstract class InstructionField[T] {
  val width: Int
  val domain: Domain = Domain(0, scala.math.pow(2, width).toInt)
  var value: Option[T] = None

  def setValue(value: T): Unit = this.value = Some(value)

  override def toString: String = s"${value.getOrElse("None")}"
}

class AddressField(val width: Int) extends InstructionField[BigInt]

object AddressField extends FieldFactory[AddressField, BigInt](new AddressField(_))

class RegisterField(val width: Int) extends InstructionField[Register]

object RegisterField extends FieldFactory[RegisterField, Register](new RegisterField(_))

class ConstantField(val width: Int) extends InstructionField[BigInt]

object ConstantField extends FieldFactory[ConstantField, BigInt](new ConstantField(_))

class AddressOffsetField(val width: Int) extends InstructionField[BigInt]

object AddressOffsetField extends FieldFactory[AddressOffsetField, BigInt](new AddressOffsetField(_))

class BranchOffsetField(val width: Int) extends InstructionField[BigInt]

object BranchOffsetField extends FieldFactory[BranchOffsetField, BigInt](new BranchOffsetField(_))