package chiselverify.approximation

object Metrics {

  /** 
    * Represents the result type of a generic error metric of which there are two types:
    * 
    * - Absolute metrics that return non-normalized results
    * - Relative metrics that return normalized results
    */
  private[chiselverify] trait MetricResult {
    /** 
      * Checks if the metric is absolute
      * @return true if the metric is absolute
      */
    def isAbsolute: Boolean

    /** 
      * Checks if the metric is relative
      * @return true if the metric is relative
      */
    final def isRelative: Boolean = !isAbsolute
  }

  /** 
    * Represents an absolute `MetricResult`
    */
  trait Absolute extends MetricResult {
    final def isAbsolute: Boolean = true
  }

  /** 
    * Represents a relative `MetricResult`
    */
  trait Relative extends MetricResult {
    final def isAbsolute: Boolean = false
  }

  /** 
    * Represents a generic error metric of which there are two types:
    * 
    * - Instantaneous metrics that require only two samples. They define at least:
    *   * `compute(v1: BigInt, v2: BigInt)` to calculate the metric given two samples
    * - History-based metrics that require a sequence of samples. They define at least:
    *   * `compute(vs1: Iterable[BigInt], vs2: Iterable[BigInt])` to calculate the metric 
    *     given two sequences of samples
    *   * `compute(vss: Iterable[(BigInt, BigInt)])` to calculate the metric given a 
    *     sequence of tuples of samples
    * 
    * @todo make type-generic (unsure whether the current code will fail for signed operands)
    */
  private[chiselverify] sealed trait Metric {
    def maxVal: Option[Double]

    /** 
      * Checks if the metric is constrained
      * @return true if the metric has a maximum value
      */
    final def isConstrained: Boolean = maxVal.isDefined

    /** 
      * Checks if the value of the metric is less than a given maximum
      * @param err value of the metric
      * @return true if `maxVal` is `None` or if `err` is less than `maxVal`
      */
    final def check(err: Double): Boolean = maxVal.isEmpty || err <= maxVal.get

    def compute(vs1: Iterable[BigInt], vs2: Iterable[BigInt]): Iterable[Double]

    /**
      * Computes the values of the metric given a sequence of tuples of samples
      * @param vss sequence of tuples of samples
      * @return values of the metric
      */
    final def compute(vss: Iterable[(BigInt, BigInt)]): Iterable[Double] = {
      val (vs1, vs2) = vss.unzip
      compute(vs1, vs2)
    }

  }

  /** 
    * Represents an instantaneous `Metric`
    * 
    * @note Inheriting classes must mixin either 
    */
  abstract class Instantaneous(maxVal: Option[Double]) extends Metric {
    this: MetricResult =>
    
    /** 
      * Computes the value of the metric given two samples
      * @param v1 first sample
      * @param v2 second sample
      * @return value of the metric
      */
    def compute(v1: BigInt, v2: BigInt): Double

    /** 
      * Checks if the value of the metric given two samples is less than its maximum
      * @return `check(compute(v1, v2))`
      */
    final def check(v1: BigInt, v2: BigInt): Boolean = check(compute(v1, v2))

    /** 
      * Computes the values of the metric given two sequences of samples
      * @param vs1 first sequence of samples
      * @param vs2 second sequence of samples
      * @return values of the metric
      */
    final def compute(vs1: Iterable[BigInt], vs2: Iterable[BigInt]): Iterable[Double] = {
      val (seq1, seq2) = (vs1.toSeq, vs2.toSeq)
      require(seq1.length == seq2.length, "the sequences must be the same length")
      vs1.zip(vs2).map { case (v1, v2) => compute(v1, v2) }
    }

    /** 
      * Checks if the values of the metric given two sequences of samples are less than its maximum
      * @return `check(compute(v1, v2))` for each `(v1, v2)` in `vs1.zip(vs2)`
      */
    final def check(vs1: Iterable[BigInt], vs2: Iterable[BigInt]): Boolean = compute(vs1, vs2).map(check).forall(s => s)



    /** 
      * Checks if the values of the metric given a sequence of tuples of samples are less than its maximum
      * @return `check(compute(v1, v2))` for each `(v1, v2)` in `vss`
      */
    final def check(vss: Iterable[(BigInt, BigInt)]): Boolean = compute(vss).map(check).forall(s => s)
  }

