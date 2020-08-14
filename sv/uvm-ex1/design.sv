interface dut_if;
	//Intentionally left blank
endinterface


module dut(dut_if dif);
	//Intentionally left blank.
	initial begin
		#0 $display("Hello, world");
	end
endmodule
