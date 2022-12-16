package chiselverify.crv.backends.jacop

class JaCoPConstraintGroup(group: JaCoPConstraint*) extends chiselverify.crv.CRVConstraintGroup {
  /** 
    * List of all the constraints declared in the group
    */
  override val constraints: List[JaCoPConstraint] = group.toList
}
