package chiselverify.approximation

import chiselverify.approximation.Metrics.{HistoryBased, Instantaneous, Metric}

object Reporting {

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

    override def toString(): String = report
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

    override def toString(): String = report
  }

  /** 
    * Contains the error report for a `Tracker`
    */
  private[chiselverify] case class TrackerReport(approxPort: String, exactPort: String, metrics: Iterable[Metric], results: Map[Metric, Seq[Double]])
    extends Report {
    // Create an identifier for this tracker report
    val id = s"T($approxPort, $exactPort)"

    def report(): String = metrics match {
      case Nil => 
        s"Tracker on ports $approxPort and $exactPort has no metrics!\n"
      case _ =>
        val bs = new StringBuilder(s"Tracker on ports $approxPort and $exactPort has results:\n")
        metrics.foreach { mtrc =>
          val mtrcResults = results(mtrc)
          bs ++= "- "
          mtrc match {
            // For instantaneous metrics, report the mean and maximum found error
            case _: Instantaneous =>
              val (mean, max) = (mtrcResults.sum / mtrcResults.length, mtrcResults.max)
              bs ++= f"Instantaneous ${mtrc} metric has mean $mean%.3f and maximum $max%.3f"

            // For history-based metrics, report the found error
            case _: HistoryBased =>
              bs ++= f"History-based ${mtrc} metric has value ${mtrcResults.head}%.3f"
          }
          bs ++= "!\n"
        }
        bs.mkString
    }

    def +(that: Report): Report = that match {
      case ErrorReport(watchers) => ErrorReport(watchers ++ Seq(this))
      case _ => ErrorReport(Seq(this, that))
    }

    override def toString(): String = report
  }

  /** 
    * Contains the error report for a `Constraint`
    */
  private[chiselverify] case class ConstraintReport(approxPort: String, exactPort: String, metrics: Iterable[Metric], results: Map[Metric, Seq[Double]])
    extends Report {
    // Create an identifier for this constraint report
    val id = s"C($approxPort, $exactPort)"

    def report(): String = metrics match {
      case Nil =>
        s"Constraint on ports $approxPort and $exactPort has no metrics!\n"
      case _ =>
        val bs = new StringBuilder(s"Constraint on ports $approxPort and $exactPort has results:\n")
        metrics.foreach { mtrc =>
          val mtrcResults   = results(mtrc)
          val mtrcSatisfied = mtrcResults.map(mtrc.check(_)).forall(s => s)
          bs ++= "- "
          mtrc match {
            // For instantaneous metrics:
            // - if satisfied, report this with the maximum found error
            // - if not, report this with the first violating error
            case _: Instantaneous =>
              bs ++= s"Instantaneous ${mtrc} metric "
              if (mtrcSatisfied) {
                bs ++= f"is satisfied with maximum error ${mtrcResults.max}%.3f"
              } else {
                val (violatingErr, violatingInd) = mtrcResults
                  .zipWithIndex
                  .collectFirst { case (err, ind) if !mtrc.check(err) => (err, ind) }.get
                bs ++= f"is violated by error $violatingErr%.3f (sample #$violatingInd)"
              }
            
            // For history-based metrics:
            // - if satisfied, report this with ...
            // - if not, report this with ...
            case _: HistoryBased =>
              bs ++= s"History-based ${mtrc} metric "
              if (mtrcSatisfied) {
                bs ++= f"is satisfied with error ${mtrcResults.head}%.3f"
              } else {
                bs ++= f"is violated by error ${mtrcResults.head}%.3f"
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

    override def toString(): String = report
  }
}
