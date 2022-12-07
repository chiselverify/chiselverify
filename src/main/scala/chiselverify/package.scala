package object chiselverify {
  /**
    * Implicit type conversion from string to option to simplify syntax
    *
    * @param s the string that will be converted
    * @return an option containing the given string
    */
  implicit def stringToOption(s: String): Option[String] = Some(s)

  /**
    * Implicit type conversion from int to option to simplify syntax
    *
    * @param i the int that will be converted
    * @return an option containing the given int
    */
  implicit def bigIntToOption(i: BigInt): Option[BigInt] = Some(i)
}
