package chiselverify

import chisel3.{Bits, Data}
import chisel3.experimental.DataMirror.{checkTypeEquivalence, directionOf, widthOf}
import chiseltest._
import scala.collection.mutable

import chiselverify.approximation.Reporting.{ConstraintReport, Report, TrackerReport}
import chiselverify.approximation.Metrics.{isAbsolute, HistoryBased, Instantaneous, Metric}

package object approximation {

  /** 
    * Extracts the name of a chisel Module port
    * @param port port whose name to extract
    * @return name of the given port
    */
  private[chiselverify] def portName(port: Data): String = port.pathName.split('.').last

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
    * Represents a generic port watcher of which there are two types:
    * 
    * - `Tracker`s that sample and report on error metrics related to a pair of ports 
    *   but are not used for verification
    * - `Constraint`s that sample and report on error metrics related to a pair of ports 
    *   supporting also verification
    * 
    * Implements intermediate results caching that may lead to small round-off errors but 
    * leads to greatly reduced heap memory requirements. Both the sample cache and the 
    * results caches are collapsed when exceeding `maxCacheSize` elements.
    * 
    * Cached results are computed in order of n-th roots first, products second in
    * order to avoid potential overflows. This may increase the numerical errors
    * arising from caching ever so slightly, though, the effects appear, at least
    * empirically, rather small.
    * 
    * @param approxPort port of the approximate DUT to track
    * @param exactPort port of the exact DUT to track
    * @param metrics metrics to use in this watcher
    * @param maxCacheSize the maximum cache size to use in this watcher
    * 
    * @note Requires given pairs of ports to have the same type, direction and width.
    */
  private[chiselverify] abstract class PortWatcher(approxPort: Bits, exactPort: Bits, metrics: Metric*)
    (maxCacheSize: Int) {

    // Verify that the ports have the same type, direction and width
    private[PortWatcher] val errMsg: String = "pairs of watched ports must have"
    require(checkTypeEquivalence(approxPort, exactPort), s"$errMsg the same type")
    require(directionOf(approxPort) == directionOf(exactPort), s"$errMsg the same direction")
    require(widthOf(approxPort) == widthOf(exactPort), s"$errMsg the same width")

    // Storage for port samples
    private val _samples: mutable.ArrayBuffer[(BigInt, BigInt)] = mutable.ArrayBuffer[(BigInt, BigInt)]()

    // Storage for caching results during computation
    // ... the number of times the cache has been collapsed
    private var _cacheCollapseCount: Int = 0
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
    private val _histResults: mutable.Map[Metric, Double] =
      mutable.Map[Metric, Double]()

    /** 
      * Accesses the internal sample storage
      */
    private[chiselverify] def samples: Array[(BigInt, BigInt)] = _samples.toArray

    /** 
      * Accesses the internal result storage
      */
    private[chiselverify] def resultMap: Map[Metric, Any] = _instResults.toMap ++ _histResults.toMap

    /** 
      * Samples the two ports of the watcher and stores their values internally
      */
    def sample(): Unit = {
      // Sample the ports if there are any metrics registered
      if (metrics.nonEmpty) {
        _samples += ((approxPort.peek().litValue, exactPort.peek().litValue))
        // Collapse the sample storage and internal caches, if needed
        collapse()
      }

      // Mark the watcher as non-computed
      _computed = false
    }

    /** 
      * Collapses the internal sample storage and cache if either has exceeded `maxCacheSize` elements
      */
    private[chiselverify] def collapse(): Unit = {
      // If the sample storage has more than `maxCacheSize` elements, collapse and clear it
      if (_samples.size >= maxCacheSize) {
        metrics.foreach { _ match {
          case mtrc: Instantaneous =>
            val mtrcResults = mtrc.compute(_samples)
            val (mtrcMax, maxIndex) = mtrcResults.zipWithIndex.maxBy(_._1)

            // If the metric mixes in `Absolute`, we compute arithmetic means; if it mixes in 
            // `Relative`, we compute geometric means instead
            val mtrcMean = if (isAbsolute(mtrc)) {
              mtrcResults.sum / mtrcResults.size
            } else {
              scala.math.pow(mtrcResults.product, 1.0 / mtrcResults.size)
            }
            _instCache.getOrElseUpdate(mtrc, new mutable.ArrayBuffer[(Int, Double, Double)]) +=
              ((maxIndex, mtrcMax, mtrcMean))

          case mtrc: HistoryBased =>
            _histCache.getOrElseUpdate(mtrc, new mutable.ArrayBuffer[Double]) += mtrc.compute(_samples)
        }}

        // Clear the sample storage
        _samples.clear()
      }

      // If the caches have more than `maxCacheSize` elements, collapse them and increment the counter
      if (_instCache.exists(_._2.size >= maxCacheSize) || _histCache.exists(_._2.size >= maxCacheSize)) {
        metrics.foreach { mtrc =>
          // If the metric mixes in `Absolute`, we compute arithmetic means; if it mixes in 
          // `Relative`, we compute geometric means instead
          val absolute = isAbsolute(mtrc)

          // As we occasionally collapse the cache into its first element too, it is
          // necessary to check whether this has occured before
          val wasCollapsed = _cacheCollapseCount > 0

          // Now collapse the caches
          mtrc match {
            case mtrc: Instantaneous =>
              val mtrcResults = _instCache(mtrc)
              val resWeight = {
                val first = if (wasCollapsed) _cacheCollapseCount * maxCacheSize else 1
                val rest  = mtrcResults.size - 1
                first + rest
              }

              // Find the maximum error and its index in the current list of results
              val (mtrcMax, maxIndex) = mtrcResults.map { case (maxInd, mtrcMax, _) => (mtrcMax, maxInd) }.maxBy(_._1)

              // Compute the mean error value depending on the type of metric and
              // whether the cache has been collapsed before
              val mtrcMean = if (absolute) {
                val sum = if (!wasCollapsed) {
                  mtrcResults.map(_._3).sum
                } else {
                  mtrcResults.head._3 * _cacheCollapseCount * maxCacheSize + mtrcResults.tail.map(_._3).sum
                }
                sum / resWeight
              } else {
                if (!wasCollapsed) {
                  mtrcResults.map(res => scala.math.pow(res._3, 1.0 / resWeight)).product
                } else {
                  val firstW = (_cacheCollapseCount * maxCacheSize).toDouble / resWeight
                  val restW  = 1.0 / resWeight
                  scala.math.pow(mtrcResults.head._3, firstW) * mtrcResults.tail.map(res => scala.math.pow(res._3, restW)).product
                }
              }

              // Store the result in the first position of the cache
              _instCache(mtrc) = mutable.ArrayBuffer((maxIndex, mtrcMax, mtrcMean))

            case mtrc: HistoryBased =>
              val mtrcResults = _histCache(mtrc)
              val resWeight = {
                val first = if (wasCollapsed) _cacheCollapseCount * maxCacheSize else 1
                val rest  = mtrcResults.size - 1
                first + rest
              }

              // Compute the mean error value depending on the type of metric and
              // whether the cache has been collapsed before
              val mtrcMean = if (absolute) {
                val sum = if (!wasCollapsed) {
                  mtrcResults.sum
                } else {
                  mtrcResults.head * _cacheCollapseCount * maxCacheSize + mtrcResults.tail.sum
                }
                sum / resWeight
              } else {
                if (!wasCollapsed) {
                  mtrcResults.map(res => scala.math.pow(res, 1.0 / resWeight)).product
                } else {
                  val firstW = (_cacheCollapseCount * maxCacheSize).toDouble / resWeight
                  val restW  = 1.0 / resWeight
                  scala.math.pow(mtrcResults.head, firstW) * mtrcResults.tail.map(res => scala.math.pow(res, restW)).product
                }
              }

              // Store the result in the first position of the cache
              _histCache(mtrc) = mutable.ArrayBuffer(mtrcMean)
          }
        }

        // Keep track of how many times the cache has been collapsed
        _cacheCollapseCount += 1
      }
    }

    /** 
      * Computes the metrics of the watcher and stores their values internally
      * 
      * If the watcher has already been computed or if it includes no metrics,
      * this method does nothing.
      * 
      * @note Assumes there exist samples to compute metrics based on, if `metrics` is non-empty.
      */
    private[chiselverify] def compute(): Unit = {
      for (mtrc <- metrics if !_computed && metrics.nonEmpty) {
        assume(_samples.nonEmpty || _instCache.nonEmpty || _histCache.nonEmpty,
          "cannot compute metrics without samples")

        // If the metric mixes in `Absolute`, we compute arithmetic means; if it mixes in 
        // `Relative`, we compute geometric means instead
        val absolute = isAbsolute(mtrc)

        // Check if there are any samples to compute on
        val hasSamples = _samples.nonEmpty

        // Check if there are any cached results to compute on
        val hasCached = _instCache.contains(mtrc) || _histCache.contains(mtrc)

        // As we occasionally collapse the cache into its first element too, it is
        // necessary to check whether this has occured before
        val wasCollapsed = _cacheCollapseCount > 0

        // Compute the metric. For either type of metrics, we first compute
        // based on samples (if any) and then based on cached results (if any).
        // Afterwards, we combine the results appropriately
        mtrc match {
          case mtrc: Instantaneous =>
            // Compute results from samples
            val (sMaxIndex, sMtrcMax, sMtrcMean, sWeight) = if (_samples.isEmpty) {
              (-1, Double.NegativeInfinity, 0.0, 0)
            } else {
              val mtrcResults = mtrc.compute(_samples)
              val resWeight = mtrcResults.size
              val (mtrcMax, maxIndex) = mtrcResults.zipWithIndex.maxBy(_._1)
              val mtrcMean = if (absolute) {
                mtrcResults.sum / resWeight
              } else {
                scala.math.pow(mtrcResults.product, 1.0 / resWeight)
              }
              (maxIndex, mtrcMax, mtrcMean, resWeight)
            }

            // Compute results from cache
            val (cMaxIndex, cMtrcMax, cMtrcMean, cWeight) = if (!_instCache.contains(mtrc)) {
              (-1, Double.NegativeInfinity, 0.0, 0)
            } else {
              val mtrcResults = _instCache(mtrc)
              val resWeight = {
                val first = if (wasCollapsed) _cacheCollapseCount * maxCacheSize else 1
                val rest  = mtrcResults.size - 1
                first + rest
              }
              val (mtrcMax, maxIndex) = mtrcResults.map { case (maxInd, mtrcMax, _) => (mtrcMax, maxInd) }.maxBy(_._1)
              val mtrcMean = if (!wasCollapsed) {
                if (absolute) {
                  mtrcResults.map(_._3).sum / resWeight
                } else {
                  mtrcResults.map(res => scala.math.pow(res._3, 1.0 / resWeight)).product
                }
              } else {
                if (absolute) {
                  (mtrcResults.head._3 * _cacheCollapseCount * maxCacheSize + mtrcResults.tail.map(_._3).sum) / resWeight
                } else {
                  val firstW = (_cacheCollapseCount * maxCacheSize).toDouble / resWeight
                  val restW  = 1.0 / resWeight
                  scala.math.pow(mtrcResults.head._3, firstW) * mtrcResults.tail.map(res => scala.math.pow(res._3, restW)).product
                }
              }
              (maxIndex, mtrcMax, mtrcMean, resWeight * maxCacheSize)
            }

            // Combine results and store the final value
            val (mMaxIndex, mMtrcMax, mMtrcMean) = {
              val (mtrcMax, maxIndex) = if (sMtrcMax > cMtrcMax) {
                (sMtrcMax, sMaxIndex + cWeight)
              } else {
                (cMtrcMax, cMaxIndex)
              }
              val mtrcMean = if (absolute) {
                (sMtrcMean * sWeight + cMtrcMean * cWeight) / (sWeight + cWeight)
              } else {
                scala.math.pow(sMtrcMean, sWeight.toDouble / (sWeight + cWeight)) * scala.math.pow(cMtrcMean, cWeight.toDouble / (sWeight + cWeight))
              }
              (maxIndex, mtrcMax, mtrcMean)
            }
            _instResults(mtrc) = ((mMaxIndex, mMtrcMax, mMtrcMean))

          case mtrc: HistoryBased =>
            // Compute results from samples
            val (sValue, sWeight) = if (_samples.isEmpty) (0.0, 0) else (mtrc.compute(_samples), _samples.size)

            // Compute results from cache
            val (cValue, cWeight) = if (!_histCache.contains(mtrc)) {
              (0.0, 0)
            } else {
              val mtrcResults = _histCache(mtrc)
              val resWeight = {
                val first = if (wasCollapsed) _cacheCollapseCount * maxCacheSize else 1
                val rest  = mtrcResults.size - 1
                first + rest
              }
              val mtrcMean = if (!wasCollapsed) {
                if (absolute) {
                  mtrcResults.sum / resWeight
                } else {
                  scala.math.pow(mtrcResults.product, 1.0 / resWeight)
                }
              } else {
                if (absolute) {
                  (mtrcResults.head * _cacheCollapseCount * maxCacheSize + mtrcResults.tail.sum) / resWeight
                } else {
                  val firstW = (_cacheCollapseCount * maxCacheSize).toDouble / resWeight
                  val restW  = 1.0 / resWeight
                  scala.math.pow(mtrcResults.head, firstW) * mtrcResults.tail.map(res => scala.math.pow(res, restW)).product
                }
              }
              (mtrcMean, resWeight * maxCacheSize)
            }

            // Combine results and store the final value
            val mValue = if (absolute) {
              (sValue * sWeight + cValue * cWeight) / (sWeight + cWeight)
            } else {
              scala.math.pow(sValue, sWeight.toDouble / (sWeight + cWeight)) * scala.math.pow(cValue, cWeight.toDouble / (sWeight + cWeight))
            }
            _histResults(mtrc) = mValue
        }
      }

      // Mark the watcher as computed
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
    (maxCacheSize: Int) extends PortWatcher(approxPort, exactPort, metrics:_*)(maxCacheSize) {
    def report(): Report = {
      // First compute the metric values
      compute()

      // Then create a report for this watcher
      TrackerReport(portName(approxPort), portName(exactPort), metrics, resultMap)
    }
  }

  /** 
    * Represents a port constraint that supports verification
    * @todo extend to support early termination on instantaneous metrics
    */
  private[chiselverify] class Constraint(approxPort: Bits, exactPort: Bits, metrics: Metric*)
    (maxCacheSize: Int) extends PortWatcher(approxPort, exactPort, metrics:_*)(maxCacheSize) {
    def report(): Report = {
      // First compute the metric values
      compute()

      // Then create a report for this watcher
      ConstraintReport(portName(approxPort), portName(exactPort), metrics, resultMap)
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
            bs ++= f"- $mtrc metric is violated by error $mtrcValue!\n"
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
      new Tracker(approxPort, exactPort, metrics:_*)(MaxCacheSize)
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
      new Tracker(approxPort, exactPort, metrics:_*)(maxCacheSize)
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
      new Constraint(approxPort, exactPort, constraints:_*)(MaxCacheSize)
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
      new Constraint(approxPort, exactPort, constraints:_*)(maxCacheSize)
    }
  }
}
