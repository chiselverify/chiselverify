#include "svdpi.h"
#include <stdlib.h>

enum leros_op {
	NOP, ADD, SUB, AND, OR, XOR, LD, SHR
};
typedef enum leros_op leros_op_t;

/**
* @brief Implements the scoreboard checking function in C using the UVM Direct Programming Interface
* 
* @param din The input data to the ALU
* @param op The opcode uses for the ALU
* @param reset Whether reset was asserted (1) or not (0)
* @param fromDUT The result from the DUT
*/ 
int scoreboard_check(int din, int op, int reset, int fromDUT) {
	static int accu = 0;

	if(reset) {
		accu = 0;
	} else
	{
		switch (op)
		{
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

	if(accu != fromDUT) {
		return 0; //aka bad
	} else {
		return 1; //aka good
	}
}