package chiselverify.approximation

import chiselverify.approximation.Metrics.{HistoryBased, Instantaneous, Metric}

private[chiselverify] object Reporting {

  /** 
    * Class that can generate a part of a report
    */
  private[chiselverify] abstract class Report {
    val id: String

    /** 
      * Generates a part of or the entirity of a report
      * @return a string containing a report
      */
    def report(): String

    /** 
      * Adds another report to this one
      * @param that another report
      * @return a concatenation of the two reports
      */
    def +(that: Report): Report

    override def toString(): String = report()
  }

  /** 
    * Contains error reports of several `PortWatcher`s
    * @param watchers port watchers contained in this report
    */
  private[chiselverify] case class ErrorReport(watchers: Iterable[Report]) extends Report {
    // Create an identifier for this error report
    val id = s"ER(${watchers.map(wtchr => s" - ${wtchr.id}\n")})"

    def report(): String = {
      // Generate reports for all contained watchers
      val watcherReports = watchers.toSeq.sortBy(_.id).map(_.report())
      
      // Combine the reports into one
      val bs = new StringBuilder()
      watcherReports.foreach { rprt =>
        bs ++= rprt
      }
      bs.mkString
    }

    def +(that: Report): Report = that match {
      case errRep: ErrorReport => ErrorReport(errRep.watchers ++ watchers)
      case _ => ErrorReport(watchers ++ Seq(that))
    }

    override def toString(): String = report()
  }

  /** 
    * Contains the error report for a `Tracker`
    */
  private[chiselverify] case class TrackerReport(approxPort: String, exactPort: String, metrics: Iterable[Metric], results: Map[Metric, Any])
    extends Report {
    // Create an identifier for this tracker report
    val id = s"T($approxPort, $exactPort)"

    def report(): String = if (metrics.isEmpty) {
      s"Tracker on ports $approxPort and $exactPort has no metrics!\n"
    } else {
      val bs = new StringBuilder(s"Tracker on ports $approxPort and $exactPort has results:\n")
        metrics.foreach { mtrc =>
        val mtrcResults = results(mtrc)
        bs ++= "- "
        mtrc match {
          // For instantaneous metrics, report the mean and maximum found error
          case _: Instantaneous =>
            val (_, mtrcMax, mtrcMean) = mtrcResults.asInstanceOf[(Int, Double, Double)]
            bs ++= f"Instantaneous $mtrc metric has mean $mtrcMean and maximum $mtrcMax"

            // For history-based metrics, report the found error
          case _: HistoryBased =>
            val mtrcVal = mtrcResults.asInstanceOf[Double]
            bs ++= f"History-based $mtrc metric has value $mtrcVal"
        }
        bs ++= "!\n"
      }
      bs.mkString
    }

    def +(that: Report): Report = that match {
      case ErrorReport(watchers) => ErrorReport(watchers ++ Seq(this))
      case _ => ErrorReport(Seq(this, that))
    }

    override def toString(): String = report()
  }

  /** 
    * Contains the error report for a `Constraint`
    */
  private[chiselverify] case class ConstraintReport(approxPort: String, exactPort: String, metrics: Iterable[Metric], results: Map[Metric, Any])
    extends Report {
    // Create an identifier for this constraint report
    val id = s"C($approxPort, $exactPort)"

    def report(): String = if (metrics.isEmpty) {
      s"Constraint on ports $approxPort and $exactPort has no metrics!\n"
    } else {
      val bs = new StringBuilder(s"Constraint on ports $approxPort and $exactPort has results:\n")
      metrics.foreach { mtrc =>
        val mtrcResults = results(mtrc)
        bs ++= "- "
        mtrc match {
          // For instantaneous metrics:
          // - if satisfied, report this with the maximum found error
          // - if not, report this with the first violating error
          case _: Instantaneous =>
            val (maxIndex, mtrcMax, _) = mtrcResults.asInstanceOf[(Int, Double, Double)]
            bs ++= s"Instantaneous $mtrc metric "
            if (mtrc.check(mtrcMax)) {
              bs ++= f"is satisfied with maximum error $mtrcMax"
            } else {
              bs ++= f"is violated by maximum error $mtrcMax (sample #$maxIndex)"
            }

          // For history-based metrics:
          // - if satisfied, report this with ...
          // - if not, report this with ...
          case _: HistoryBased =>
            val mtrcVal = mtrcResults.asInstanceOf[Double]
            bs ++= s"History-based $mtrc metric "
            if (mtrc.check(mtrcVal)) {
              bs ++= f"is satisfied with error $mtrcVal"
            } else {
              bs ++= f"is violated by error $mtrcVal"
            }
        }
        bs ++= "!\n"
      }
      bs.mkString
    }

    def +(that: Report): Report = that match {
      case ErrorReport(watchers) => ErrorReport(watchers ++ Seq(this))
      case _ => ErrorReport(Seq(this, that))
    }

    override def toString(): String = report()
  }
}
