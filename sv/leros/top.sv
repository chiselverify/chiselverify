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
		uvm_config_db #(virtual dut_if)::set(null, "*", "dif_v", dif);
		uvm_top.finish_on_completion = 1;
		run_test("test1");
	end

	initial begin
		#1; //Hack to make sure we don't sample at time 0
		forever begin
			@(posedge dif.clock)
			#1
			`uvm_info("TOPREPORT", $sformatf("Op=%s, din=%d, rst=%d, res=%d", dif.op.name, dif.din, dif.reset, dif.accu), UVM_MEDIUM)
		end
	end


endmodule