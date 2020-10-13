package coverage

import chisel3._
import chiseltest._

class Cover {

  var elems = List[Data]()

  def register(port: Data): Unit = {
    elems = port :: elems
  }

  def sample(): Unit = {
    for (e <- elems) {
      println("I am sampling value: " + e.peek().litValue())
    }
  }
}
