/**
  * Code taken from https://gist.github.com/kylewlacy/38681caaee2b98949dd8
  * @author Kyle Lacy (username: @kylewlacy)
  */

package chiselverify.coverage
import scala.reflect.ClassTag

object Utils {
    /**
      * Given an List containing a partial Cartesian product, and an List of
      * items, return an List adding the list of items to the partial
      * Cartesian product.
      *
      * {{{
      * val partialProduct = List(List(1, 4), List(1, 5), List(2, 4), List(2, 5))
      * val items = List(6, 7)
      * partialCartesian(partialProduct, items) ==
      * List(List(1, 4, 6),
      *       List(1, 4, 7),
      *       List(1, 5, 6),
      *       List(1, 5, 7),
      *       List(2, 4, 6),
      *       List(2, 4, 7),
      *       List(2, 5, 6),
      *       List(2, 5, 7))
      * }}}
      */
    private def partialCartesian[T: ClassTag](a: List[List[T]], b: List[T]):
    List[List[T]] = {
        a.flatMap(xs => {
            b.map(y => {
                xs ++ List(y)
            })
        })
    }

    /**
      * Computes the Cartesian product of lists[0] * lists[1] * ... * lists[n].
      *
      * {{{
      * scala> import CartesianProduct._
      * scala> val lists = List(List(1, 2), List(4, 5), List(6, 7));
      * scala> cartesianProduct(lists)
      * scala> cartesianProduct(lists)
      * res0: List[List[Int]] = List(List(1, 4, 6),
      *                                 List(1, 4, 7),
      *                                 List(1, 5, 6),
      *                                 List(1, 5, 7),
      *                                 List(2, 4, 6),
      *                                 List(2, 4, 7),
      *                                 List(2, 5, 6),
      *                                 List(2, 5, 7))
      * }}}
      */
    def cartesianProduct[T: ClassTag](lists: List[List[T]]): List[List[T]] = {
        lists.headOption match {
            case Some(head) => {
                val tail = lists.tail
                val init = head.map(n => List(n))

                tail.foldLeft(init)((arr, list) => {
                    partialCartesian(arr, list)
                })
            }
            case None => {
                Nil
            }
        }
    }
}
