// See README.md for license details.

package coverage

import chisel3.Data

object Coverage {
    case class CoverGroup(id: BigInt, points: List[CoverPoint], crosses: List[Cross])
    case class CoverPoint(port: Data, portName: String, bins: List[Bins])
    case class Cross(name: String, pointName1: String, pointName2: String, bins: List[CrossBin])
    case class Bins(name: String, range: Range)
    case class CrossBin(name: String, range1: Range, range2: Range)
}
