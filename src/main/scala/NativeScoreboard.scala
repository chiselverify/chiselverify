import ch.jodersky.jni.nativeLoader

@nativeLoader("chisel-uvm0")
class NativeScoreboard {
	/**
	 * @brief Calculates the expected output based on current state and input 
	 * @param din The input to the DUT
	 * @param op The opcode for the DUT
	 * @param reset Whether reset is asserted (1) or not (0)
	 * @return The expected output value of the DUT
	 */
	@native def calc(din: Int, op: Int, reset: Int): Int;
}