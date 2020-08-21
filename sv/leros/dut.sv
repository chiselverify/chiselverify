// Module: dut
// This module is a wrapper around the design found in AluAccu.v to facilitate UVM testing.
module dut ( dut_if dif);
	
	AluAccu alu (.clock(dif.clock), .reset(dif.reset), .io_op(dif.op), .io_din(dif.din), .io_ena(dif.ena), .io_accu(dif.accu));
	
endmodule