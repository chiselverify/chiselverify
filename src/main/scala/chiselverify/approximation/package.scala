package chiselverify

import chisel3.{Bits, Data}
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
}
