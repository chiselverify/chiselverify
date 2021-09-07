package chiselverify.assembly

trait Category

object Category extends DummyEnum[Category] {
  val values = Seq(LoadInstruction,ArithmeticInstruction,StoreInstruction,BranchInstruction,JumpInstruction)
  case object LoadInstruction extends Category

  case object ArithmeticInstruction extends Category

  case object StoreInstruction extends Category

  case object BranchInstruction extends Category

  case object JumpInstruction extends Category
}