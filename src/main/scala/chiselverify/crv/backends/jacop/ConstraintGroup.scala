package chiselverify.crv.backends.jacop

class ConstraintGroup(group: Constraint*) extends chiselverify.crv.ConstraintGroup {
  /** 
    * List of all the constraints declared in the group
    */
  override val constraints: List[Constraint] = group.toList
}