  /** 
    * Represents a history-based `Metric`
    */
  abstract class HistoryBased(maxVal: Option[Double]) extends Metric {
    this: MetricResult =>
    
    /** 
      * Computes the value of the metric given two sequences of samples
      * @param vs1 first sequence of samples
      * @param vs2 second sequence of samples
      * @return value of the absolute metric
      */
    def compute(vs1: Iterable[BigInt], vs2: Iterable[BigInt]): Iterable[Double]

    /** 
      * Checks if the value of the metric given two sequences of samples is less than its maximum
      * @return `check(compute(v1, v2))`
      */
    final def check(vs1: Iterable[BigInt], vs2: Iterable[BigInt]): Boolean = check(compute(vs1, vs2))

    /** 
      * Checks if the value of the metric given a sequence of tuples of samples is less than its maximum
      * @return `check(compute(vss))`
      */
    final def check(vss: Iterable[(BigInt, BigInt)]): Boolean = check(compute(vss))
  }

  /** 
    * Error distance metric
    * $$ed(v1, v2) = abs(v2 - v1)$$
    * 
    * @param maxVal [Optional] maximum value of the metric
    */
  final case class ED(maxVal: Option[Double] = None) extends Instantaneous(maxVal) with Absolute {
    def compute(v1: BigInt, v2: BigInt): Double = (v2 - v1).abs.toDouble
  }
  case object ED {
    /** 
      * Creates a new unconstrained error distance metric
      */
    def apply(): ED = new ED

    /** 
      * Creates a new constrained error distance metric
      * @param maxVal maximum value of the metric
      */
    def apply(maxVal: Double): ED = new ED(Some(maxVal))

    /** 
      * Creates a new error distance metric and computes its value on the two given samples
      * @param v1 first sample
      * @param v2 second sample
      * @return value of the metric
      */
    def apply(v1: BigInt, v2: BigInt): Double = (new ED).compute(v1, v2)
  }

  /** 
    * Squared error metric
    * $$se(v1, v2) = ed(v1, v2) ** 2$$
    * 
    * @param maxVal [Optional] maximum value of the metric
    */
  final case class SE(maxVal: Option[Double] = None) extends Instantaneous(maxVal) with Absolute {
    private val ed: ED = new ED
    def compute(v1: BigInt, v2: BigInt): Double = (ed.compute(v1, v2) * ed.compute(v1, v2)).toDouble
  }
  case object SE {
    /** 
      * Creates a new unconstrained squared error metric
      */
    def apply(): SE = new SE

    /** 
      * Creates a new constrained squared error metric
      * @param maxVal maximum value of the metric
      */
    def apply(maxVal: Double): SE = new SE(Some(maxVal))

    /** 
      * Creates a new squared error metric and computes its value on the two given samples
      * @param v1 first sample
      * @param v2 second sample
      * @return value of the metric
      */
    def apply(v1: BigInt, v2: BigInt): Double = (new SE).compute(v1, v2)
  }

  /** 
    * Hamming distance metric
    * $$hd(v1, v2) = popcount(v1 ^ v2)$$
    * 
    * @param maxVal [Optional] maximum value of the metric
    */
  final case class HD(maxVal: Option[Double] = None) extends Instantaneous(maxVal) with Absolute {
    def compute(v1: BigInt, v2: BigInt): Double = (v1 ^ v2).bitCount.toDouble
  }
  case object HD {
    /** 
      * Creates a new unconstrained Hamming distance metric
      */
    def apply(): HD = new HD

    /** 
      * Creates a new constrained Hamming distance metric
      * @param maxVal maximum value of the metric
      */
    def apply(maxVal: Double): HD = new HD(Some(maxVal))

    /** 
      * Creates a new Hamming distance metric and computes its value on the two given samples
      * @param v1 first sample
      * @param v2 second sample
      * @return value of the metric
      */
    def apply(v1: BigInt, v2: BigInt): Double = (new HD).compute(v1, v2)
  }

