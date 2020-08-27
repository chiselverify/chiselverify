//  Class: runall_test
//
class runall_test extends base_test;
	`uvm_component_utils(runall_test);

	//  Constructor: new
	function new(string name = "runall_test", uvm_component parent);
		super.new(name, parent);
	endfunction: new

	// Cannot perform a factory override of base_sequence here, 
	// as runall_sequence does not inherit from base_sequence
	task run_phase(uvm_phase phase);
		runall_sequence seq;
		seq = runall_sequence::type_id::create("seq");
		seq.starting_phase = phase;
		seq.start (m_env.m_seqr);
	endtask

	
endclass: runall_test
