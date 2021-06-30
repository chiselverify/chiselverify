/*
* Copyright 2020 DTU Compute - Section for Embedded Systems Engineering
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
* or implied. See the License for the specific language governing
* permissions and limitations under the License.
*/
package chiselverify

import chisel3.Data
import chiseltest.testableData
import chiselverify.coverage.CoverReport._
import chiselverify.timing.TimedOp._
import chiselverify.timing._

package object coverage {
    /**
      * Represents a group of cover points that will be sampled simultaneously
      *
      * @param id      a unique identifier for the group
      * @param points  the cover points that are grouped together
      * @param crosses the cross points contained in the group
      */
    case class CoverGroup(id: BigInt, points: List[Cover])

    /**
      * Represents the generic notion of a CoverPoint.
      * @param portName the readable name used by the reporter.
      * @param ports a sequence of ports that are associated to this point.
      */
    abstract class Cover(val pointName: String, val ports: Seq[Data]) {
        override def toString: String = serialize

        /**
          * Generates a report in a form that is compatible with the coverage reporter
          *
          * @return a Report
          */
        def report(db: CoverageDB): Report

        /**
          * Converts the current cover construct into a human-readable string
          *
          * @return a String representing the cover construct
          */
        def serialize: String

        /**
          * Samples the current cover construct
          *
          * @param db the coverage DataBase we are sampling in
          */
        def sample(db: CoverageDB): Unit

        /**
          * Register the current point in the given database
          * @param db the database in which we want to register the point
          */
        def register(db: CoverageDB): Unit
    }

    /**
      * Represents the generic idea of a cross point
      * @param name the readable name of the cross point, which will be used by the reporter.
      * @param pointName1 the name of the 1st point associated to the cross relation.
      *                   Must have already been registered before it can be used in the cross point.
      * @param pointName2 the name of the 2nd point associated to the cross relation.
      *                   Must have already been registered before it can be used in the cross point.
      * @param bins the set of bins associated to the relation.
      */
    abstract class Cross(val name: String, ports: Seq[Data])(val bins: List[CrossBin]) extends Cover(name, ports) {
        /**
          * Generates a report in a form that is compatible with the coverage reporter
          *
          * @return a Report
          */
        def report(db: CoverageDB): Report

        /**
          * Converts the current cover construct into a human-readable string
          *
          * @return a String representing the cover construct
          */
        def serialize: String

        /**
          * Samples the current cross relation using a given database
          *
          * @param db the current database used throughout the test suits
          * @return the two points that were sampled during this cross sampling
          */
        override def sample(db: CoverageDB): Unit

        /**
          * Registers the current cross point with the given coverage DB
          *
          * @param db the database used for the current test suite
          */
        override def register(db: CoverageDB): Unit
    }

    /**
      * Represents a hit-consideration condition. Similar to a Bin, but with condition defined ranges.
      * @param name the name of the condition that will be used in the report
      * @param cond the condition function
      * @param expectedHits optional expected number of hits. This will add a coverage % field in the report.
      */
    case class Condition(name: String, cond: Seq[BigInt] => Boolean, expectedHits: Option[BigInt] = None) {
        /**
          * Applies the condition on given dut port values.
          * @param args a sequence of dut port values.
          * @return the result of condition on the given values.
          */
        def apply(args: Seq[BigInt]): Boolean = cond(args)

        /**
          * Generated the condition's "part" of the report.
          * @return a string containing the condition's readable name.
          */
        def report: String = s"CONDITION $name"

        /**
          * Adds two conditions together.
          * @param that the condition we are adding to our own.
          * @return a new condition containing the names of both conditions and a function that is valid only when
          *         both conditions are valid.
          */
        def +(that: Condition): Condition = Condition(s"$name + ${that.name}", (x : Seq[BigInt]) => cond(x) && that.cond(x))
    }

    /**
      * Represents a CoverPoint where the bins are defined by arbitrary conditions.
      * These special points can be related to multiple points.
      * @param pN the name of the point, used by the coverage reporter.
      * @param p a variable number of associated ports.
      * @param conds a variable number of conditions associated to the set of ports.
      */
    case class CoverCondition(pN: String, p: Data*)(conds: Condition*)
        extends Cover(pN, p.toList) {
        val conditions: List[Condition] = conds.toList

        override def report(db: CoverageDB): Report = ConditionReport(pointName, conditions, db)

        override def serialize: String = s"CoverCondition($ports, $pointName)($conds)"

        override def sample(db: CoverageDB): Unit = {
            val pointVals = ports.map(_.peek().asUInt().litValue())
            db.addConditionalHit(conditions.filter(c => c(pointVals)).map(_.name), pointVals)
        }

        override def register(db: CoverageDB): Unit = {
            //Register the coverpoint
            db.registerCoverPoint(pointName, this)

            //Register the conditions
            db.registerConditions(pointName, conditions)
        }
    }

    /**
      * Represents a single cover point that samples a given dut port
      *
      * @param p    the DUT port that will be sampled for this point
      * @param pN   the name that will be used to represent the point in the report
      * @param bins the list of value ranges that will be checked for for the given port
      */
    case class CoverPoint(pN: String, port: Data)(b: Bins*)
        extends Cover(pN, port::Nil) {
        val bins: List[Bins] = if(b.isEmpty) List(DefaultBin(port)) else b.toList

        override def serialize: String = s"CoverPoint($port, $pointName)(${bins.map(_.serialize)})"

        override def sample(db: CoverageDB): Unit = {
            //Check for the ports & sample all bins
            val pointVal = port.peek().asUInt().litValue().toInt
            bins.foreach(_.sample(pointName, pointVal, db))
        }

        override def report(db: CoverageDB): Report =
            PointReport(pointName, bins.map(b => BinReport(b, db.getNHits(pointName, b.name))))

        override def register(db: CoverageDB): Unit = db.registerCoverPoint(pointName, this)
    }

    /**
      * Represents a coverage relation between two different DUT ports
      *
      * @param name       the name that will be used to represent the relation in the report
      * @param pointName1 the first point of the relation
      * @param pointName2 the other point in the relation
      * @param bins       the list of value ranges that will be checked for for the given relation
      */
    case class CrossPoint(n: String, p: Data*)(b: CrossBin*) extends Cross(n, p)(b.toList) {

        //Check that the correct number of ranges was given
        override val bins: List[CrossBin] =
            if(b.forall(_.ranges.length == ports.length)) b.toList
            else throw new IllegalArgumentException(
                "CrossBins must contain the same number of ranges as the number of ports in the CrossPoint!"
            )

        override def sample(db: CoverageDB): Unit = {

            //Sample all of the ports
            val portVal = ports.map(_.peek().litValue())

            //Check for hits with each bin
            bins.foreach { b => if(portVal.zip(b.ranges).forall{ case (v, r) => r contains v })
                    db.addCrossBinHit(b, portVal)
            }
        }

        override def register(db: CoverageDB): Unit = db.registerCross(this)

        /**
          * Generates a report in a form that is compatible with the coverage reporter
          *
          * @return a Report
          */
        override def report(db: CoverageDB): Report =
            CrossReport(this, bins.map(cb => CrossBinReport(cb, db.getNHits(cb))))

        /**
          * Converts the current cover construct into a human-readable string
          *
          * @return a String representing the cover construct
          */
        override def serialize: String = s"CROSS $name"
    }

    /**
      * A timed version of a cross point. This means that, given a delay, point 2 will be sampled a certain amount of 
      * cycles after point 1.
      *
      * @param n        the name of the cross, used for the report
      * @param delay    the number of cycles between sampling point1 and point2
      * @param p1       the first port that will be sampled
      * @param p2       the second port that will be sampled
      * @param b      the first list of value ranges that will be checked for for the given relation
      */
    case class TimedCross(n: String, p1: Data, p2: Data)(val delay: DelayType)(b: CrossBin*)
        extends Cross(n, Seq(p1, p2))(b.toList) {

        //Check that the correct number of ranges was given
        override val bins: List[CrossBin] =
            if(b.forall(_.ranges.length == ports.length)) b.toList
            else throw new IllegalArgumentException(
                "CrossBins must contain the same number of ranges as the number of ports in the CrossPoint!"
            )

        //Used to keep track of time
        private var initCycle: Option[BigInt] = None

        override def sample(db: CoverageDB): Unit = {
            //Sanity check
            if (initCycle.isEmpty) throw new IllegalStateException("Timed relation hasn't been registered!")

            def sampleBins(tBins: Seq[CrossBin], values: Seq[BigInt], cycle: BigInt): Unit = {
                //Cries in Scala
                var idx = 0
                tBins.foreach(tBin =>
                    tBin.ranges.zip(values).foreach{ case (r, v) =>
                        if(r contains v) {
                            db.addTimedBinHit(tBin.binNames(idx), v, cycle)
                            idx += 1
                        }
                    }
                )
            }

            //Sample the points at the current cycle
            val curCycle = db.getCurCycle
            sampleBins(bins, ports.map(_.peek().litValue()), curCycle)
        }

        /**
          * Registers the cross relation with the db and schedules the second point
          *
          * @param db the database used for the current test suite
          */
        override def register(db: CoverageDB): Unit = {
            //Register our point as a regular cross point
            db.registerCross(this)

            //Initialize current cycle
            initCycle = Some(db.getCurCycle)
        }

        /**
          * Generates a report in a form that is compatible with the coverage reporter
          *
          * @return a Report
          */
        override def report(db: CoverageDB): Report = {

            /**
              * Go through the database and compile the number of timed hits into a number of delayed hits
              * @param t the timed cross point in question
              * @return a list of Cross bin => numHits mappings
              */
            def compileTimedHits(t: TimedCross): List[(CrossBin, BigInt)] =
                t.bins.map(cb => {
                    //Retrieve the timed hit samples for both ranges
                    val bin1cycles = db.getTimedHits(cb.binNames.head)
                    val bin2cycles = db.getTimedHits(cb.binNames(1))

                    //Compute the number of delay-synchronized hits
                    val groups = bin1cycles.zip(bin2cycles)
                    t.delay match {
                        case Exactly(delay) =>
                            (cb, BigInt(groups.filter(g => (g._1._2 + delay) == g._2._2).map(g => g._1._1).length))
                        case Eventually(delay) =>
                            (cb, BigInt(groups.filter(g => (g._2._2 - g._1._2) <= delay).map(g => g._1._1).length))
                        case Always(delay) => (cb,
                            if((0 until delay).forall(i => bin2cycles.map(_._2).contains(i) && bin1cycles.map(_._2).contains(i)))
                                BigInt(1)
                            else
                                BigInt(0))
                        case _ => (cb, BigInt(0))
                    }
                })
            //Sanity check
            if(db.getCurCycle == 0) throw new IllegalStateException("Clock must be stepped with CR!")
            CrossReport(this, compileTimedHits(this).map(e => CrossBinReport(e._1, e._2)), delay)
        }

        /**
          * Converts the current cover construct into a human-readable string
          *
          * @return a String representing the cover construct
          */
        override def serialize: String = s"TIMED CROSS $name WITH DELAY $delay"
    }

    /**
      * A value range that will be used for sampling
      *
      * @param name      the name of the value range that will be used to represent it in the report
      * @param range     the actual scala range
      * @param condition an extra condition that can be used to consider a hit
      */
    class Bins(val name: String, val rangeOpt: Option[Range], val condition: Condition = Condition("$$__def__$$", _ => true)) {
        def ==(that: Bins): Boolean = {
            val r = rangeOpt.getOrElse(0 to 0)
            val thatR = that.rangeOpt.getOrElse(0 to 0)
            (name == that.name) && (r.start == thatR.start) && (r.end == thatR.end)
        }

        def sample(portName: String, value: BigInt, coverageDB: CoverageDB): Unit =
            if (condition(List(value))) rangeOpt match {
                case None => coverageDB.addBinHit(portName, name, value)
                case Some(r) => if (r.contains(value)) coverageDB.addBinHit(portName, name, value)
            }

        def range: Range = rangeOpt.getOrElse(0 to 0)

        def serialize: String = s"Bin( $name, ${rangeOpt.getOrElse("")} ${if (condition.cond != ((_: List[BigInt]) => true)) condition})"
    }

    /**
      * Shorthand to simplify the Bin's API
      */
    object Bins {
        def apply(name: String, condition: Condition): Bins = new Bins(name, None, condition)
        def apply(name: String, range: Range): Bins = new Bins(name, Some(range))
        def apply(name: String, range: Range, condition: Condition) = new Bins(name, Some(range), condition)
    }


    /**
      * A range relation between two different ranges
      *
      * @param name   the name of the relation, used for the report
      * @param range1 the range that will be sampled for point1 of the relation
      * @param range2 the range that will be sampled for point2 of the relation
      */
    case class CrossBin(name: String, ranges: Range*) {
        val binNames: List[String] = ranges.indices.map(i => s"${name}_$i").toList

        def ==(that: CrossBin): Boolean = (name == that.name) && (ranges == that.ranges)
    }

    /**
      * Defines the default bins for both cover points and cross points
      */
    object DefaultBin {
        def defaultRange(port: Data): Range = 0 until math.pow(2, port.getWidth).toInt

        /**
          * Generates a default bin for a given cover point port
          *
          * @param port the port for which the bin will be generated
          * @return a bin covering all possible values for a given port
          */
        def apply(port: Data): Bins = Bins("default", defaultRange(port))

        /**
          * Generates a default bin for given cross point ports
          *
          * @param port1 the first point of the cross point
          * @param port2 the second point of the cross point
          * @return a cross bin covering all possible value combinations for the given ports
          */
        def apply(port1: Data, port2: Data): CrossBin = CrossBin("defaultCross", defaultRange(port1), defaultRange(port2))
    }
}