  /** 
    * Relative error distance metric
    * $$red(v1, v2) = ed(v1, v2) / v2$$
    * 
    * @param maxVal [Optional] maximum value of the metric
    */
  final case class RED(maxVal: Option[Double] = None) extends Instantaneous(maxVal) with Relative {
    private val ed: ED = new ED
    def compute(v1: BigInt, v2: BigInt): Double = ed.compute(v1, v2) / v2.toDouble
  }
  case object RED {
    /** 
      * Creates a new unconstrained relative error distance metric
      */
    def apply(): RED = new RED

    /** 
      * Creates a new constrained relative error distance metric
      * @param maxVal maximum value of the metric
      */
    def apply(maxVal: Double): RED = new RED(Some(maxVal))

    /** 
      * Creates a new relative error distance metric and computes its value on the two given samples
      * @param v1 first sample
      * @param v2 second sample
      * @return value of the metric
      */
    def apply(v1: BigInt, v2: BigInt): Double = (new RED).compute(v1, v2)
  }

  /** 
    * Error rate metric
    * $$er(vs1, vs2) = sum[i=0..len(vs1)-1](vs1[i] != vs2[i]) / len(vs1)$$
    * 
    * @param maxVal [Optional] maximum value of the metric
    */
  final case class ER(maxVal: Option[Double] = None) extends HistoryBased(maxVal) with Relative {
    def compute(vs1: Iterable[BigInt], vs2: Iterable[BigInt]): Iterable[Double] = {
      val (seq1, seq2) = (vs1.toSeq, vs2.toSeq)
      require(seq1.length == seq2.length, "the sequences must be the same length")
      (seq1.zip(seq2)).map { case (v1, v2) => if (v1 != v2) 1 else 0 }.sum.toDouble / seq1.length
    }
  }
  case object ER {
    /** 
      * Creates a new unconstrained error rate metric
      */
    def apply(): ER = new ER

    /** 
      * Creates a new constrained error rate metric
      * @param maxVal maximum value of the metric
      */
    def apply(maxVal: Double): ER = new ER(Some(maxVal))

    /** 
      * Creates a new error rate metric and computes its value on the two given sequences of samples
      * @param vs1 first sequence of samples
      * @param vs2 second sequence of samples
      * @return value of the metric
      */
    def apply(vs1: Iterable[BigInt], vs2: Iterable[BigInt]): Double = (new ER).compute(vs1, vs2)

    /** 
      * Creates a new error rate metric and computes its value on the given sequence of tuples of samples
      * @param vss sequence of tuples of samples
      * @return value of the metric
      */
    def apply(vss: Iterable[(BigInt, BigInt)]): Double = (new ER).compute(vss)
  }

  /** 
    * Mean error distance metric
    * $$med(vs1, vs2) = sum[i=0..len(vs1)-1](abs(vs2[i] - vs1[i])) / len(vs1)$$
    * 
    * @param maxVal [Optional] maximum value of the metric
    */
  final case class MED(maxVal: Option[Double] = None) extends HistoryBased(maxVal) with Absolute {
    private val ed: ED  = new ED
    def compute(vs1: Iterable[BigInt], vs2: Iterable[BigInt]): Iterable[Double] = {
      val (seq1, seq2) = (vs1.toSeq, vs2.toSeq)
      require(seq1.length == seq2.length, "the sequences must be the same length")
      (seq1.zip(seq2)).map { case (v1, v2) => ed.compute(v1, v2) }.sum / seq1.length
    }
  }
  case object MED {
    /** 
      * Creates a new unconstrained mean error distance metric
      */
    def apply(): MED = new MED

    /** 
      * Creates a new constrained mean error distance metric
      * @param maxVal maximum value of the metric
      */
    def apply(maxVal: Double): MED = new MED(Some(maxVal))

    /** 
      * Creates a new mean error distance metric and computes its value on the two given sequences of samples
      * @param vs1 first sequence of samples
      * @param vs2 second sequence of samples
      * @return value of the metric
      */
    def apply(vs1: Iterable[BigInt], vs2: Iterable[BigInt]): Double = (new MED).compute(vs1, vs2)

    /** 
      * Creates a new mean error distance metric and computes its value on the given sequence of tuples of samples
      * @param vss sequence of tuples of samples
      * @return value of the metric
      */
    def apply(vss: Iterable[(BigInt, BigInt)]): Double = (new MED).compute(vss)
  }

