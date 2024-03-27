package chiselverify

import chisel3.Bits

import chiselverify.approximation.Metrics.Metric
import chiselverify.approximation.Watchers._

package object approximation {

  /** 
    * Extracts the name of a chisel Module port
    * @param port port whose name to extract
    * @return name of the given port
    */
  private[chiselverify] def portName(port: Bits): String = port.pathName.split('.').last

  /** 
    * Number of samples to keep before caching a computation of an error metric in the 
    * `PortWatcher`. Beware that intermediate caching of results may lead to small round-off 
    * errors compared to not using it, yet it greatly reduces the required heap memory as 
    * much fewer (costly) `BigInt`s will be stored internally.
    * 
    * @note Selected to be safely below the cache size of most processors assuming an average 
    *       of 64 bits per `BigInt`.
    * @note This value is also used to delimit cache size, meaning cached values are collapsed 
    *       if their count exceeds this number.
    */
  private[chiselverify] final val MaxCacheSize: Int = 1024

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
      new PlainTracker(approxPort, metrics:_*)(MaxCacheSize)
    }

    /** 
      * Creates a new `PlainTracker` with the given arguments
      * @param approxPort port of the approximate DUT to track
      * @param maxCacheSize the maximum cache size to use in this tracker
      * @param metrics metrics to use in this tracker
      */
    def apply(approxPort: Bits, maxCacheSize: Int, metrics: Metric*): Tracker = {
      warn(portName(approxPort), metrics)

      // Create a new `PlainTracker` with the given arguments
      new PlainTracker(approxPort, metrics:_*)(maxCacheSize)
    }

    /** 
      * Creates a new `PortTracker` with the given arguments
      * @param approxPort port of the approximate DUT to track
      * @param exactPort port of the exact DUT to track
      * @param metrics metrics to use in this tracker
      */
    def apply(approxPort: Bits, exactPort: Bits, metrics: Metric*): Tracker = {
      warn(portName(approxPort), metrics)

      // Create a new `Tracker` with the given arguments
      new PortTracker(approxPort, exactPort, metrics:_*)(MaxCacheSize)
    }

    /** 
      * Creates a new Tracker with the given arguments
      * @param approxPort port of the approximate DUT to track
      * @param exactPort port of the exact DUT to track
      * @param maxCacheSize the maximum cache size to use in this tracker
      * @param metrics metrics to use in this tracker
      */
    def apply(approxPort: Bits, exactPort: Bits, maxCacheSize: Int, metrics: Metric*): Tracker = {
      // Warn the designer about any metrics with maximum values
      metrics.foreach { metr =>
        println(s"Tracker on ports ${portName(approxPort)} and ${portName(exactPort)} ignores maximum value of $metr metric!")
      }

      // Create a new `Tracker` with the given arguments
      new PortTracker(approxPort, exactPort, metrics:_*)(maxCacheSize)
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
      new PlainConstraint(approxPort, constraints:_*)(MaxCacheSize)
    }

    /** 
      * Creates a new `PlainConstraint` with the given arguments
      * @param approxPort port of the approximate DUT to track
      * @param maxCacheSize the maximum cache size to use in this constraint
      * @param constraints metrics to verify in this constraint
      */
    def apply(approxPort: Bits, maxCacheSize: Int, constraints: Metric*): Constraint = {
      check(portName(approxPort), constraints)

      // Create a new `PlainConstraint` with the given arguments
      new PlainConstraint(approxPort, constraints:_*)(maxCacheSize)
    }

    /** 
      * Creates a new `PortConstraint` with the given arguments
      * @param approxPort port of the approximate DUT to track
      * @param exactPort port of the exact DUT to track
      * @param constraints metrics to verify in this constraint
      */
    def apply(approxPort: Bits, exactPort: Bits, constraints: Metric*): Constraint = {
      check(portName(approxPort), constraints)

      // Create a new `Constraint` with the given arguments
      new PortConstraint(approxPort, exactPort, constraints:_*)(MaxCacheSize)
    }

    /** 
      * Creates a new Constraint with the given arguments
      * @param approxPort port of the approximate DUT to track
      * @param exactPort port of the exact DUT to track
      * @param maxCacheSize the maximum cache size to use in this constraint
      * @param constraints metrics to verify in this constraint
      */
    def apply(approxPort: Bits, exactPort: Bits, maxCacheSize: Int, constraints: Metric*): Constraint = {
      // Verify that all the metrics have maximum values
      constraints.foreach { constr =>
        require(constr.isConstrained, 
          s"cannot create $constr constraint without maximum value")
      }

      // Create a new `Constraint` with the given arguments
      new PortConstraint(approxPort, exactPort, constraints:_*)(maxCacheSize)
    }
  }
}
