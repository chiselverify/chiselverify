package chiselverify.coverage

import scala.collection.mutable

object GlobalCoverage {
    class QueryableCoverageDB {
        //Contains all of the sampled values for a given port
        private val portToValueCycleMap: mutable.HashMap[String, List[(BigInt, BigInt)]] =
            new mutable.HashMap[String, List[(BigInt, BigInt)]]()
        //Contains the registers range for a given port
        private val portToRangeMap: mutable.HashMap[String, Range] = new mutable.HashMap[String, Range]()
        //Contains all values that resulted in a hit
        private val portToHitsMap: mutable.HashMap[String, List[BigInt]] = new mutable.HashMap[String, List[BigInt]]()

        //Caches
        private val nHitsCache: mutable.HashMap[String, BigInt] = new mutable.HashMap[String, BigInt]()
        private val coverageCache: mutable.HashMap[String, Double] = new mutable.HashMap[String, Double]()

        /**
          * Getter for the sampled (value, cycle) mappings
          *
          * @param id the unique id of the port that was sampled
          * @return the (value, cycle) pairs that were sampled for the given port
          */
        def get(id: String): List[(BigInt, BigInt)] = portToValueCycleMap.get(id) match {
            case None => throw new IllegalArgumentException(s"Invalid id: $id")
            case Some(_) => _
        }

        /**
          * Getter for the number of hits a given port has
          *
          * @param id the unique id of the port that was sampled
          * @return the number of hits that were sampled for a given port
          */
        def getHits(id: String): BigInt = nHitsCache.get(id) match {
            case None =>
                //Compute hits
                val hits = portToValueCycleMap.get(id) match {
                    case None => throw new IllegalArgumentException(s"Can't get hits with the given ID: $id")
                    case Some(value) => portToRangeMap.get(id) match {
                        case None => throw new IllegalArgumentException(s"$id must be registered before use!")
                        case Some(r) => value.foldLeft(BigInt(0)) {
                            case (acc, (v, _)) => acc + (
                                if(r contains v) {
                                    portToHitsMap.get(id) match {
                                        case None =>
                                            portToHitsMap.update(id, List(v))
                                            1
                                        case Some(lv) =>
                                            if(lv contains v)
                                                0
                                            else {
                                                portToHitsMap.update(id, lv :+ v)
                                                1
                                            }
                                    }
                                }
                                else 0
                            )
                        }
                    }
                }
                //Cache Hits
                nHitsCache.update(id, hits)
                hits
            case Some(_) => _
        }

        /**
          * Getter for the amount of coverage that was obtained for a given port
          *
          * @param id the unique id of the port that was sampled
          * @return the coverage percentage that the given port has obtained
          */
        def getCoverage(id: String): Double = coverageCache.get(id) match {
            case None =>
                portToRangeMap.get(id) match {
                    case None => throw new IllegalArgumentException(s"$id is not registered")
                    case Some(r) =>
                        val hits = getHits(id)
                        //Sanity check
                        if(hits > r.size) throw new IllegalStateException(
                            s"The number of hits ($hits), shouldn't be higher than the range size (${r.size})."
                        )
                        else {
                            //Compute coverage
                            val coverage = (hits.toDouble / r.size.toDouble) * 100

                            //Cache coverage
                            coverageCache.update(id, coverage)
                            coverage
                        }
                }
            case Some(_) => _
        }

        /**
          * Updates the values sampled for a given port
          *
          * @param id    the unique id of the port that is being sampled
          * @param value the value that was sampled for the given port
          */
        def set(id: String, value: (BigInt, BigInt)): Unit = portToRangeMap.get(id) match {
            case None => throw new IllegalArgumentException(s"ID must be registered before use: $id")
            case Some(_) => portToValueCycleMap.get(id) match {
                case None => portToValueCycleMap.update(id, List(value))
                case Some(v) => portToValueCycleMap.update(id, v :+ value)
            }
        }

        /**
          * Registers a port in the database by associating a range to it
          * @param id the unique ID of the sampled port
          * @param range the range that the port covers
          */
        def register(id: String, range: Range): Unit = portToRangeMap.get(id) match {
            case None => portToRangeMap.update(id, range)
            case Some(_) => throw new IllegalArgumentException(s"$id is already registered!")
        }
    }
}