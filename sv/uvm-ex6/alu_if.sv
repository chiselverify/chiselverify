`ifndef __ALU_IF_SV
`define __ALU_IF_SV

interface alu_if;
	import simplealu_pkg::*;
	logic clock, reset;
	logic start;
	op_t op;
	logic [7:0] a, b;
	logic [15:0] result;
	logic done;

	//Initials make for nicer looking waveforms
	initial begin
		a = '0;
		b = '0;
		done = '0;
		op = ADD;
		reset = '0;
		start = '0;
		result = '0;
	end
endinterface //alu_if

`endif
