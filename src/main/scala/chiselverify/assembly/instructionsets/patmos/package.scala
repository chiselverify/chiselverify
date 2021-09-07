package chiselverify.assembly.instructionsets

import chiselverify.assembly._

package object patmos {

  abstract class PatmosInstruction extends Instruction {
    val bundleSize = ConstantField(1)
    val mnemonic: String
  }



}
