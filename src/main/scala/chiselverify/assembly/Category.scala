package chiselverify.assembly

trait Categorizable {
  val categories: Seq[Category]
  def isOfCategory(cat: Category): Boolean = categories.contains(cat)
  def isOfOneOfCategories(cats: Seq[Category]): Boolean = cats.map(isOfCategory).reduce(_ || _)
}
trait Category
object Category {
  val all = Seq(Arithmetic,Logical,Load,Store,Input,Output,Jump,Branch,EnvironmentCall,Nop)
  case object Arithmetic extends Category
  case object Logical extends Category
  case object Load extends Category
  case object Store extends Category
  case object Input extends Category
  case object Output extends Category
  case object Jump extends Category
  case object Branch extends Category
  case object EnvironmentCall extends Category
  case object Nop extends Category
}
