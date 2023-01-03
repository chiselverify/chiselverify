package chiselverify

import chisel3.{Bits, Data}
import chisel3.experimental.DataMirror.{checkTypeEquivalence, directionOf, widthOf}
import chiseltest._
import scala.collection.mutable

import chiselverify.approximation.Reporting.{ConstraintReport, Report, TrackerReport, OptimizedTrackerReport, OptimizedConstraintReport}
import chiselverify.approximation.Metrics.{HistoryBased, Instantaneous, Metric}

package object approximation {

  /** 
    * Extracts the name of a chisel Module port
    * @param port port whose name to extract
    * @return name of the given port
    */
  private[chiselverify] def portName(port: Data): String = port.pathName.split('.').last

  /** 
    * Represents a generic port watcher of which there are two types:
    * 
    * - `Tracker`s that sample and report on error metrics related to a pair of ports 
    *   but are not used for verification
    * - `Constraint`s that sample and report on error metrics related to a pair of ports 
    *   supporting also verification
    * 
    * @param approxPort port of the approximate DUT to track
    * @param exactPort port of the exact DUT to track
    * @param metrics metrics to use in this watcher
    * 
    * @note Requires given pairs of ports to have the same type, direction and width.
    * 
    * @todo extend to support software emulation of exact DUT
    */
  private[chiselverify] abstract class PortWatcher(val approxPort: Bits, val exactPort: Bits, val metrics: Metric*) {
    // Verify that the ports have the same type, direction and width
    private[PortWatcher] val errMsg: String = "pairs of watched ports must have"
    require(checkTypeEquivalence(approxPort, exactPort), s"$errMsg the same type")
    require(directionOf(approxPort) == directionOf(exactPort), s"$errMsg the same direction")
    require(widthOf(approxPort) == widthOf(exactPort), s"$errMsg the same width")

    // Storage for port samples
    private val _samples: mutable.ArrayBuffer[(BigInt, BigInt)] = mutable.ArrayBuffer[(BigInt, BigInt)]()

    // Storage for caching results during computation
    private var _computed: Boolean = false
    private val _results: mutable.Map[Metric, Seq[Double]] = mutable.Map[Metric, Seq[Double]]()

    /** 
      * Accesses the internal sample storage
      */
    private[chiselverify] def samples: Array[(BigInt, BigInt)] = _samples.toArray

    /** 
      * Accesses the internal result storage
      */
    private[chiselverify] def computed: Boolean = _computed
    private[chiselverify] def results: Map[Metric, Seq[Double]] = _results.toMap

    /** 
      * Samples the two ports of the watcher and stores their values internally
      */
    def sample(): Unit = {
      metrics match {
        case Nil =>
        case _ => _samples += ((approxPort.peek().litValue, exactPort.peek().litValue))
      }
      _computed = false
    }

    /** 
      * Computes the metrics of the watcher and stores their values internally
      * 
      * @note Assumes there exist samples to compute metrics based on, if `metrics` is 
      *       non-empty.
      */
    private[chiselverify] def compute(): Unit = {
      if (!_computed) {
        metrics match {
          case Nil =>
          case _ =>
            assume(!_samples.isEmpty, "cannot compute metrics without samples")
            metrics.foreach { _ match {
              case mtrc: Instantaneous =>
                _results += (mtrc -> mtrc.compute(samples).toSeq)
              case mtrc: HistoryBased =>
                _results += (mtrc -> Seq(mtrc.compute(samples)))
            }}
        }
      }
      _computed = true
    }

    /** 
      * Converts the watcher into a report
      * @return a report
      */
    def report(): Report
  }

  /** 
    * Number of samples to keep before caching a computation of an error metric in the 
    * `PortWatcher`. Beware that intermediate caching of results may lead to small round-off 
    * errors compared to not using it, yet it greatly reduces the required heap memory as 
    * much fewer (costly) `BigInt`s will be stored internally.
    * 
    * @note Selected to be safely below the cache size of most processors assuming an average 
    *       of 64 bits per `BigInt`.
    */
  private[chiselverify] final val MaxBufferedSamples: Int = 1024

  /** 
    * Represents a generic port watcher of which there are two types:
    * 
    * - `Tracker`s that sample and report on error metrics related to a pair of ports 
    *   but are not used for verification
    * - `Constraint`s that sample and report on error metrics related to a pair of ports 
    *   supporting also verification
    * 
    * Implements intermediate results caching that may lead to small round-off errors but 
    * leads to greatly reduced heap memory requirements.
    * 
    * @param approxPort port of the approximate DUT to track
    * @param exactPort port of the exact DUT to track
    * @param metrics metrics to use in this watcher
    * 
    * @note Requires given pairs of ports to have the same type, direction and width.
    * 
    * @todo extend to support software emulation of exact DUT
    */
  private[chiselverify] abstract class OptimizedPortWatcher(approxPort: Bits, exactPort: Bits, metrics: Metric*) extends PortWatcher(approxPort, exactPort, metrics:_*) {
    // Verify that the ports have the same type, direction and width
    private[OptimizedPortWatcher] val errMsg: String = "pairs of watched ports must have"
    require(checkTypeEquivalence(approxPort, exactPort), s"$errMsg the same type")
    require(directionOf(approxPort) == directionOf(exactPort), s"$errMsg the same direction")
    require(widthOf(approxPort) == widthOf(exactPort), s"$errMsg the same width")

    // Storage for port samples
    private val _samples: mutable.ArrayBuffer[(BigInt, BigInt)] = mutable.ArrayBuffer[(BigInt, BigInt)]()

    // Storage for caching results during computation
    // ... stores (metric max index, metric max, metric mean) for instantaneous metrics
    private val _instCache: mutable.Map[Metric, mutable.ArrayBuffer[(Int, Double, Double)]] =
      mutable.Map[Metric, mutable.ArrayBuffer[(Int, Double, Double)]]()
    // ... stores (metric value) for history-based metrics
    private val _histCache: mutable.Map[Metric, mutable.ArrayBuffer[Double]] =
      mutable.Map[Metric, mutable.ArrayBuffer[Double]]()

    // Storage for results after computation
    private var _computed: Boolean = false
    // ... stores (metric max index, metric max, metric mean) for instantaneous metrics
    private val _instResults: mutable.Map[Metric, (Int, Double, Double)] =
      mutable.Map[Metric, (Int, Double, Double)]()
    // ... stores (metric value) for history-based metrics
    private val _histResults: mutable.Map[Metric, Double] = mutable.Map[Metric, Double]()

    /** 
      * Accesses the internal sample storage
      */
    override private[chiselverify] def samples: Array[(BigInt, BigInt)] = _samples.toArray

    /** 
      * Accesses the internal result storage
      */
    private[chiselverify] def resultMap: Map[Metric, Any] = _instResults.toMap ++ _histResults.toMap

    /** 
      * Samples the two ports of the watcher and stores their values internally
      */
    override def sample(): Unit = {
      metrics match {
        case Nil =>
        case _ => _samples += ((approxPort.peek().litValue, exactPort.peek().litValue))
      }
      // For every `MaxBufferedSamples`, compute intermediate results and buffer them
      if (_samples.length == MaxBufferedSamples) {
        metrics.foreach { _ match {
          case mtrc: Instantaneous =>
            val mtrcResults         = mtrc.compute(_samples).toSeq
            val (mtrcMax, maxIndex) = mtrcResults.zipWithIndex.maxBy(_._1)
            val mtrcMean            = mtrcResults.sum / mtrcResults.length
            _instCache.getOrElseUpdate(mtrc, new mutable.ArrayBuffer[(Int, Double, Double)]) += ((maxIndex, mtrcMax, mtrcMean))
          case mtrc: HistoryBased =>
            val mtrcResult = mtrc.compute(_samples)
            _histCache.getOrElseUpdate(mtrc, new mutable.ArrayBuffer[Double]) += mtrcResult
        }}
        _samples.clear()
      }
      _computed = false
    }

    /** 
      * Computes the metrics of the watcher and stores their values internally
      * 
      * @note Assumes there exist samples to compute metrics based on, if `metrics` is 
      *       non-empty.
      */
    override private[chiselverify] def compute(): Unit = {
      if (!_computed) {
        metrics match {
          case Nil =>
          case _ =>
            assume(!(_samples.isEmpty && _instCache.isEmpty && _histCache.isEmpty), "cannot compute metrics without samples")
            metrics.foreach { _ match {
              case mtrc: Instantaneous =>
                // Compute results from samples
                val (sMaxIndex, sMtrcMax, sMtrcMean) = {
                  val mtrcResults         = mtrc.compute(_samples).toSeq
                  val (mtrcMax, maxIndex) = mtrcResults.zipWithIndex.maxBy(_._1)
                  val mtrcMean            = mtrcResults.sum / mtrcResults.length
                  (maxIndex, mtrcMax, mtrcMean)
                }
                // Combine with results from cache
                val combResults = _instCache.getOrElse(mtrc, mutable.ArrayBuffer[(Int, Double, Double)]())
                  .toArray :+ ((sMaxIndex, sMtrcMax, sMtrcMean))
                val (cMaxIndex, cMtrcMax, cMtrcMean) = {
                  val (maxIndex, mtrcMax) = combResults.zipWithIndex
                    .map { case ((maxIndex, mtrcMax, _), offset) => (maxIndex + offset * MaxBufferedSamples, mtrcMax) }
                    .maxBy(_._2)
                  val mtrcMean = (combResults.take(combResults.length-1).map(_._3 * MaxBufferedSamples) :+ combResults.last._3 * _samples.length).sum / ((combResults.length - 1) * MaxBufferedSamples + _samples.length)
                  (maxIndex, mtrcMax, mtrcMean)
                }
                // Store the final result
                _instResults(mtrc) = ((cMaxIndex, cMtrcMax, cMtrcMean))
              case mtrc: HistoryBased =>
                // Compute results from samples
                val sMtrcResult = mtrc.compute(_samples)
                // Combine with results from cache
                val combResults = _histCache.getOrElse(mtrc, mutable.ArrayBuffer[Double]())
                  .toArray :+ sMtrcResult
                val cMtrcMean = (combResults.take(combResults.length-1).map(_ * MaxBufferedSamples) :+ combResults.last * _samples.length).sum / ((combResults.length - 1) * MaxBufferedSamples + _samples.length)
                // Store the final result
                _histResults(mtrc) = cMtrcMean
            }}
        }
      }
      _computed = true
    }

    /** 
      * Converts the watcher into a report
      * @return a report
      */
    def report(): Report
  }

  /** 
    * Represents a port tracker that does not support verification
    */
  private[chiselverify] class Tracker(approxPort: Bits, exactPort: Bits, metrics: Metric*)
    extends PortWatcher(approxPort, exactPort, metrics:_*) {
    def report(): Report = {
      // First compute the metric values
      compute()

      // Then create a report for this watcher
      TrackerReport(portName(approxPort), portName(exactPort), metrics, results)
    }
  }

  /** 
    * Represents a port tracker that does not support verification
    */
  private[chiselverify] class OptimizedTracker(approxPort: Bits, exactPort: Bits, metrics: Metric*)
    extends OptimizedPortWatcher(approxPort, exactPort, metrics:_*) {
    def report(): Report = {
      // First compute the metric values

      // Then create a report for this watcher
      OptimizedTrackerReport(portName(approxPort), portName(exactPort), metrics, resultMap)
    }
  }

  /** 
    * Represents a port constraint that supports verification
    * @todo extend to support early termination on instantaneous metrics
    */
  private[chiselverify] class Constraint(approxPort: Bits, exactPort: Bits, metrics: Metric*)
    extends PortWatcher(approxPort, exactPort, metrics:_*) {
    def report(): Report = {
      // First compute the metric values
      compute()

      // Then create a report for this watcher
      ConstraintReport(portName(approxPort), portName(exactPort), metrics, results)
    }

    /** 
      * Checks if all registered metrics are satisfied
      * @return true if all `metrics` are satisfied
      */
    def verify(): Boolean = metrics match {
      case Nil => true
      case _ =>
        // First compute the metric values
        compute()

        // Then verify all the metrics and identify the unsatisfied ones
        val bs = new StringBuilder()
        bs ++= s"Verification results of constraint on ports ${portName(approxPort)} and ${portName(exactPort)}\n"
        val satisfieds = metrics.map { mtrc =>
          val mtrcResults   = results(mtrc)
          val mtrcSatisfied = mtrcResults.map(mtrc.check(_)).forall(s => s)
          if (mtrcSatisfied) {
            bs ++= s"- ${mtrc} metric is satisfied!\n"
          } else {
            bs ++= s"- ${mtrc} metric is violated by error "
            bs ++= f"${mtrcResults.collectFirst { case res if !mtrc.check(res) => res }.get}%.3f!\n"
          }
          mtrcSatisfied
        }
        print(bs.mkString)

        // Finally, return whether the metrics are satisfied
        satisfieds.forall(s => s)
    }
  }

  /** 
    * Represents a port constraint that supports verification
    * @todo extend to support early termination on instantaneous metrics
    */
  private[chiselverify] class OptimizedConstraint(approxPort: Bits, exactPort: Bits, metrics: Metric*)
    extends OptimizedPortWatcher(approxPort, exactPort, metrics:_*) {
    def report(): Report = {
      // First compute the metric values
      compute()

      // Then create a report for this watcher
      OptimizedConstraintReport(portName(approxPort), portName(exactPort), metrics, resultMap)
    }

    /** 
      * Checks if all registered metrics are satisfied
      * @return true if all `metrics` are satisfied
      */
    def verify(): Boolean = metrics match {
      case Nil => true
      case _ =>
        // First compute the metric values
        compute()

        // Then verify all the metrics and identify the violated ones
        val bs = new StringBuilder()
        bs ++= s"Verification results of constraint on ports ${portName(approxPort)} and ${portName(exactPort)}\n"
        val satisfieds = metrics.map { mtrc =>
          val mtrcResults = resultMap(mtrc)
          val (mtrcSatisfied, mtrcValue) = mtrc match {
            case _: Instantaneous =>
              val (_, mtrcMax, _) = mtrcResults.asInstanceOf[(Int, Double, Double)]
              (mtrc.check(mtrcMax), mtrcMax)
            case _: HistoryBased =>
              val mtrcVal = mtrcResults.asInstanceOf[Double]
              (mtrc.check(mtrcVal), mtrcVal)
          }
          if (mtrcSatisfied) {
            bs ++= s"- $mtrc metric is satisfied!\n"
          } else {
            bs ++= f"- $mtrc metric is violated by error $mtrcValue%.3f!\n"
          }
          mtrcSatisfied
        }
        print(bs.mkString)

        // Finally, return whether the metrics are satisfied
        satisfieds.forall(s => s)
    }
  }

  /** 
    * API end-point for the approximate verification constructs. Allows to define port trackers 
    * with a specified list of relevant error metrics to be used in a report
    */
  object track {
    /** 
      * Creates a new Tracker with the given arguments
      * @param approxPort port of the approximate DUT to track
      * @param exactPort port of the exact DUT to track
      * @param metrics metrics to use in this tracker
      */
    def apply(approxPort: Bits, exactPort: Bits, metrics: Metric*): Tracker = {
      // Warn the designer about any metrics with maximum values
      metrics.foreach { metr =>
        println(s"Tracker on ports ${portName(approxPort)} and ${portName(exactPort)} ignores maximum value of $metr metric!")
      }

      // Create a new `Tracker` with the given arguments
      new Tracker(approxPort, exactPort, metrics:_*)
    }
  }

  /** 
    * API end-point for the approximate verification constructs. Allows to define port trackers 
    * with a specified list of relevant error metrics to be used in a report
    */
  object opttrack {
    /** 
      * Creates a new Tracker with the given arguments
      * @param approxPort port of the approximate DUT to track
      * @param exactPort port of the exact DUT to track
      * @param metrics metrics to use in this tracker
      */
    def apply(approxPort: Bits, exactPort: Bits, metrics: Metric*): OptimizedTracker = {
      // Warn the designer about any metrics with maximum values
      metrics.foreach { metr =>
        println(s"Tracker on ports ${portName(approxPort)} and ${portName(exactPort)} ignores maximum value of $metr metric!")
      }

      // Create a new `Tracker` with the given arguments
      new OptimizedTracker(approxPort, exactPort, metrics:_*)
    }
  }

  /** 
    * API end-point for the approximate verification constructs. Allows to define port constraints  
    * with a specified list of relevant error metrics to be used in a report
    */
  object constrain {
    /** 
      * Creates a new Constraint with the given arguments
      * @param approxPort port of the approximate DUT to track
      * @param exactPort port of the exact DUT to track
      * @param constraints metrics to verify in this constraint
      */
    def apply(approxPort: Bits, exactPort: Bits, constraints: Metric*): Constraint = {
      // Verify that all the metrics have maximum values
      constraints.foreach { constr =>
        require(constr.isConstrained, 
          s"cannot create $constr constraint without maximum value")
      }
      
      // Create a new `Constraint` with the given arguments
      new Constraint(approxPort, exactPort, constraints:_*)
    }
  }

  /** 
    * API end-point for the approximate verification constructs. Allows to define port constraints  
    * with a specified list of relevant error metrics to be used in a report
    */
  object optconstrain {
    /** 
      * Creates a new Constraint with the given arguments
      * @param approxPort port of the approximate DUT to track
      * @param exactPort port of the exact DUT to track
      * @param constraints metrics to verify in this constraint
      */
    def apply(approxPort: Bits, exactPort: Bits, constraints: Metric*): OptimizedConstraint = {
      // Verify that all the metrics have maximum values
      constraints.foreach { constr =>
        require(constr.isConstrained, 
          s"cannot create $constr constraint without maximum value")
      }
      
      // Create a new `Constraint` with the given arguments
      new OptimizedConstraint(approxPort, exactPort, constraints:_*)
    }
  }
}