  /** 
    * Error distance variance metric
    * $$ved(vs1, vs2) = sum[i=0..len(vs1)-1]((ed(vs1[i], vs2[i]) - med(vs1, vs2)) ** 2) / len(vs1)$$
    * 
    * @param maxVal [Optional] maximum value of the metric
    */
  final case class VED(maxVal: Option[Double] = None) extends HistoryBased(maxVal) with Absolute {
    private val ed: ED = new ED
    def compute(vs1: Iterable[BigInt], vs2: Iterable[BigInt]): Iterable[Double] = {
      val (seq1, seq2) = (vs1.toSeq, vs2.toSeq)
      require(seq1.length == seq2.length, "the sequences must be the same length")
      val eds = seq1.zip(seq2).map { case (v1, v2) => ed.compute(v1, v2) }
      val med = eds.sum / eds.length
      eds.map(err => (err - med) * (err - med)).sum / eds.length
    }
  }
  case object VED {
    /** 
      * Creates a new unconstrained error distance variance metric
      */
    def apply(): VED = new VED

    /** 
      * Creates a new constrained error distance variance metric
      * @param maxVal maximum value of the metric
      */
    def apply(maxVal: Double): VED = new VED(Some(maxVal))

    /** 
      * Creates a new error distance variance metric and computes its value on the two given sequences of samples
      * @param vs1 first sequence of samples
      * @param vs2 second sequence of samples
      * @return value of the metric
      */
    def apply(vs1: Iterable[BigInt], vs2: Iterable[BigInt]): Double = (new VED).compute(vs1, vs2)
    
    /** 
      * Creates a new error distance variance metric and computes its value on the given sequence of tuples of samples
      * @param vss sequence of tuples of samples
      * @return value of the metric
      */
    def apply(vss: Iterable[(BigInt, BigInt)]): Double = (new VED).compute(vss)
  }

  /** 
    * Error distance standard deviation metric
    * $$sded(vs1, vs2) = sqrt(ved(vs1, vs2))$$
    * 
    * @param maxVal [Optional] maximum value of the metric
    */
  final case class SDED(maxVal: Option[Double] = None) extends HistoryBased(maxVal) with Absolute {
    private val ved: VED = new VED
    def compute(vs1: Iterable[BigInt], vs2: Iterable[BigInt]): Iterable[Double] = scala.math.sqrt(ved.compute(vs1, vs2))
  }
  case object SDED {
    /** 
      * Creates a new unconstrained error distance standard deviation metric
      */
    def apply(): SDED = new SDED

    /** 
      * Creates a new constrained error distance standard deviation metric
      * @param maxVal maximum value of the metric
      */
    def apply(maxVal: Double): SDED = new SDED(Some(maxVal))

    /** 
      * Creates a new error distance variance metric and computes its value on the two given sequences of samples
      * @param vs1 first sequence of samples
      * @param vs2 second sequence of samples
      * @return value of the metric
      */
    def apply(vs1: Iterable[BigInt], vs2: Iterable[BigInt]): Double = (new SDED).compute(vs1, vs2)

    /** 
      * Creates a new error distance variance metric and computes its value on the given sequence of tuples of samples
      * @param vss sequence of tuples of samples
      * @return value of the metric
      */
    def apply(vss: Iterable[(BigInt, BigInt)]): Double = (new SDED).compute(vss)
  }

