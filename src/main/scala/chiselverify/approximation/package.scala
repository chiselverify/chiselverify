package chiselverify

import chisel3.Bits
import chisel3.experimental.DataMirror.{checkTypeEquivalence, directionOf, widthOf}
import chiseltest._
import scala.collection.mutable

import chiselverify.approximation.Reporting.{ConstraintReport, Report, TrackerReport}
import chiselverify.approximation.Metrics.{HistoryBased, Instantaneous, Metric}

package object approximation {

  /** 
    * Extracts the name of a chisel Module port
    * @param port port whose name to extract
    * @return name of the given port
    */
  private[chiselverify] def portName(port: Bits): String = port.pathName.split('.').last

  private[chiselverify] implicit def iterableToDouble(iterable: Iterable[Double]): Double = iterable.head
  private [chiselverify] implicit def doubleToIterable(double: Double): Iterable[Double] = Iterable(double)

  /** 
    * Represents the expected value style of a generic port watcher of which there are 
    * two types:
    * 
    * - Reference-based watchers that require expected values when sampled
    * - Port-based watchers that require an exact port to peek when sampled
    */
  private[chiselverify] sealed trait WatcherStyle

  /** 
    * Represents a reference-based 
    */
  private[chiselverify] sealed trait ReferenceBased extends WatcherStyle

  /** 
    * Represents a super-type of port-based watchers
    */
  private[chiselverify] sealed trait PortBased extends WatcherStyle {
    def exactPort: Bits

    /** 
      * Samples the two given ports and stores their values internally
      */
    def sample(): Unit
  }

  /** 
    * Represents a generic watcher of which there are two types:
    * 
    * - `Tracker`s that sample and report on error metrics related to a pair of ports 
    *   but are not used for verification
    * - `Constraint`s that sample and report on error metrics related to a pair of ports 
    *   supporting also verification
    * 
    * Both types come in two sub-types: one taking a port as reference and one that requires 
    * passing in expected values when sampling. The port-based watcher also accepts passed 
    * expected values.
    * 
    * @param approxPort port of the approximate DUT to track
    * @param metrics metrics to use in this watcher
    */
  private[chiselverify] sealed abstract class Watcher(val approxPort: Bits, val metrics: Metric*) {
    // Storage for port samples
    private val _samples: mutable.ArrayBuffer[(BigInt, BigInt)] = mutable.ArrayBuffer[(BigInt, BigInt)]()

    // For caching
    private val _caching_period = 100 //TODO: MAKE PARAMETER
    private val _sample_cache: mutable.ArrayBuffer[Double] = mutable.ArrayBuffer[Double]()

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
      * Samples the approximate port of the watcher and stores its value with a 
      * given expected value internally
      * @param expected expected value of the exact port
      */
    def sample(expected: BigInt): Unit = {
      if(metrics.nonEmpty) {
          _samples += ((approxPort.peek().litValue, expected))

          // Check for buffer emptying
          if(_samples.length >= _caching_period) {

            // Compute and cache results
            _sample_cache += compute_imm()

            // Clear sampling buffer
            _samples.clear()
          }
      }
      _computed = false
    }

    private[chiselverify] implicit def seqToAverageable(s: Seq[Double]): AverageableSeq = AverageableSeq(s)

    /**
      * Computes the average of a sequence of elements in a single pass
      * @param s the sequence we want to average
      * @return the numerical average of all values in the sequence
      */
    private[chiselverify] case class AverageableSeq(s: Seq[Double]) {
      def average: Double = {
          val t = s.foldLeft((0.0, 0)) { case (acc, i) => (acc._1 + i, acc._2 + 1) }
          t._1 / t._2
        }
    }

    /**
      * Computes the average of all sampled metrics.
      * @return
      */
    private[chiselverify] def compute_imm(): Double =
      metrics.map((m: Metric) => m.compute(samples).toSeq.average).average

      /**
      * Computes the metrics of the watcher and stores their values internally
      * 
      * @note Assumes there exist samples to compute metrics based on, if `metrics` is 
      *       non-empty.
      */
    private[chiselverify] def compute(): Unit = {
      assume(_samples.nonEmpty, "cannot compute metrics without samples")
      if (!_computed && metrics.nonEmpty)
            metrics.foreach(m => _results += (m -> m.compute(samples).toSeq))
      _computed = true
    }

    /** 
      * Converts the watcher into a report
      * @return a report
      */
    def report(): Report
  }

  /** 
    * Represents a tracker that does not support verification
    */
  private[chiselverify] sealed abstract class Tracker(approxPort: Bits, metrics: Metric*)
    extends Watcher(approxPort, metrics:_*) {
    this: WatcherStyle =>

    /** 
      * Converts the tracker into a report
      * @return a report
      */
    def report(): Report = {
      // First compute the metric values
      compute()

      // Then create a report for this watcher
      TrackerReport(portName(approxPort), metrics, results)
    }
  }

  /** 
    * Represents a plain tracker
    */
  private[chiselverify] sealed class PlainTracker(approxPort: Bits, metrics: Metric*)
    extends Tracker(approxPort, metrics:_*) with ReferenceBased

  /** 
    * Represents a port-based tracker
    * @param exactPort port of the exact DUT to track
    * 
    * @note Requires given pairs of ports to have the same type, direction and width.
    */
  private[chiselverify] sealed class PortTracker(approxPort: Bits, val exactPort: Bits, metrics: Metric*)
    extends Tracker(approxPort, metrics:_*) with PortBased {
    // Verify that the ports have the same type, direction and width
    private[PortTracker] val errMsg: String = "pairs of watched ports must have"
    require(checkTypeEquivalence(approxPort, exactPort), s"$errMsg the same type")
    require(directionOf(approxPort) == directionOf(exactPort), s"$errMsg the same direction")
    require(widthOf(approxPort) == widthOf(exactPort), s"$errMsg the same width")

    def sample(): Unit = sample(exactPort.peek().litValue)
  }

  /** 
    * Represents a constraint that supports verification
    * @todo extend to support early termination on instantaneous metrics
    */
  private[chiselverify] sealed abstract class Constraint(approxPort: Bits, metrics: Metric*)
    extends Watcher(approxPort, metrics:_*) {
    this: WatcherStyle =>

    /** 
      * Converts the constraint into a report
      * @return a report
      */
    def report(): Report = {
      // First compute the metric values
      compute()

      // Then create a report for this watcher
      ConstraintReport(portName(approxPort), metrics, results)
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
        bs ++= s"Verification results of constraint on port ${portName(approxPort)}\n"
        val satisfieds = metrics.map { mtrc =>
          val mtrcResults   = results(mtrc)
          val mtrcSatisfied = mtrcResults.map(mtrc.check).forall(s => s)
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
    * Represents a plain constraint
    */
  private[chiselverify] sealed class PlainConstraint(approxPort: Bits, metrics: Metric*)
    extends Constraint(approxPort, metrics:_*) with ReferenceBased

  /** 
    * Represents a port-based constraint
    * @param exactPort port of the exact DUT to track
    * 
    * @note Requires given pairs of ports to have the same type, direction and width.
    */
  private[chiselverify] sealed class PortConstraint(approxPort: Bits, val exactPort: Bits, metrics: Metric*)
    extends Constraint(approxPort, metrics:_*) with PortBased {
    // Verify that the ports have the same type, direction and width
    private[PortConstraint] val errMsg: String = "pairs of watched ports must have"
    require(checkTypeEquivalence(approxPort, exactPort), s"$errMsg the same type")
    require(directionOf(approxPort) == directionOf(exactPort), s"$errMsg the same direction")
    require(widthOf(approxPort) == widthOf(exactPort), s"$errMsg the same width")

    def sample(): Unit = sample(exactPort.peek().litValue)
  }

  /** 
    * API end-point for the approximate verification constructs. Allows to define port trackers 
    * with a specified list of relevant error metrics to be used in a report
    */
  object track {
    /** 
      * Print warnings about metrics with ignored maximum values
      * @param portName a port name
      * @param metrics a sequence of metrics
      */
    private[chiselverify] def warn(portName: String, metrics: Iterable[Metric]): Unit = metrics
      .filter(_.isConstrained).foreach { mtrc =>
      println(s"Tracker on port ${portName} ignores maximum value of $mtrc metric!")
    }

    /** 
      * Creates a new `PlainTracker` with the given arguments
      * @param approxPort port of the approximate DUT to track
      * @param metrics metrics to use in this tracker
      */
    def apply(approxPort: Bits, metrics: Metric*): Tracker = {
      warn(portName(approxPort), metrics)

      // Create a new `PlainTracker` with the given arguments
      new PlainTracker(approxPort, metrics:_*)
    }

    /** 
      * Creates a new `PortTracker` with the given arguments
      * @param approxPort port of the approximate DUT to track
      * @param exactPort port of the exact DUT to track
      * @param metrics metrics to use in this tracker
      */
    def apply(approxPort: Bits, exactPort: Bits, metrics: Metric*): Tracker = {
      warn(portName(approxPort), metrics)

      // Create a new `PortTracker` with the given arguments
      new PortTracker(approxPort, exactPort, metrics:_*)
    }
  }

  /** 
    * API end-point for the approximate verification constructs. Allows to define port constraints  
    * with a specified list of relevant error metrics to be used in a report
    */
  object constrain {
    /** 
      * Checks and fails on unconstrained metrics
      * @param port a port name
      * @param constraints a sequence of metrics
      */
    private[chiselverify] def check(portName: String, constraints: Iterable[Metric]): Unit = constraints
      .foreach { mtrc =>
        require(mtrc.isConstrained, s"cannot create $mtrc constraint without maximum value")
    }

    /** 
      * Creates a new `PlainConstraint` with the given arguments
      * @param approxPort port of the approximate DUT to track
      * @param constraints metrics to verify in this constraint
      */
    def apply(approxPort: Bits, constraints: Metric*): Constraint = {
      check(portName(approxPort), constraints)

      // Create a new `PlainConstraint` with the given arguments
      new PlainConstraint(approxPort, constraints:_*)
    }

    /** 
      * Creates a new `PortConstraint` with the given arguments
      * @param approxPort port of the approximate DUT to track
      * @param exactPort port of the exact DUT to track
      * @param constraints metrics to verify in this constraint
      */
    def apply(approxPort: Bits, exactPort: Bits, constraints: Metric*): Constraint = {
      check(portName(approxPort), constraints)
      
      // Create a new `PortConstraint` with the given arguments
      new PortConstraint(approxPort, exactPort, constraints:_*)
    }
  }
}
