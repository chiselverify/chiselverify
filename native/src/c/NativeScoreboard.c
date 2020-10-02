#include "NativeScoreboard.h"
#include <ctype.h>

enum leros_op {
	NOP, ADD, SUB, AND, OR, XOR, LD, SHR
};
typedef enum leros_op leros_op_t;

/**
* @brief Implements the scoreboard checking function in C using the JNI
* 
* @param din The input data to the ALU
* @param op The opcode used for the ALU
* @param reset Whether reset was asserted (1) or not (0)
* @return The calculated output value
*/ 
JNIEXPORT jint JNICALL Java_NativeScoreboard_calc
  (JNIEnv* env, jobject obj, jint din, jint op, jint reset) {
	static int accu = 0;

	if(reset) {
		accu = 0;
	} else {
		switch (op) {
		case ADD:
			accu += din;
			break;
		case SUB:
			accu -= din;
			break;
		case AND:
			accu = accu & din;
			break;
		case OR:
			accu = accu | din;
			break;
		case XOR:
			accu = accu ^ din;
			break;
		case LD:
			accu = din;
			break;
		case SHR:
			//C performs arithmetic right shift (platform dependent), but we want a logical shift
			//We have to perform a mask with 7fff_ffff to ensure MSB is 0
			accu = (accu >> 1) & 0x7fffffff;
			break;
		// case NOP:
		default:
			break;
		}
	}
	return accu;
}