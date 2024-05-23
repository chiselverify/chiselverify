package chiselverify.approximation

import scala.util.{Either, Left, Right}

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
    val id = s"ER(\n${watchers.map(wtchr => s" - ${wtchr.id}\n")})"

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
  private[chiselverify] case class TrackerReport(approxPort: String, metrics: Iterable[Metric],
                                                 results: Map[Metric, Either[(Int, Double, Double), Double]])
    extends Report {
    // Create an identifier for this tracker report
    val id = s"T($approxPort)"

    def report(): String = metrics match {
      case Nil => 
        s"Tracker on port $approxPort has no metrics!\n"
      case _ =>
        val bs = new StringBuilder(s"Tracker on port $approxPort has results:\n")
        metrics.foreach { mtrc =>
          bs ++= "- "
          (mtrc, results(mtrc)) match {
            // For instantaneous metrics, report the mean and maximum found error
            case (_: Instantaneous, Left((_, mtrcMax, mtrcMean))) =>
              bs ++= f"Instantaneous $mtrc metric has mean $mtrcMean and maximum $mtrcMax"

            // For history-based metrics, report the found error
            case (_: HistoryBased, Right(mtrcVal)) =>
              bs ++= f"History-based $mtrc metric has value $mtrcVal"

            case _ => throw new Exception("mismatched metric and result types")
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
  private[chiselverify] case class ConstraintReport(approxPort: String, metrics: Iterable[Metric],
                                                    results: Map[Metric, Either[(Int, Double, Double), Double]])
    extends Report {
    // Create an identifier for this constraint report
    val id = s"C($approxPort)"

    def report(): String = if (metrics.isEmpty) {
      s"Constraint on port $approxPort has no metrics!\n"
    } else {
      val bs = new StringBuilder(s"Constraint on port $approxPort has results:\n")
      metrics.foreach { mtrc =>
        bs ++= "- "
        (mtrc, results(mtrc)) match {
          // For instantaneous metrics:
          // - if satisfied, report this with the maximum found error
          // - if not, report this with the first violating error
          case (_: Instantaneous, Left((maxIndex, mtrcMax, _))) =>
            bs ++= s"Instantaneous $mtrc metric "
            if (mtrc.check(mtrcMax)) {
              bs ++= f"is satisfied with maximum error $mtrcMax"
            } else {
              bs ++= f"is violated by maximum error $mtrcMax (sample #$maxIndex)"
            }

          // For history-based metrics:
          // - if satisfied, report this with ...
          // - if not, report this with ...
          case (_: HistoryBased, Right(mtrcVal)) =>
            bs ++= s"History-based $mtrc metric "
            if (mtrc.check(mtrcVal)) {
              bs ++= f"is satisfied with error $mtrcVal"
            } else {
              bs ++= f"is violated by error $mtrcVal"
            }

          case _ => throw new Exception("mismatched metric and result types")
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
