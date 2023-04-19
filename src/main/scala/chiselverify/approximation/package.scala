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
    * @param approxPort port of the approximate DUT to track
    * @param exactPort port of the exact DUT to track
    * @param metrics metrics to use in this watcher
    * 
    * @note Requires given pairs of ports to have the same type, direction and width.
    */
  private[chiselverify] abstract class PortWatcher(approxPort: Bits, exactPort: Bits, metrics: Metric*) {
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
      * Collapses the internal sample storage and cache if either has exceeded `MaxCacheSize` elements
      * 
      * @todo Double-check all the math in this method!
      */
    private[chiselverify] def collapse(): Unit = {
      // If the sample storage has more than `MaxCacheSize` elements, collapse and clear it
      if (_samples.length >= MaxCacheSize) {
        metrics.foreach { mtrc =>
          // If the metric mixes in `Absolute`, we compute arithmetic means; if it mixes in 
          // `Relative`, we compute geometric means instead
          val absolute = isAbsolute(mtrc)

          // Now compute the metric value and cache it
          mtrc match {
            case mtrc: Instantaneous =>
              val mtrcResults = mtrc.compute(_samples)
              val (mtrcMax, maxIndex) = mtrcResults.zipWithIndex.maxBy(_._1)
              val mtrcMean = if (absolute) {
                mtrcResults.sum / mtrcResults.size
              } else {
                scala.math.pow(mtrcResults.product, 1.0 / mtrcResults.size)
              }
              _instCache.getOrElseUpdate(mtrc, new mutable.ArrayBuffer[(Int, Double, Double)]) +=
                ((maxIndex, mtrcMax, mtrcMean))
            case mtrc: HistoryBased =>
              val mtrcResult = mtrc.compute(_samples)
              _histCache.getOrElseUpdate(mtrc, new mutable.ArrayBuffer[Double]) += mtrcResult
          }
        }
        _samples.clear()
      }

      // If the caches have more than `MaxCacheSize` elements, collapse them and increment the counter
      if ((_instCache.count(_._2.length >= MaxCacheSize) >= 1) ||
          (_histCache.count(_._2.length >= MaxCacheSize) >= 1)) {
        metrics.foreach { mtrc =>
          // If the metric mixes in `Absolute`, we compute arithmetic means; if it mixes in 
          // `Relative`, we compute geometric means instead
          val absolute = isAbsolute(mtrc)
          
          // Now collapse the caches
          mtrc match {
            case mtrc: Instantaneous =>
              val mtrcResults = _instCache(mtrc)
              val weight = _cacheCollapseCount * MaxCacheSize + mtrcResults.size - 1
              val (mtrcMax, maxIndex) = mtrcResults.map { case (maxInd, mtrcMax, _) => (mtrcMax, maxInd) }.maxBy(_._1)
              val mtrcMean = if (absolute) {
                val sum = if (_cacheCollapseCount == 0) {
                  mtrcResults.map(_._3).sum
                } else {
                  mtrcResults.head._3 * _cacheCollapseCount * MaxCacheSize + mtrcResults.tail.map(_._3).sum
                }
                sum / weight
              } else {
                val prod = if (_cacheCollapseCount == 0) {
                  mtrcResults.map(_._3).product
                } else {
                  scala.math.pow(mtrcResults.head._3, _cacheCollapseCount * MaxCacheSize) * mtrcResults.map(_._3).product
                }
                scala.math.pow(prod, 1.0 / weight)
              }
              _instCache.update(mtrc, mutable.ArrayBuffer((maxIndex, mtrcMax, mtrcMean)))
            case mtrc: HistoryBased =>
              val mtrcResults = _histCache(mtrc)
              val weight = _cacheCollapseCount * MaxCacheSize + mtrcResults.size - 1
              val mtrcMean = if (absolute) {
                val sum = if (_cacheCollapseCount == 0) {
                  mtrcResults.sum
                } else {
                  mtrcResults.head * _cacheCollapseCount * MaxCacheSize + mtrcResults.tail.sum
                }
                sum / weight
              } else {
                val prod = if (_cacheCollapseCount == 0) {
                  mtrcResults.product
                } else {
                  scala.math.pow(mtrcResults.head, _cacheCollapseCount * MaxCacheSize) * mtrcResults.tail.product
                }
                scala.math.pow(prod, 1.0 / weight)
              }
              _histCache.update(mtrc, mutable.ArrayBuffer(mtrcMean))
          }
        }
        _cacheCollapseCount += 1
      }
    }

    /** 
      * Computes the metrics of the watcher and stores their values internally
      * 
      * @note Assumes there exist samples to compute metrics based on, if `metrics` is non-empty.
      */
    private[chiselverify] def compute(): Unit = {
      // Perform computation only if this watcher is not already computed
      if (!_computed && metrics.nonEmpty) {
        assume(_samples.nonEmpty || _instCache.nonEmpty || _histCache.nonEmpty,
          "cannot compute metrics without samples")
        metrics.foreach { mtrc =>
          // If the metric mixes in `Absolute`, we compute arithmetic means; if it mixes in 
          // `Relative`, we compute geometric means instead
          val absolute = isAbsolute(mtrc)

          // Now compute the metric
          mtrc match {
            case mtrc: Instantaneous =>
              // Compute results from samples
              val (sMaxIndex, sMtrcMax, sMtrcMean, sWeight) = {
                val mtrcResults = mtrc.compute(_samples)
                val (mtrcMax, maxIndex) = mtrcResults.zipWithIndex.maxBy(_._1)
                val weight = mtrcResults.size
                val mtrcMean = if (absolute) {
                  mtrcResults.sum / weight
                } else {
                  scala.math.pow(mtrcResults.product, 1.0 / weight)
                }
                (maxIndex, mtrcMax, mtrcMean, weight)
              }
              // Compute results from cache
              val (cMaxIndex, cMtrcMax, cMtrcMean, cWeight) = if (!_instCache.contains(mtrc)) {
                (-1, Double.NegativeInfinity, 0.0, 0)
              } else {
                val mtrcResults = _instCache(mtrc)
                val (mtrcMax, maxIndex) = mtrcResults.map { case (maxInd, mtrcMax, _) => (mtrcMax, maxInd) }.maxBy(_._1)
                if (_cacheCollapseCount == 0) {
                  val weight = mtrcResults.size
                  val mtrcMean = if (absolute) {
                    mtrcResults.map(_._3).sum / weight
                  } else {
                    scala.math.pow(mtrcResults.map(_._3).product, 1.0 / weight)
                  }
                  (maxIndex, mtrcMax, mtrcMean, weight * MaxCacheSize)
                } else {
                  val weight = _cacheCollapseCount * MaxCacheSize + mtrcResults.size - 1
                  val mtrcMean = if (absolute) {
                    (mtrcResults.head._3 * _cacheCollapseCount * MaxCacheSize + mtrcResults.tail.map(_._3).sum) / weight
                  } else {
                    scala.math.pow(scala.math.pow(mtrcResults.head._3, _cacheCollapseCount * MaxCacheSize) * mtrcResults.tail.map(_._3).product, 1.0 / weight)
                  }
                  (maxIndex, mtrcMax, mtrcMean, weight * MaxCacheSize)
                }
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
                  scala.math.pow(scala.math.pow(sMtrcMean, sWeight) * scala.math.pow(cMtrcMean, cWeight), 1.0 / (sWeight + cWeight))
                }
                (maxIndex, mtrcMax, mtrcMean)
              }
              _instResults(mtrc) = ((mMaxIndex, mMtrcMax, mMtrcMean))
            case mtrc: HistoryBased =>
              // Compute results from samples
              val (sValue, sWeight) = (mtrc.compute(_samples), _samples.size)
              // Compute results from cache
              val (cValue, cWeight) = if (!_histCache.contains(mtrc)) {
                (0.0, 0)
              } else {
                val mtrcResults = _histCache(mtrc)
                if (_cacheCollapseCount == 0) {
                  val weight = mtrcResults.size
                  val mtrcMean = if (absolute) {
                    mtrcResults.sum / weight
                  } else {
                    scala.math.pow(mtrcResults.product, 1.0 / weight)
                  }
                  (mtrcMean, weight * MaxCacheSize)
                } else {
                  val weight = _cacheCollapseCount * MaxCacheSize + mtrcResults.size - 1
                  val mtrcMean = if (absolute) {
                    (mtrcResults.head * _cacheCollapseCount * MaxCacheSize + mtrcResults.tail.sum) / weight
                  } else {
                    scala.math.pow(scala.math.pow(mtrcResults.head, _cacheCollapseCount * MaxCacheSize) * mtrcResults.tail.product, 1.0 / weight)
                  }
                  (mtrcMean, weight * MaxCacheSize)
                }
              }
              // Combine results and store the final value
              val mValue = if (absolute) {
                (sValue * sWeight + cValue * cWeight) / (sWeight + cWeight)
              } else {
                scala.math.pow(scala.math.pow(sValue, sValue) * scala.math.pow(cValue, cWeight), 1.0 / (sWeight + cWeight))
              }
              _histResults(mtrc) = mValue
          }
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
    extends PortWatcher(approxPort, exactPort, metrics:_*) {
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
    extends PortWatcher(approxPort, exactPort, metrics:_*) {
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
