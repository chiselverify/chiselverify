// See README.md for license details.

package coverage

import chisel3.Data

object Coverage {
    case class CoverGroup(points: List[CoverPoint], id: BigInt)
    case class CoverPoint(port: Data, portName: String, bins: List[Bins])
    case class Bins(name: String, range: Range)
}
