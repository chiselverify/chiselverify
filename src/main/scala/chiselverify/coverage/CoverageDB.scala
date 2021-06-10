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

package chiselverify.coverage

import chisel3._
import chiselverify.coverage.Utils._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Stores all of the data relevant to the functional coverage elements
  */
class CoverageDB {
    //Contains all a mapping from coverPoint id to the DUT's port name
    private val coverIdPortMap: mutable.HashMap[String, Data] = new mutable.HashMap[String, Data]()
    //Contains the current sampled values of the different bins
    private val binIdHitValuesMap: mutable.HashMap[(String, String), List[BigInt]] = new mutable.HashMap[(String, String), List[BigInt]]()
    //Contains a mapping for the bin ID pair (coverPointID, binID) to the current number of hits it has
    private val binIdNumHitsMap: mutable.HashMap[(String, String), BigInt] = new mutable.HashMap[(String, String), BigInt]()

    //CrossBins
    private val crossBinHitValuesMap: mutable.HashMap[CrossBin, List[(BigInt, BigInt)]] = new mutable.HashMap[CrossBin, List[(BigInt, BigInt)]]()
    private val crossBinNumHitsMap: mutable.HashMap[CrossBin, BigInt] = new mutable.HashMap[CrossBin, BigInt]()

    //Mappings for cross coverage
    private val pointNameToPoint: mutable.HashMap[String, Cover] = new mutable.HashMap[String, Cover]()
    private val crossToPoints: mutable.HashMap[Cross, (Cover, Cover)] = new  mutable.HashMap[Cross, (Cover, Cover)]()
    private val pointToCross: mutable.HashMap[Cover, Cross] = new mutable.HashMap[Cover, Cross]()

    //((port1name, port2name) -> delay) timed relation delays mapping
    private val timedCrossDelays: mutable.HashMap[(String, String), Int] = new mutable.HashMap[(String, String), Int]()

    //Mappings for conditional coverage that hit
    private val registeredConditions: mutable.ArrayBuffer[String] = new ArrayBuffer[String]()
    private val conditionalHits: mutable.HashMap[String, List[List[BigInt]]] = new mutable.HashMap[String, List[List[BigInt]]]()
    private val conditionSizes: mutable.HashMap[String, BigInt] = new mutable.HashMap[String, BigInt]()

    //(pointname, binname) -> List[(value, bin hit cycle)] mapping for timed cross coverage
    private val timedCrossBinHits: mutable.HashMap[(String, String), List[(BigInt, BigInt)]] = new mutable.HashMap[(String, String), List[(BigInt, BigInt)]]()

    //Keep track of the different valid IDs
    private val groupIds: ArrayBuffer[BigInt] = new ArrayBuffer[BigInt]()

    private var lastCoverGroupId: BigInt = 0

    private var curCycle : BigInt = 0

    /**
      * Adds a coverGroup to the list of valid coverGroups
      * @return the newly generated group id
      */
    def createCoverGroup(): BigInt = {
        lastCoverGroupId += 1
        groupIds append lastCoverGroupId
        lastCoverGroupId
    }

    def getCurCycle: BigInt = curCycle

    def step(cycles: Int = 1): Unit = curCycle += cycles

    def getTimedHits(pointName: String, binName: String): List[(BigInt, BigInt)] = timedCrossBinHits.getOrElse((pointName, binName), Nil)

    /**
      * Retrieves a coverpoint registered in the database
      * @param name the name of the point we want to retrieve
      * @return the coverpoint with the given name
      */
    def getPoint(name: String): Cover = pointNameToPoint get name match {
        case None => throw new IllegalArgumentException(s"$name is not a registered coverpoint!")
        case Some(p) => p
    }

    /**
      * Registers a cross relation in the database
      * @param cross the relation which we want to register
      */
    def registerCross(cross: Cross) : Unit = {
        val point1 = getPoint(cross.pointName1)
        val point2 = getPoint(cross.pointName2)

        //Register the cross
        crossToPoints update (cross, (point1, point2))
        pointToCross update (point1, cross)
        pointToCross update (point2, cross)
    }

