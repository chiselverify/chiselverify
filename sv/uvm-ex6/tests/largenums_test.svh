//  Class: largenums_test
//
class largenums_test extends base_test;
	`uvm_component_utils(largenums_test);

	//  Constructor: new
	function new(string name = "largenums_test", uvm_component parent);
		super.new(name, parent);

	endfunction: new

	function void start_of_simulation_phase(uvm_phase phase);
		//Performing a factory override to supply largenums_transaction instead
		//and increasing the number of runs
		base_transaction::type_id::set_type_override(largenums_transaction::get_type());
		g_cfg.no_runs = 30;
	endfunction: start_of_simulation_phase
	
endclass: largenums_test
