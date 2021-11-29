package chiselverify.assembly

/**
  * Trait common to all instruction like objects which can be categorized using the category model.
  * Comes with category membership utility functions
  */
trait Categorizable {
  val categories: Seq[Category]

  def isOfOneOfCategories(cats: Seq[Category]): Boolean = cats.nonEmpty && cats.map(isOfCategory).reduce(_ || _)

  def isOfCategory(cat: Category): Boolean = categories.contains(cat)
}

trait Category

/**
  * The common category container
  */
object Category {
  val all = Seq(Arithmetic, Logical, Load, Store, Input, Output, JumpAndLink, Branch, EnvironmentCall, Nop, Immediate, Label, Compare, StateRegister, Synchronization)

  case object Arithmetic extends Category

  case object Logical extends Category

  case object Load extends Category

  case object Store extends Category

  case object Input extends Category

  case object Output extends Category

  case object JumpAndLink extends Category

  case object Branch extends Category

  case object EnvironmentCall extends Category

  case object Nop extends Category

  case object Immediate extends Category

  case object Label extends Category

  case object Compare extends Category

  case object StateRegister extends Category

  case object Synchronization extends Category
}
