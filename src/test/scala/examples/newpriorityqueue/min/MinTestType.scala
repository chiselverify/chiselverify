package examples.newpriorityqueue.min

import chisel3._
import examples.newpriorityqueue.util.OrderedData

class MinTestType(width: Int) extends Bundle with OrderedData[MinTestType] {

  val value = UInt(width.W)
  val valid = Bool()
  val isParent = Bool()

  override def isSmallerThan(that: MinTestType): Bool = {
    val selectLeft = Wire(Bool())
    when(!valid && !that.valid) {
      selectLeft := isParent
    }.elsewhen(valid && !that.valid) {
      selectLeft := 1.B
    }.elsewhen(!valid && that.valid) {
      selectLeft := 0.B
    }.otherwise {
      when(value === that.value) {
        selectLeft := isParent
      }.otherwise{
        selectLeft := value < that.value
      }
    }
    selectLeft
  }

  override def isGreaterThan(that: MinTestType): Bool = that.isSmallerThan(this)

  override def toString(): String = if(valid.litToBooleanOption.isDefined) s"${if(valid.litToBoolean) value.litValue else "invalid"}${if(isParent.litToBoolean) " (parent)" else ""}" else ""
  override def cloneType: MinTestType.this.type = (new MinTestType(width)).asInstanceOf[this.type]


}
