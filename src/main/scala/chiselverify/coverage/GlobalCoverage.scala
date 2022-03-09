package chiselverify.coverage

import chisel3.experimental.DataMirror
import chisel3._
import chiseltest._

import scala.collection.mutable

object GlobalCoverage {
    /**
      * Maintains the coverage data sampled from each port throughout the test suite
      */
    class QueryableCoverageDB {
        //Contains all of the sampled values for a given port
        private val portToValueCycleMap: mutable.ArrayBuffer[(String, (BigInt, BigInt))] =
            new mutable.ArrayBuffer[(String, (BigInt, BigInt))]()
        //Contains the registers range for a given port
        private val portToRangeMap: mutable.HashMap[String, Range] = new mutable.HashMap[String, Range]()
        //Stores the names of all ports
        private val portNameToNameMap: mutable.HashMap[String, String] = new mutable.HashMap[String, String]()

        //Caches
        private val nHitsCache: mutable.HashMap[(String, Int, Int, Int), BigInt] =
            new mutable.HashMap[(String, Int, Int, Int), BigInt]()
        //(id, expectedHits) -> Coverage
        private val coverageCache: mutable.HashMap[(String, Int), Double] = new mutable.HashMap[(String, Int), Double]()

        /**
          * Retrieves the registered name of a port given it's firrtl name
          * @param portName the firrtl name of the port (port.toNamed.name)
          * @return the registered name of said port
          */
        def name(portName: String) : String = portNameToNameMap.get(portName) match {
            case None => throw new IllegalArgumentException(s"$portName is not registered!")
            case Some(n) => n
        }

        /**
          * Getter for the sampled (value, cycle) mappings
          *
          * @param id the unique id of the port that was sampled
          * @return the (value, cycle) pairs that were sampled for the given port
          */
        def get(id: String): List[(BigInt, BigInt)] = portToValueCycleMap.groupBy(_._1).find(_._1 == id) match {
            case None => throw new IllegalArgumentException(s"Invalid id: $id")
            case Some(res) => res._2.map(elem => elem._2).toList
        }

        /**
          * Getter for the number of hits a given port has
          *
          * @param id the unique id of the port that was sampled
          * @return the number of hits that were sampled for a given port
          */
        def getHits(id: String, range: Option[Range] = None): BigInt = {
            //Contains all values that resulted in a hit
            val portToHitsMap: mutable.HashMap[String, List[BigInt]] = new mutable.HashMap[String, List[BigInt]]()

            //Get range
            val r = range.getOrElse(portToRangeMap.get(id) match {
                case None => throw new IllegalArgumentException(s"$id must be registered before use!")
                case Some(ran) => ran
            })

            nHitsCache.get((id, r.start, r.end, r.step)) match {
                case None =>
                    //Compute hits
                    val value = get(id)
                    val hits = value.foldLeft(BigInt(0)) {
                        case (acc, (v, _)) => acc + (
                            if (r contains v) {
                                portToHitsMap.get(id) match {
                                    case None =>
                                        portToHitsMap.update(id, List(v))
                                        1
                                    case Some(lv) =>
                                        if (lv contains v) 0
                                        else {
                                            portToHitsMap.update(id, lv :+ v)
                                            1
                                        }
                                }
                            } else 0
                        )
                    }
                    //Cache Hits
                    nHitsCache.update((id, r.start, r.end, r.step), hits)
                    hits
                case Some(res: BigInt) => res
            }
        }

        /**
          * Getter for the amount of coverage that was obtained for a given port
          *
          * @param id the unique id of the port that was sampled
          * @return the coverage percentage that the given port has obtained
          */
        def getCoverage(id: String, hits: BigInt, range: Option[Range] = None, expected: Option[Int] = None): Double = {
            val rangeSize = range.getOrElse(portToRangeMap.get(id) match {
                case None => throw new IllegalArgumentException(s"$id is not registered")
                case Some(r) => r
            }).size

            coverageCache.get(id, expected.getOrElse(rangeSize)) match {
                case None =>
                    val total = expected.getOrElse(rangeSize)
                    //Sanity check
                    if(hits > rangeSize) throw new IllegalStateException(
                        s"The number of hits ($hits), shouldn't be higher than the range size ($rangeSize)."
                    )
                    else {
                        //Compute coverage
                        val coverage = (hits.toDouble / total.toDouble) * 100

                        //Cache coverage
                        coverageCache.update((id, total), coverage)
                        coverage
                    }
                case Some(res: Double) => res
            }
        }