  /** 
    * Mean squared error metric
    * $$mse(vs1, vs2) = sum[i=0..len(vs1)-1](se(vs1[i], vs2[i])) / len(vs1)$$
    * 
    * @param maxVal [Optional] maximum value of the metric
    */
  final case class MSE(maxVal: Option[Double] = None) extends HistoryBased(maxVal) with Absolute {
    private val se: SE = new SE
    def compute(vs1: Iterable[BigInt], vs2: Iterable[BigInt]): Iterable[Double] = {
      val (seq1, seq2) = (vs1.toSeq, vs2.toSeq)
      require(seq1.length == seq2.length, "the sequences must be the same length")
      (seq1.zip(seq2)).map { case (v1, v2) => se.compute(v1, v2) }.sum / seq1.length
    }
  }
  case object MSE {
    /** 
      * Creates a new unconstrained mean squared error metric
      */
    def apply(): MSE = new MSE

    /** 
      * Creates a new constrained mean squared error metric
      * @param maxVal maximum value of the metric
      */
    def apply(maxVal: Double): MSE = new MSE(Some(maxVal))

    /** 
      * Creates a new mean squared error metric and computes its value on the two given sequences of samples
      * @param vs1 first sequence of samples
      * @param vs2 second sequence of samples
      * @return value of the metric
      */
    def apply(vs1: Iterable[BigInt], vs2: Iterable[BigInt]): Double = (new MSE).compute(vs1, vs2)

    /** 
      * Creates a new mean squared error metric and computes its value on the given sequence of tuples of samples
      * @param vss sequence of tuples of samples
      * @return value of the metric
      */
    def apply(vss: Iterable[(BigInt, BigInt)]): Double = (new MSE).compute(vss)
  }

  /** 
    * Root mean squared error metric
    * $$rmse(vs1, vs2) = sqrt(mse(vs1, vs2))$$
    * 
    * @param maxVal [Optional] maximum value of the metric
    */
  final case class RMSE(maxVal: Option[Double] = None) extends HistoryBased(maxVal) with Absolute {
    private val mse: MSE = new MSE
    def compute(vs1: Iterable[BigInt], vs2: Iterable[BigInt]): Iterable[Double] = scala.math.sqrt(mse.compute(vs1, vs2))
  }
  case object RMSE {
    /** 
      * Create a new unconstrained root mean squared error metric
      */
    def apply(): RMSE = new RMSE

    /** 
      * Create a new constrained root mean squared error metric
      * @param maxVal maximum value of the metric
      */
    def apply(maxVal: Double): RMSE = new RMSE(Some(maxVal))

    /** 
      * Creates a new root mean squared error metric and computes its value on the two given sequences of samples
      * @param vs1 first sequence of samples
      * @param vs2 second sequence of samples
      * @return value of the metric
      */
    def apply(vs1: Iterable[BigInt], vs2: Iterable[BigInt]): Double = (new RMSE).compute(vs1, vs2)
    
    /** 
      * Creates a new root mean squared error metric and computes its value on the given sequence of tuples of samples
      * @param vss sequence of tuples of samples
      * @return value of the metric
      */
    def apply(vss: Iterable[(BigInt, BigInt)]): Double = (new RMSE).compute(vss)
  }

  /** 
    * Mean relative error distance metric
    * $$mred(vs1, vs2) = sum[i=0..len(vs1)-1](red(vs1[i], vs2[i])) / len(vs1)$$
    * 
    * @param maxVal [Optional] maximum value of the metric
    */
  final case class MRED(maxVal: Option[Double] = None) extends HistoryBased(maxVal) with Relative {
    private val red: RED = new RED
    def compute(vs1: Iterable[BigInt], vs2: Iterable[BigInt]): Iterable[Double] = {
      val (seq1, seq2) = (vs1.toSeq, vs2.toSeq)
      require(seq1.length == seq2.length, "the sequences must be the same length")
	  	(seq1.zip(seq2)).map { case (v1, v2) => red.compute(v1, v2) }.sum / seq1.length
    }
  }
  case object MRED {
    /** 
      * Creates a new unconstrained mean relative error distance metric
      */
    def apply(): MRED = new MRED

    /** 
      * Creates a new constrained mean relative error distance metric
      * @param maxVal maximum value of the metric
      */
    def apply(maxVal: Double): MRED = new MRED(Some(maxVal))

    /** 
      * Creates a new mean relative error distance metric and computes its value on the two given sequences of samples
      * @param vs1 first sequence of samples
      * @param vs2 second sequence of samples
      * @return value of the metric
      */
    def apply(vs1: Iterable[BigInt], vs2: Iterable[BigInt]): Double = (new MRED).compute(vs1, vs2)

    /** 
      * Creates a new mean relative error distance metric and computes its value on the given sequence of tuples of samples
      * @param vss sequence of tuples of samples
      * @return value of the metric
      */
    def apply(vss: Iterable[(BigInt, BigInt)]): Double = (new MRED).compute(vss)
  }
}
