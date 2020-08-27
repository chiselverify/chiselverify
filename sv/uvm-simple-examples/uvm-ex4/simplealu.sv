
package simplealu_pkg;
	typedef enum logic [1:0] { ADD, SUB, XOR, MUL } op_t;

	class command;
		logic[7:0] a,b;
		op_t op;
	endclass;
endpackage

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

//A simple ALU which can perform 4 operations
module simplealu(alu_if alif);
	import simplealu_pkg::*;

	logic [15:0] res_int = '0;
	logic done_int = '0;

	logic [7:0] a_int, b_int;
	op_t op_int;
	
	//Input registers
	always_ff @(posedge alif.clock) begin
		if(alif.start) begin
			a_int <= alif.a;
			b_int <= alif.b;
			op_int <= alif.op;
			done_int <= '1;
		end else
			done_int <= '0;
	end

	//ALU Logic
	always_comb begin
		case (op_int)
			ADD: res_int = a_int + b_int;
			SUB: res_int = a_int - b_int;
			XOR: res_int = a_int ^ b_int;
			MUL: res_int = a_int * b_int;
			default: res_int = '0;
		endcase
	end

	//Output registers
	always_ff @(posedge alif.clock) begin
		if(alif.reset) begin
			alif.result <= '0;
			alif.done <= '0;
		end else begin
			alif.result <= res_int;
			alif.done = done_int;
		end
	end


endmodule: simplealu