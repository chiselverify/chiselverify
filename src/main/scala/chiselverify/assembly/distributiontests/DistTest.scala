package chiselverify.assembly.distributiontests

import probability_monad.Distribution._
import probability_monad._

object DistTest extends App {
  def bayesianCoin(nflips: Int): Distribution[Trial] = {
    for {
      haveFairCoin <- tf()
      c = if (haveFairCoin) coin else biasedCoin(0.9)
      flips <- c.repeat(nflips)
    } yield Trial(haveFairCoin, flips)
  }

  def dis = discrete(
    0 -> 0.5,
    1 -> 0.2,
    2 -> 0.3
  ).sample(1).head

  println(bayesianCoin(5).given(_.flips.forall(_ == H)).pr(_.haveFairCoin))

  def s = discrete(
    Seq(
      (100 until 150) -> 0.2,
      (1000 until 1002) -> 0.5,
      (3000 until 3001) -> 0.0
    ).map { case (range, p) =>
      discrete(range.map(i => (i, 1.0 / range.length)): _*) -> p
    }: _*
  )

  println(Seq.fill(10)(dis))

  case class Trial(haveFairCoin: Boolean, flips: List[Coin])

  println(Seq.fill(20)(s.sample(1).head.sample(1).head))
}
