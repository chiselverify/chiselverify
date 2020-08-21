`ifndef __DUT_IF_SV
`define __DUT_IF_SV

interface dut_if;
	import leros_pkg::leros_op_t;
	logic clock, reset;
	logic ena;
	logic [15:0] din;
	leros_op_t op;
	logic [15:0] accu;
endinterface //dut_if

`endif
