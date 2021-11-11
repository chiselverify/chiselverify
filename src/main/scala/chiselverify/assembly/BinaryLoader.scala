package chiselverify.assembly

import java.nio.file.{Files, Paths}

class ProgramBinaries(n: String, bin: Array[Int], len: Int) {
  val name = n
  val byteBinaries = bin
  val wordBinaries = bin.map(BigInt(_)).sliding(4, 4).map(a => a(3) << 24 | a(2) << 16 | a(1) << 8 | a(0)).toArray
  val length = len
}

object BinaryLoader {
  def loadProgram(path: String): ProgramBinaries = {
    val (bin, length) = loadBin(path)
    val split = path.split(Array('/', '\\'))
    val name = split(split.length - 1)
    new ProgramBinaries(name.substring(0, name.length - 4), bin, length)
  }

  def loadBin(path: String): (Array[Int], Int) = {
    val bytes = Files.readAllBytes(Paths.get(path)).map(_ & 0xFF).sliding(4, 4).toArray
    if (bytes(bytes.length - 1).length < 4) bytes(bytes.length - 1) = Array(0, 0, 0, 0)
    (Array.concat(bytes.flatten, Array.fill(16)(0)), bytes.flatten.length / 4)
  }
}