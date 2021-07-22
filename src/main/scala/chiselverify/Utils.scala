package chiselverify

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

    //Hardcoded cartesian for better performance
    def cartesian(ranges: List[List[Int]]): List[List[Int]] = ranges.length match {
        case 1 => ranges
        case 2 => for (r1 <- ranges.head; r2 <- ranges.tail.head) yield List(r1, r2)
        case 3 => for (x1 <- ranges.head; x2 <- ranges.tail.head; x3 <- ranges.tail.tail.head)
            yield List(x1, x2, x3)
        case 4 => for {
            x1 <- ranges.head
            x2 <- ranges.tail.head
            x3 <- ranges.tail.tail.head
            x4 <- ranges.tail.tail.tail.head
        } yield List(x1, x2, x3, x4)
        case _ => throw new IllegalArgumentException("MAX ARRAY SIZE IN CARTESIAN PRODUCT IS 4")
    }

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

    def randName(length: Int, seed: Int) : String = {
        import scala.util.Random
        val rand = new Random(seed)
        val res: StringBuilder = new StringBuilder()
        (0 until length).foreach(_ => {
            val randNum = rand.nextInt(122 - 48) + 48
            res += randNum.toChar
        })
        res.mkString
    }
}
