// Module: dut
// This module is a wrapper around the design found in aluaccu.v to facilitate UVM testing.
module dut_vhd ( dut_if dif);

    //The below workaround is necessary, as Modelsim doesn't know how to map the leros_op_t into a std_logic_vector
    logic [2:0] op_logic;
    assign op_logic = dif.op;
	
	accualu alu (.clock(dif.clock), .reset(dif.reset), .op(op_logic), .din(dif.din), .enable(dif.ena), .accu(dif.accu));
	
endmodule