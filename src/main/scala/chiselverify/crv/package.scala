package chiselverify

package object crv {
  final case class CRVException(private val message: String = "", private val cause: Throwable = None.orNull)
    extends Exception(message, cause)

}
