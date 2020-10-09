package cover

import chisel3._
import chiseltest._

class Coverage {

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