    /**
      * Registers a new condition inside of the database
      * WARNING: O(pow(W,N)) with N = #ports and W = maxWidth
      * Complexity use with caution!
      *
      * @param condName the unique identifier for the condition
      * @throws IllegalArgumentException if the name isn't unique
      */
    def registerConditions(pointName: String, conds: List[Condition]): Unit = pointNameToPoint.get(pointName) match {
        case Some(p) => p match {
            case covCond: CoverCondition =>
                if(p.ports.length > 3) {
                    println("WARNING: MORE THAN 3 PORTS IN A COVER_CONDITION MAY LEAD TO EXTREME COMPLEXITY, USE WITH CAUTION")
                }
                val cartesianRange = cartesianProduct(covCond.ports.map(DefaultBin.defaultRange(_).toList))

                //Compute the size of the domain defined by each condition
                conds foreach {condition =>
                    if(registeredConditions.contains(condition.name)) {
                        registeredConditions += condition.name

                        val condSize = cartesianRange.count(r => condition(r.map(BigInt(_))))
                        conditionSizes.update(condition.name, condSize)

                    } else throw new IllegalArgumentException(
                        s"${condition.name} is already taken! Please use a unique ID for each Condition!"
                    )
                }
            case _ => throw new IllegalArgumentException("Requested point must be of type CoverCondition")
        }
        case None => throw new IllegalArgumentException(s"$pointName isn't a registered point!")
    }

    def getCondSize(condName: String): BigInt = conditionSizes.getOrElse(condName, 0)

    def getCrossFromPoint(point: Cover) : Cross = pointToCross getOrElse(point, null)
    def getPointsFromCross(cross: Cross) : (Cover, Cover) = crossToPoints getOrElse(cross, null)

    /**
      * Updates the number of hits done in a given bin
      */
    def addBinHit(pointName: String, binName: String, value: BigInt): Unit = {
        val newValues = (binIdHitValuesMap.getOrElse((pointName, binName), Nil) :+ value).distinct

        binIdHitValuesMap update ((pointName, binName), newValues)
        binIdNumHitsMap update ((pointName, binName), newValues.length)
    }

    /**
      * Updates the number of hits done in a given bin
      */
    def addCrossBinHit(crossBin: CrossBin, value: (BigInt, BigInt)): Unit = {
        val newValues = (crossBinHitValuesMap.getOrElse(crossBin, Nil) :+ value).distinct

        crossBinHitValuesMap update (crossBin, newValues)
        crossBinNumHitsMap update (crossBin, newValues.length)
    }

    /**
      * Keeps track of the cycles at which a bin hit occurred
      * @param pointName the name of the point we want to record a hit for
      * @param cycle the cycle at which a hit occurred
      */
    def addTimedBinHit(pointName: String, binName: String, value: BigInt, cycle: BigInt): Unit = {
        val newCycles = (timedCrossBinHits.getOrElse((pointName, binName), Nil) :+ (value, cycle)).distinct
        timedCrossBinHits.update((pointName, binName), newCycles)
    }

    /**
      * Keeps track of conditional hits that occur
      * @param pointName the name of the CoverCondition
      * @param condNames the name of the conditions that occured in a hit
      */
    def addConditionalHit(condNames: List[String], values: List[BigInt]): Unit = {
        condNames foreach { cond =>
            conditionalHits.update(cond, conditionalHits.getOrElse(cond, Nil) :+ values)
        }
    }

    /**
      * Registers a given coverpoint in the databas
      * @param name the name of the point we want to register (will be used as it's primary key)
      * @param coverPoint the point which we want to register
      */
    def registerCoverPoint(name: String, coverPoint: Cover) : Unit =
        if(pointNameToPoint contains name) throw new IllegalArgumentException("CoverPoint Name already taken!")
        else pointNameToPoint update (name, coverPoint)

    /**
      * Gets the number of hits form the DB for a given bin id
      * @return the number of hits for the given bin
      */
    def getNHits(pointName: String, binName: String): BigInt = binIdNumHitsMap getOrElse ((pointName, binName), 0)
    def getNHits(cross: CrossBin): BigInt = crossBinNumHitsMap getOrElse (cross, 0)
    def getNHits(conditionalName: String): Int = conditionalHits.getOrElse(conditionalName, Nil).size

    /**
      * Retrieves a port name given an id
      * @return a string containing the name of the port
      */
    def getPort(portName: String): Data = coverIdPortMap get portName match {
        case None => throw new IllegalArgumentException(s"$portName is not a registered port name!")
        case Some(port) => port
    }
}