        /**
          * Updates the values sampled for a given port
          *
          * @param id    the unique id of the port that is being sampled
          * @param value the value that was sampled for the given port
          */
        def set(id: String, value: (BigInt, BigInt)): Unit = portToValueCycleMap.+=((id, value))

        /**
          * Registers a port in the database by associating a range to it
          * @param port the port that is being sampled
          * @param range the range that the port covers
          */
        def register(port: (String, Data), range: Range): Unit = {
            //Register name
            portNameToNameMap.update(port._2.toNamed.name, port._1)

            //Register range
            portToRangeMap.get(port._1) match {
                case None => portToRangeMap.update(port._1, range)
                case Some(_) => throw new IllegalArgumentException(s"${port._1} is already registered!")
            }
        }
    }

    /**
      * Case class containing all of the information pertaining to the functional coverage of a given port.
      * @param name the name of the port
      * @param valueCycles the list of (value, cycles) mappings that lead to a hit
      * @param hits the number of hits obtained
      * @param coverage the amount of coverage obtained
      * @param range the custom range over which the port was sampled
      */
    case class CoverageResult(
         name: String,
         valueCycles: List[(BigInt, BigInt)],
         hits: BigInt,
         coverage: Double,
         range: Option[Range] = None
     ) {
        val report : String = s"Port $name${
            if(range.isDefined) s" for ${range.get.toString()}" else ""
        } has $hits hits${
            val cov = (coverage * 100).toInt / 100.0
            if(cov == 0.0) "."
            else s" = $cov% coverage."
        }"
        def print(): Unit = println(report)
    }

    /**
      * Defines a global verfication plan that covers all ports in a DUT.
      * @param dut the dut being covered
      * @tparam T the type of said dut
      */
    class QueryableCoverage[T <: Module](val dut: T) {
        val db = new QueryableCoverageDB
        val ports = (DataMirror.fullModulePorts(dut)).filter(p => p._1 != "clock" && p._1 != "reset" && p._1 != "io")
        private var cycles : Int = 0

        //Register all output ports
        ports.foreach(p => {
            db.register(p, DefaultBin.defaultRange(p._2))
        })

        /**
          * Samples every port in the DUT.
          */
        def sample(): Unit = ports.foreach(p => db.set(p._1, (p._2.peek().litValue, cycles)))

        /**
          * Steps the DUT clock while maintaining our internal one.
          * @param c the number of cycles by which we want to step
          */
        def step(c: Int): Unit = {
            dut.clock.step(c)
            cycles = cycles + c
        }

        /**
          * Getter that allows the user to query the coverage database.
          * @param port the port that the user wants to know about
          * @param expected [Optional] the expected number of hits
          * @param range [Optional] the range over which the port is sampled
          * @return CoverageResult containing all of the coverage data
          */
        def get(port: Data, expected: Int = -1, range: Option[Range] = None): CoverageResult = {
            val id = db.name(port.toNamed.name)
            val hits = db.getHits(id, range)
            CoverageResult(
                id,
                db.get(id),
                hits,
                db.getCoverage(id, hits, range, if(expected == -1) None else Some(expected)),
                range
            )
        }

        /**
          * Queries the database for all of the ports at once.
          * @return a sequence of CoverageResults (one for each port)
          */
        def getAll: Seq[CoverageResult] = ports.map(p => get(p._2))

        /**
          * Prints a full coverage report to the terminal.
          */
        def printAll(): Unit = {
            println("==================== COVERAGE REPORT ====================")
            getAll.foreach(_.print())
            println("=========================================================")
        }
    }
}