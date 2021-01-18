package chiselverify.crv

trait RandObj {


  /** Randomize the current object
    * @return Boolean returns true only if a solution was found
    */
  def randomize: Boolean

  /** Method containing a set of directive to run before the current object is randomized
    */
  def preRandomize(): Unit = {}

  /** Method containing a set of directive to run after the current object is randomized
    */
  def postRandomize(): Unit = {}
}
