package chiselverify.approximation

import chisel3.Bits
import scala.util.{Either, Left, Right}

import chiselverify.approximation.Metrics.Metric
import chiselverify.approximation.Watchers._

/** 
  * Handles everything related to tracking and verification of error metrics
  * 
  * An error reporter that is given no watchers performs neither tracking nor verification 
  * and produces an empty report if requested. Given only `Tracker`s, the error reporter 
  * tracks the marked ports and generates an error report from the given metrics, ignoring 
  * any given maximum values. Lastly, given only `Constraint`s or both `Constraint`s and 
  * `Tracker`s, the error reporter tracks the marked ports and supports verifying their 
  * constraints.
  * 
  * @param watchers any number of `Watcher`s, either `Tracker`s or `Constraint`s, to 
  *                 track and verify
  * 
  * @example {{{
  * ErrorReporter() // <- empty error reporter does nothing
  * ErrorReporter(  // <- using only trackers means `verify()` performs no verification
  *   track(approxPort_1, exactPort_1, metric_1_1, ...),
  *   track(approxPort_2, exactPort_2, metric_2_1, ...),
  *   ...
  * )
  * ErrorReporter(  // <- using constraints (and trackers) means `verify()` performs verification
  *   track(approxPort_1, exactPort_1, metric_1_1, ...),
  *   constrain(approxPort_2, exactPort_2, metric_2_1, ...),
  *   ...
  * )
  * }}}
  */
class ErrorReporter(watchers: Watcher*) {
  /** 
    * Creates a readable error metric report
    * @return report in string form
    */
  def report(): String = {
    val bs = new StringBuilder()

    // Identify the lines to include in the report
    val lines = watchers.map(_.report()).reduceOption(_ + _) match {
      case Some(errReport) => errReport.report().split('\n')
      case _ => Array("Error reporter is empty!")
    }

    // Compute the padding width to use for report headers and footers
    val padWidth = {
      val res = lines.map(_.length()).max
      if (res < 16) 2 else res - 14
    }

    // Combine the lines into one final report with proper indentation
    bs ++= s"${"=" * (padWidth / 2)} Error report ${"=" * (padWidth / 2 + (if ((padWidth & 0x1) == 1) 1 else 0))}\n"
    lines.foreach(ln => bs ++= s"$ln\n")
    bs ++= s"${"=" * (padWidth + 14)}"
    bs.mkString
  }

  /** 
    * Queries the metrics of all given watchers on a given port
    * @param approxPort port of the approximate DUT to query
    * @param metric [Optional] metric to query (defaults to None, meaning all metrics)
    * @return a map of (metric, values) pairs
    */
  def query(approxPort: Bits, metric: Option[Metric] = None): Map[Metric, Either[(Int, Double, Double), Double]] = {
    (metric match {
      case Some(mtrc) =>
        watchers.filter(wtchr => wtchr.approxPort == approxPort && wtchr.metrics.contains(mtrc))
      case _ =>
        watchers.filter(_.approxPort == approxPort)    
    }).foldLeft(Map.empty[Metric, Either[(Int, Double, Double), Double]]) { case (acc, wtchr) =>
      wtchr.compute()
      acc ++ wtchr.resultMap
    }
  }

  /** 
    * Samples all given watchers with given expected values
    * @param expected a map of (port, value) pairs
    * 
    * @note Requires that there exists an entry in the map for all non-port based watchers, 
    *       the corresponding approximate DUT ports being the keys.
    */
  def sample(expected: Map[Bits, BigInt] = Map.empty): Unit = watchers.foreach {
    case pwtchr: Watcher with PortBased =>
      // Port-based watchers can have optional reference values
      if (expected.contains(pwtchr.approxPort)) pwtchr.sample(expected(pwtchr.approxPort)) else pwtchr.sample()
    case rbsd =>
      // Reference-based watchers must have reference values
      rbsd.sample(expected.getOrElse(rbsd.approxPort,
        throw new AssertionError(s"watcher on port ${portName(rbsd.approxPort)} needs a reference value but none was provided")
      ))
  }

  /** 
    * Resets the state of all given watchers
    */
  def reset(): Unit = watchers.foreach { _.reset() }

  /** 
    * Verifies any given constraints and asserts their satisfaction, printing results of 
    * verification for each constraint
    * @return true if all constraints are satisfied
    */
  def verify(): Boolean = watchers.collect { case cnstr: Constraint => cnstr.verify() }.forall(s => s)
}
