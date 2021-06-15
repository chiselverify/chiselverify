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
import chiselverify.timing._

package object coverage {
    /**
      * Represents a group of cover points that will be sampled simultaneously
      *
      * @param id      a unique identifier for the group
      * @param points  the cover points that are grouped together
      * @param crosses the cross points contained in the group
      */
    case class CoverGroup(id: BigInt, points: List[Cover], crosses: List[Cross])

    abstract class Cover(val portName: String, val ports: Seq[Data]) {
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

    case class Condition(name: String, cond: Seq[BigInt] => Boolean, expectedHits: Option[BigInt] = None) {
        def apply(args: Seq[BigInt]): Boolean = cond(args)
        def report: String = s"CONDITION $name"
        def +(that: Condition): Condition = Condition(s"$name + ${that.name}", (x : Seq[BigInt]) => cond(x) && that.cond(x))
    }

    case class CoverCondition(pN: String, p: Data*)(conds: Condition*)
        extends Cover(pN, p.toList) {
        val conditions: List[Condition] = conds.toList

        override def report(db: CoverageDB): Report = ConditionReport(portName, conditions, db)

        override def serialize: String = s"CoverCondition($ports, $portName)($conds)"

        override def sample(db: CoverageDB): Unit = {
            val pointVals = ports.map(_.peek().asUInt().litValue())
            db.addConditionalHit(conditions.filter(c => c(pointVals)).map(_.name), pointVals)
        }

        override def register(db: CoverageDB): Unit = {
            //Register the coverpoint
            db.registerCoverPoint(portName, this)

            //Register the conditions
            db.registerConditions(portName, conditions)
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

        override def serialize: String = s"CoverPoint($port, $portName)(${bins.map(_.serialize)})"

        override def sample(db: CoverageDB): Unit = {
            //Check for the ports & sample all bins
            val pointVal = port.peek().asUInt().litValue().toInt
            bins.foreach(_.sample(portName, pointVal, db))
        }

        override def report(db: CoverageDB): Report =
            PointReport(portName, bins.map(b => BinReport(b, db.getNHits(portName, b.name))))

        override def register(db: CoverageDB): Unit = db.registerCoverPoint(portName, this)
    }

    abstract class Cross(val name: String, val pointName1: String, val pointName2: String, val bins: List[CrossBin]) {

        /**
          * Samples the current cross relation using a given database
          *
          * @param db the current database used throughout the test suits
          * @return the two points that were sampled during this cross sampling
          */
        def sample(db: CoverageDB): Option[(Cover, Cover)]

        /**
          * Registers the current cross point with the given coverage DB
          *
          * @param db the database used for the current test suite
          */
        def register(db: CoverageDB): Unit
    }

    /**
      * Represents a coverage relation between two different DUT ports
      *
      * @param name       the name that will be used to represent the relation in the report
      * @param pointName1 the first point of the relation
      * @param pointName2 the other point in the relation
      * @param bins       the list of value ranges that will be checked for for the given relation
      */
    case class CrossPoint(override val name: String, override val pointName1: String, override val pointName2: String)(b: CrossBin*)
        extends Cross(name, pointName1, pointName2, b.toList) {

        override val bins: List[CrossBin] = b.toList

        override def sample(db: CoverageDB): Option[(Cover, Cover)] = {

            db.getPointsFromCross(this) match {
                case (point1: CoverPoint, point2: CoverPoint) =>
                    val pointVal1 = point1.port.peek().asUInt().litValue()
                    val pointVal2 = point2.port.peek().asUInt().litValue()

                    //Sample the points individually first
                    point1.sample(db)
                    point2.sample(db)

                    //Sample the cross bins
                    bins.foreach(cb => {
                        if ((cb.range1 contains pointVal1) && (cb.range2 contains pointVal2)) {
                            db.addCrossBinHit(cb, (pointVal1, pointVal2))
                        }
                    })

                    Some(point1, point2)

                case _ => None
            }
        }

        override def register(db: CoverageDB): Unit = db.registerCross(this)
    }

    /**
      * A timed version of a cross point. This means that, given a delay, point 2 will be sampled a certain amount of 
      * cycles after point 1.
      *
      * @param name       the name of the cross, used for the report
      * @param delay      the number of cycles between sampling point1 and point2
      * @param pointName1 the first point that will be sampled
      * @param pointName2 the point that will be sampled ${delay} cycles after point1 
      * @param bins       the list of value ranges that will be checked for for the given relation
      */
    case class TimedCross(override val name: String, override val pointName1: String, override val pointName2: String,
                          delay: DelayType)(b: CrossBin*) extends Cross(name, pointName1, pointName2, b.toList) {
        override val bins: List[CrossBin] = b.toList

        private var initCycle: Option[BigInt] = None

        override def sample(db: CoverageDB): Option[(CoverPoint, CoverPoint)] = {
            //Sanity check
            if (initCycle.isEmpty) throw new IllegalStateException("Timed relation hasn't been registered!")

            def sampleBins(portNames: (String, String), tBins: List[CrossBin], values: (BigInt, BigInt), cycle: BigInt): Unit =
                tBins.foreach(tBin => {
                    if (tBin.range1 contains values._1) {
                        db.addTimedBinHit(portNames._1, tBin.bin1Name, values._1, cycle)
                    }

                    if (tBin.range2 contains values._2) {
                        db.addTimedBinHit(portNames._2, tBin.bin2Name, values._2, cycle)
                    }
                })

            db.getPointsFromCross(this) match {
                case (point1: CoverPoint, point2: CoverPoint) =>
                    val pointVal1 = point1.port.peek().asUInt().litValue()
                    val pointVal2 = point2.port.peek().asUInt().litValue()

                    //Sample the points at the current cycle
                    val curCycle = db.getCurCycle
                    sampleBins((point1.portName, point2.portName), bins, (pointVal1, pointVal2), curCycle)
            }

            None
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
    case class CrossBin(name: String, range1: Range, range2: Range) {
        val bin1Name: String = s"${name}_1"
        val bin2Name: String = s"${name}_2"

        def ==(that: CrossBin): Boolean = (name == that.name) &&
            (range1.start == that.range1.start) && (range1.end == that.range1.end) &&
            (range2.start == that.range2.start) && (range2.end == that.range2.end)
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
