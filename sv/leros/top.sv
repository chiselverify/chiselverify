module top;
	
	import uvm_pkg::*;
	import leros_pkg::*;
	`include "uvm_macros.svh"

	dut_if dif ();
	dut mydut (.dif);

	//Clock gen
	initial begin
		dif.clock = '1;
		forever #5 dif.clock = ~dif.clock;
	end

	initial begin
		//Store virtual interface in config DB
		uvm_config_db #(virtual dut_if)::set(null, "uvm_test_top*", "dif_v", dif);
		uvm_top.finish_on_completion = 1;		
		run_test();
	end
endmodule
