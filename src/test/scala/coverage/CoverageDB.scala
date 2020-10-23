// See README.md for license details.

package coverage

import chisel3.Data
import coverage.Coverage.{Bins, CoverPoint, Cross, CrossBin}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
  * Stores all of the data relevant to the functional coverage elements
  */
class CoverageDB {
    //Contains all a mapping from coverPoint id to the DUT's port name
    private val coverIdPortMap: mutable.HashMap[String, Data] = new mutable.HashMap[String, Data]()
    //Contains the current sampled values of the different bins
    private val binIdHitValuesMap: mutable.HashMap[Bins, List[BigInt]] = new mutable.HashMap[Bins, List[BigInt]]()
    //Contains a mapping for the bin ID pair (coverPointID, binID) to the current number of hits it has
    private val binIdNumHitsMap: mutable.HashMap[Bins, BigInt] = new mutable.HashMap[Bins, BigInt]()

    //CrossBins
    private val crossBinHitValuesMap: mutable.HashMap[CrossBin, List[(BigInt, BigInt)]] = new mutable.HashMap[CrossBin, List[(BigInt, BigInt)]]()
    private val crossBinNumHitsMap: mutable.HashMap[CrossBin, BigInt] = new mutable.HashMap[CrossBin, BigInt]()

    //Mappings for cross coverage
    private val pointNameToPoint: mutable.HashMap[String, CoverPoint] = new mutable.HashMap[String, CoverPoint]()
    private val crossToPoints: mutable.HashMap[Cross, (CoverPoint, CoverPoint)] = new  mutable.HashMap[Cross, (CoverPoint, CoverPoint)]()
    private val pointToCross: mutable.HashMap[CoverPoint, Cross] = new mutable.HashMap[CoverPoint, Cross]()

    //Keep track of the different valid IDs
    private val groupIds: ArrayBuffer[BigInt] = new ArrayBuffer[BigInt]()
    private val pointIds: ArrayBuffer[BigInt] = new ArrayBuffer[BigInt]()

    private var lastCoverGroupId: BigInt = 0

    /**
      * Adds a coverGroup to the list of valid coverGroups
      * @return the newly generated group id
      */
    def createCoverGroup(): BigInt = {
        lastCoverGroupId += 1
        groupIds append lastCoverGroupId
        lastCoverGroupId
    }

    def registerCross(cross: Cross) : Unit = {
        val point1 = pointNameToPoint get cross.pointName1 match {
            case None => throw new IllegalArgumentException(s"${cross.pointName1} is not a registered coverpoint!")
            case Some(p) => p
        }
        val point2 = pointNameToPoint get cross.pointName2 match {
            case None => throw new IllegalArgumentException(s"${cross.pointName2} is not a registered coverpoint!")
            case Some(p) => p
        }

        //Register the cross
        crossToPoints update (cross, (point1, point2))
        pointToCross update (point1, cross)
        pointToCross update (point2, cross)
    }

    def getCrossFromPoint(point: CoverPoint) : Cross = pointToCross getOrElse(point, null)
    def getPointsFromCross(cross: Cross) : (CoverPoint, CoverPoint) = crossToPoints getOrElse(cross, null)

    /**
      * Updates the number of hits done in a given bin
      */
    def addBinHit(bin: Bins, value: BigInt): Unit = {
        val newValues = (binIdHitValuesMap.getOrElse(bin, Nil) :+ value).distinct

        binIdHitValuesMap update (bin, newValues)
        binIdNumHitsMap update (bin, newValues.length)
    }

    /**
      * Updates the number of hits done in a given bin
      */
    def addCrossBinHit(crossBin: CrossBin, value: (BigInt, BigInt)): Unit = {
        val newValues = (crossBinHitValuesMap.getOrElse(crossBin, Nil) :+ value).distinct

        crossBinHitValuesMap update (crossBin, newValues)
        crossBinNumHitsMap update (crossBin, newValues.length)
    }

    def registerCoverPoint(name: String, coverPoint: CoverPoint) : Unit =
        if(pointNameToPoint contains name) throw new IllegalArgumentException("CoverPoint Name already taken!")
        else pointNameToPoint update (name, coverPoint)

    /**
      * Gets the number of hits form the DB for a given bin id
      * @return the number of hits for the given bin
      */
    def getNHits(bin: Bins): BigInt = binIdNumHitsMap getOrElse (bin, 0)
    def getNHits(cross: CrossBin): BigInt = crossBinNumHitsMap getOrElse (cross, 0)

    /**
      * Retrieves a port name given an id
      * @return a string containing the name of the port
      */
    def getPort(portName: String): Data = coverIdPortMap get portName match {
        case None => throw new IllegalArgumentException(s"$portName is not a registered port name!")
        case Some(port) => port
    }
}
