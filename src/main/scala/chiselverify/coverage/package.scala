/*
* Copyright 2021 DTU Compute - Section for Embedded Systems Engineering
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
      * API end-point for the Cover constructs. Allows to define every type of cover points using different arguments
      * @param pointName the name of the cover construct ( used in the report ).
      * @param ports the ports associated to the cover construct.
      */
    case class cover(pointName: String, ports: Data*) {
        //Sanity check
        if(ports.isEmpty) throw new IllegalArgumentException("A Cover construct must cover ports!")

        /**
          * Allows the definition of cover points, cross points or cover conditions
          * @param b the bins associated to the point
          * @return a cover construct that uses the given arguments
          */
        def apply(b: Bin*) : CoverConst = {
            if(b.forall(bin => bin.isCondition)) CoverCondition(pointName, ports)(
                b.map(bin => Condition(bin.name, bin.condition, bin.expectedHits)))
            else if (ports.size == 1) CoverPoint(pointName, ports.head)(b)
            else CrossPoint(pointName, ports)(b)
        }

        /**
          * Allows the definition of timed cover constructs
          * @param delay the delay associated to the timed cross point
          * @param b the bins associated to the point
          * @return a timed cover construct that uses the given arguments
          */
        def apply(delay: DelayType)(b: Bin*): CoverConst = {
            if (ports.size != 2) throw new IllegalArgumentException(s"Timed coverage only works with two ports not ${ports.size}!")
            TimedCross(pointName, ports.head, ports.tail.head)(delay)(b)
        }
    }

    /**
      * Represents a group of cover points that will be sampled simultaneously
      *
      * @param id      a unique identifier for the group
      * @param points  the cover points that are grouped together
      */
    case class CoverGroup(id: BigInt, points: List[CoverConst])

    /**
      * Represents the generic notion of a CoverPoint.
      * @param pointName the readable name used by the reporter.
      * @param ports a sequence of ports that are associated to this point.
      */
    abstract class CoverConst(val pointName: String, val ports: Seq[Data]) {
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
      * @param ports the ports associated to the cross relation.
      * @param bins the set of bins associated to the relation.
      */
    private[chiselverify] abstract class CrossConst(val name: String, ports: Seq[Data])(val bins: List[CrossBin])
        extends CoverConst(name, ports) {
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
    private[chiselverify] case class CoverCondition(pN: String, p: Seq[Data])(conds: Seq[Condition])
        extends CoverConst(pN, p.toList) {
        val conditions: List[Condition] = conds.toList

        override def report(db: CoverageDB): Report = ConditionReport(pointName, conditions, db)

        override def serialize: String = s"CoverCondition($ports, $pointName)($conds)"

        override def sample(db: CoverageDB): Unit = {
            val pointVals = ports.map(_.peek().asUInt().litValue)
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
      * @param port the DUT port that will be sampled for this point
      * @param pN   the name that will be used to represent the point in the report
      * @param b    the list of value ranges that will be checked for for the given port
      */
    private[chiselverify] case class CoverPoint(pN: String, port: Data)(b: Seq[Bin])
        extends CoverConst(pN, Seq(port)) {
        val bins: List[Bin] = if (b.isEmpty) List(DefaultBin(port)) else b.toList

        override def serialize: String = s"CoverPoint($port, $pointName)(${bins.map(_.serialize)})"

        override def sample(db: CoverageDB): Unit = {
            //Check for the ports & sample all bins
            val pointVal = port.peek().asUInt().litValue.toInt
            bins.foreach(_.sample(pointName, pointVal, db))
        }

        override def report(db: CoverageDB): Report =
            PointReport(pointName, bins.map(b => BinReport(b, db.getNHits(pointName, b.name))))

        override def register(db: CoverageDB): Unit = db.registerCoverPoint(pointName, this)
    }

    /**
      * Representation of a sampled timing value. This is used for the TimedCoverOp construct.
      * @param operandNum either 1 or 2, represents which operand we are working with
      * @param value, the sampled value of the port
      * @param cycle, the cycle at which the value was sampled
      */
    private[chiselverify] case class TimingValue(operandNum: Int, value: BigInt, cycle: BigInt) {
        if(operandNum > 2 || operandNum < 0)
            throw new IllegalArgumentException(s"operand number can only be 1 or 2!!")
    }

    private[chiselverify] case class TimedCoverOp(pN: String, op: TimedOperator)(val delay: DelayType)
        extends CoverConst(pN, Seq(op.operand1, op.operand2)) {

        //Implicit reference to simplify internal DB function calls
        implicit val _this: TimedCoverOp = this

        override def report(db: CoverageDB): Report = {
            def compileHits: Int = {
                val timingVals = db.getTimingVals
                val op1Vals = timingVals.filter(_.operandNum == 1)
                val op2Vals = timingVals.filter(_.operandNum == 2)

                delay match {
                    //(op1, List of op2Vals where cycle is delay cycles later) then reduced by applying the operand
                    case Exactly(delay) => op1Vals.map(p => (p, op2Vals.filter((p.cycle + (delay - 1)) == _.cycle)))
                        .map { case (t, ts) => ts.map(t2 => op(t.value, t2.value)).count(_ == true) }.sum

                    //(op1, List of op2Vals where cycle is up to delay cycles later) then checked to find at least one
                    //entry than satisfies the operation
                    case Eventually(delay) => op1Vals.map(p =>
                        (p, op2Vals.filter(p2 => ((p.cycle + delay) >= p2.cycle) && (p.cycle <= p2.cycle))))
                        .filter { case (_, ts) => ts.size > delay }
                        .count { case (t, ts) => ts.exists(t2 => op(t.value, t2.value)) }

                    //(op1, List of op2Vals where cycle is up to delay cycles later) then checked to find out if all
                    //entries than satisfies the operation
                    case Always(delay) => op1Vals.map(p =>
                        (p, op2Vals.filter(p2 => ((p.cycle + delay) >= p2.cycle) && (p.cycle <= p2.cycle))))
                        .filter { case (_, ts) => ts.size > delay }
                        .count { case (t, ts) => ts.forall(t2 => op(t.value, t2.value)) }

                    case _ => throw new IllegalArgumentException(s"$delay DelayType not supported")
                }
            }

            TimedOpReport(this, compileHits)
        }

         def +(that: TimedCoverOp): TimedCoverOp = {
            if(delay != that.delay) throw new IllegalArgumentException("DELAYS MUST BE EQUAL IN ORDER TO SUM")
            else TimedCoverOp(s"$pointName + ${that.pointName}", op + that.op)(delay)
        }

        override def serialize: String = s"TIMED_COVER_OP $pointName${delay.toString}"

        override def sample(db: CoverageDB): Unit = {
            val curCycle = db.getCurCycle

            //Sample the two operands
            db.addTimingValue(TimingValue(1, op.operand1.peek().litValue, curCycle))
            db.addTimingValue(TimingValue(2, op.operand2.peek().litValue, curCycle))
        }

        override def register(db: CoverageDB): Unit = db.registerTimedCoverOp
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
    private[chiselverify] case class TimedCross(n: String, p1: Data, p2: Data)(val delay: DelayType)(b: Seq[CrossBin])
        extends CrossConst(n, Seq(p1, p2))(b.toList) {

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
            sampleBins(bins, ports.map(_.peek().litValue), curCycle)
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
                        case _ => (cb, BigInt(0)) //Never isn't supported for coverage
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
      * Represents a coverage relation between two different DUT ports
      *
      * @param n       the name that will be used to represent the relation in the report
      * @param p       the points of the relation
      * @param b       the list of value ranges that will be checked for for the given relation
      */
    private[chiselverify] case class CrossPoint(n: String, p: Seq[Data])(b: Seq[CrossBin]) extends CrossConst(n, p)(b.toList) {

        //Check that the correct number of ranges was given
        override val bins: List[CrossBin] =
            if(b.forall(_.ranges.length == ports.length)) b.toList
            else throw new IllegalArgumentException(
                "CrossBins must contain the same number of ranges as the number of ports in the CrossPoint!"
            )

        override def sample(db: CoverageDB): Unit = {

            //Sample all of the ports
            val portVal = ports.map(_.peek().litValue)

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
      * A value range that will be used for sampling
      *
      * @param name      the name of the value range that will be used to represent it in the report
      * @param ranges     the actual scala range
      * @param conditionOpt an extra condition that can be used to consider a hit
      */
    private[chiselverify] class Bin(val name: String, val ranges : Seq[Range] = Seq.empty,
                                      conditionOpt: Option[Seq[BigInt] => Boolean] = None, val expectedHits: Option[BigInt] = None) {

        val condition : Seq[BigInt] => Boolean = conditionOpt.getOrElse((_: Seq[BigInt]) => true)
        val isCondition: Boolean = conditionOpt.isDefined && ranges.isEmpty

        def ==(that: Bin): Boolean = {
            (name == that.name) &&
                (ranges.zip(that.ranges) forall { case (r1, r2) => r1.start == r2.start && r1.end == r2.end })
        }

        def sample(portName: String, value: BigInt, coverageDB: CoverageDB): Unit = {
            //Multi-range bins only work in the case of cross coverage,
            //for simple bins their are considered as one large bin
            if(conditionOpt.isEmpty) ranges match {
                case r => if (r.forall(_.contains(value))) coverageDB.addBinHit(portName, name, value)
            }
            //Multi-range bins are ignored in the case of conditional coverage
            else if (condition(List(value))) ranges match {
                case r if r.isEmpty => coverageDB.addBinHit(portName, name, value)
                case r => if (r.head.contains(value)) coverageDB.addBinHit(portName, name, value)
            }
        }

        def range: Range = ranges.head

        def serialize: String = s"Bin( $name, $ranges ${if (conditionOpt.isDefined) condition})"
    }

    /**
      * Shorthand to simplify the Bin's API
      */
    def bin(name: String, range: Option[Range] = None, condition: Option[Seq[BigInt] => Boolean] = None, expectedHits: BigInt = 0): Bin =
            new Bin(name, if(range.isDefined) Seq(range.get) else Seq.empty, condition, if (expectedHits == 0) None else Some(expectedHits))

    def cross(name: String, ranges: Seq[Range], expectedHits: BigInt = 0): CrossBin =
        CrossBin(name, if(expectedHits == 0) None else Some(expectedHits))(ranges)

    implicit def condToOption(cond: Seq[BigInt] => Boolean): Option[Seq[BigInt] => Boolean] = Some(cond)
    implicit def rangeToSeqRangeOpt(r: Range): Option[Seq[Range]] = Some(Seq(r))
    implicit def binsToCrossBins(bins: Seq[Bin]): Seq[CrossBin] = bins.map(b => CrossBin(b.name, b.expectedHits)(b.ranges))
    /**
      * Implicit type conversion from range to option to simplify syntax
      * @param r the range that will be converted
      * @return an option containing the given range
      */
    implicit def rangeToOption(r: Range): Option[Range] = Some(r)

    /**
      * A range relation between two different ranges
      *
      * @param cname   the name of the relation, used for the report
      * @param cranges the ranges that will be sampled for each point of the relation
      */
    private[chiselverify] case class CrossBin(cname: String, expectedH: Option[BigInt] = None)(cranges: Seq[Range])
        extends Bin(cname, cranges, expectedHits = expectedH) {
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
        def apply(port: Data): Bin = bin("default", defaultRange(port))

        /**
          * Generates a default bin for given cross point ports
          *
          * @param port1 the first point of the cross point
          * @param port2 the second point of the cross point
          * @return a cross bin covering all possible value combinations for the given ports
          */
        def apply(port1: Data, port2: Data): CrossBin = CrossBin("defaultCross")(Seq(defaultRange(port1), defaultRange(port2)))
    }
}
