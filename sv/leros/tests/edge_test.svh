//  Class: edge_test
//
class edge_test extends base_test;
	`uvm_component_utils(edge_test);

	//  Group: Variables
	random_sequence rand_seq;

	//  Group: Functions
	//  Function: start_of_simulation_phase
	extern function void start_of_simulation_phase(uvm_phase phase);

	//  Function: run_phase
	extern task run_phase(uvm_phase phase);
	
	//  Constructor: new
	function new(string name = "edge_test", uvm_component parent);
		super.new(name, parent);	
	endfunction: new

endclass: edge_test

task edge_test::run_phase(uvm_phase phase);
	phase.raise_objection(this);
	generate_reset(phase);

	// random_sequence rand_seq;
	rand_seq = random_sequence::type_id::create("rand_seq");
	rand_seq.starting_phase = phase;
	
	if( !rand_seq.randomize())
		`uvm_error(get_name(), "Unable to randomize rand_seq")
		
	rand_seq.start(m_env.m_agent.m_seqr);
	phase.drop_objection(this);

endtask: run_phase

function void edge_test::start_of_simulation_phase(uvm_phase phase);
	base_transaction::type_id::set_type_override(edge_transaction::get_type());
endfunction: start_of_simulation_phase