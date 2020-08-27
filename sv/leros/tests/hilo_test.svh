//  Class: hilo_test
//
class hilo_test extends base_test;
	`uvm_component_utils(hilo_test);

	//  Group: Variables
	sequence_config random_seq_cfg;

	random_sequence rand_seq;
	hilo_sequence hilo_seq;


	//  Group: Functions
	//  Function: start_of_simulation_phase
	extern function void start_of_simulation_phase(uvm_phase phase);

	//  Function: build_phase
	extern function void build_phase(uvm_phase phase);

	//  Constructor: new
	function new(string name = "hilo_test", uvm_component parent);
		super.new(name, parent);

	endfunction: new

endclass: hilo_test

/*----------------------------------------------------------------------------*/
/*  UVM Build Phases                                                          */
/*----------------------------------------------------------------------------*/
function void hilo_test::build_phase(uvm_phase phase);
	/*  note: Do not call super.build_phase() from any class that is extended from an UVM base class!  */
	/*  For more information see UVM Cookbook v1800.2 p.503  */
	super.build_phase(phase);

	random_seq_cfg = new;
	random_seq_cfg.no_repeats = 20;
	uvm_config_db#(sequence_config)::set(this, "", "random_seq_cfg", random_seq_cfg);
		
endfunction: build_phase
/*----------------------------------------------------------------------------*/
/*  UVM Run Phases                                                            */
/*----------------------------------------------------------------------------*/

/**
Used to perform factory overrides on the relevant types
*/
function void hilo_test::start_of_simulation_phase(uvm_phase phase);
	base_sequence::type_id::set_type_override(random_sequence::get_type());
	base_transaction::type_id::set_type_override(hilo_transaction::get_type());
endfunction: start_of_simulation_phase

task hilo_test::run_phase(uvm_phase phase);
	super.run_phase(phase);

endtask: run_phase
