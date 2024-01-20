package chiselverify.approximation

import chisel3.Bits
import chisel3.experimental.DataMirror.{checkTypeEquivalence, directionOf, widthOf}
import chiseltest._
import scala.collection.mutable
import scala.util.{Either, Left, Right}

import chiselverify.approximation.Reporting.{ConstraintReport, Report, TrackerReport}
import chiselverify.approximation.Metrics.{isAbsolute, HistoryBased, Instantaneous, Metric}

private[chiselverify] object Watchers {

  /** 
    * Represents the expected value style of a generic port watcher of which there are 
    * two types:
    * 
    * - Reference-based watchers that require expected values when sampled
    * - Port-based watchers that require an exact port to peek when sampled
    */
  private[chiselverify] sealed abstract trait WatcherStyle

  /** 
    * Represents a reference-based 
    */
  private[chiselverify] sealed abstract trait ReferenceBased extends WatcherStyle

  /** 
    * Represents a super-type of port-based watchers
    */
  private[chiselverify] sealed abstract trait PortBased extends WatcherStyle {
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
    * @param metrics metrics to use in this watcher
    * @param maxCacheSize the maximum cache size to use in this watcher
    */
  private[chiselverify] sealed abstract class Watcher(val approxPort: Bits, val metrics: Metric*)(maxCacheSize: Int) {
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
    private[chiselverify] def resultMap: Map[Metric, Either[(Int, Double, Double), Double]] = {
      _instResults.map { case (k, v) => k -> Left [(Int, Double, Double), Double](v) }.toMap ++
      _histResults.map { case (k, v) => k -> Right[(Int, Double, Double), Double](v) }.toMap
    }

    /** 
      * Samples the approximate port of the watcher and stores its value with a 
      * given expected value internally
      * @param expected expected value of the exact port
      */
    def sample(expected: BigInt): Unit = {
      metrics match {
        case Nil =>
        case _ =>
          _samples += ((approxPort.peek().litValue, expected))
          collapse()
      }

      // Mark the watcher as non-computed
      _computed = false
    }

    /** 
      * Resets the state of the watcher
      */
    def reset(): Unit = {
      _samples.clear()

      _cacheCollapseCount = 0
      _instCache.clear()
      _histCache.clear()

      _computed = false
      _instResults.clear()
      _histResults.clear()
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
            val (mtrcMax, locIndex) = mtrcResults.zipWithIndex.maxBy(_._1)

            // Compute the maximum error's global sample index
            val maxIndex = {
              val globOffset = if (_cacheCollapseCount == 0) {
                if (_instCache.contains(mtrc)) _instCache(mtrc).size * maxCacheSize else 0
              } else
                (_cacheCollapseCount * maxCacheSize + _instCache(mtrc).size - _cacheCollapseCount) * maxCacheSize
              globOffset + locIndex
            }

            // If the metric mixes in `Absolute`, we compute arithmetic means; if it mixes in 
            // `Relative`, we compute geometric means instead
            val mtrcMean = if (isAbsolute(mtrc)) {
              mtrcResults.sum / mtrcResults.size
            } else {
              mtrcResults.map(res => scala.math.pow(res, 1.0 / mtrcResults.size)).product
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
              val (mtrcMax, locIndex) = mtrcResults.zipWithIndex.maxBy(_._1)

              // Compute the maximum error's global sample index
              val maxIndex = {
                val globOffset = if (_cacheCollapseCount == 0) {
                  if (_instCache.contains(mtrc)) _instCache(mtrc).size * maxCacheSize else 0
                } else
                  (_cacheCollapseCount * maxCacheSize + _instCache(mtrc).size - _cacheCollapseCount) * maxCacheSize
                globOffset + locIndex
              }

              val mtrcMean = if (absolute) {
                mtrcResults.sum / resWeight
              } else {
                mtrcResults.map(res => scala.math.pow(res, 1.0 / resWeight)).product
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
                (sMtrcMax, sMaxIndex)
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
                  mtrcResults.map(res => scala.math.pow(res, 1.0 / resWeight)).product
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
    * Represents a tracker that does not support verification
    */
  private[chiselverify] sealed abstract class Tracker(approxPort: Bits, metrics: Metric*)(maxCacheSize: Int)
    extends Watcher(approxPort, metrics:_*)(maxCacheSize) {
    def report(): Report = {
      // First compute the metric values
      compute()

      // Then create a report for this watcher
      TrackerReport(portName(approxPort), metrics, resultMap)
    }
  }

  /** 
    * Represents a plain tracker
    */
  private[chiselverify] sealed class PlainTracker(approxPort: Bits, metrics: Metric*)(maxCacheSize: Int)
    extends Tracker(approxPort, metrics:_*)(maxCacheSize) with ReferenceBased

  /** 
    * Represents a port-based tracker
    * @param exactPort port of the exact DUT to track
    * 
    * @note Requires given pairs of ports to have the same type, direction and width.
    */
  private[chiselverify] sealed class PortTracker(approxPort: Bits, val exactPort: Bits, metrics: Metric*)
    (maxCacheSize: Int) extends Tracker(approxPort, metrics:_*)(maxCacheSize) with PortBased {
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
  private[chiselverify] sealed abstract class Constraint(approxPort: Bits, metrics: Metric*)(maxCacheSize: Int)
    extends Watcher(approxPort, metrics:_*)(maxCacheSize) {
    def report(): Report = {
      // First compute the metric values
      compute()

      // Then create a report for this watcher
      ConstraintReport(portName(approxPort), metrics, resultMap)
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
        bs ++= s"Verification results of constraint on port ${portName(approxPort)}\n"
        val satisfieds = metrics.map { mtrc =>
          val (mtrcSatisfied, mtrcValue) = (mtrc, resultMap(mtrc)) match {
            case (_: Instantaneous, Left((_, mtrcMax, _))) =>
              (mtrc.check(mtrcMax), mtrcMax)
            case (_: HistoryBased, Right(mtrcVal)) =>
              (mtrc.check(mtrcVal), mtrcVal)
            case _ => throw new Exception("mismatched metric and result types")
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
    * Represents a plain constraint
    */
  private[chiselverify] sealed class PlainConstraint(approxPort: Bits, metrics: Metric*)(maxCacheSize: Int)
    extends Constraint(approxPort, metrics:_*)(maxCacheSize) with ReferenceBased

  /** 
    * Represents a port-based constraint
    * @param exactPort port of the exact DUT to track
    * 
    * @note Requires given pairs of ports to have the same type, direction and width.
    */
  private[chiselverify] sealed class PortConstraint(approxPort: Bits, val exactPort: Bits, metrics: Metric*)
    (maxCacheSize: Int) extends Constraint(approxPort, metrics:_*)(maxCacheSize) with PortBased {
    // Verify that the ports have the same type, direction and width
    private[PortConstraint] val errMsg: String = "pairs of watched ports must have"
    require(checkTypeEquivalence(approxPort, exactPort), s"$errMsg the same type")
    require(directionOf(approxPort) == directionOf(exactPort), s"$errMsg the same direction")
    require(widthOf(approxPort) == widthOf(exactPort), s"$errMsg the same width")

    def sample(): Unit = sample(exactPort.peek().litValue)
  }
}
