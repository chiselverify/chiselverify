//  Class: test1
//
class test1 extends uvm_test;
	`uvm_component_utils(test1);

	//  Group: Configuration Object(s)

	//  Group: Components
	env m_env;

	//  Group: Variables


	//  Group: Functions
	//  Function: build_phase
	extern function void build_phase(uvm_phase phase);
	
	//  Function: run_phase
	extern task run_phase(uvm_phase phase);
	
	//  Constructor: new
	function new(string name = "test1", uvm_component parent);
		super.new(name, parent);
	endfunction: new

	
endclass: test1

function void test1::build_phase(uvm_phase phase);
	m_env = env::type_id::create(.name("m_env"), .parent(this));
endfunction: build_phase

task test1::run_phase(uvm_phase phase);
	random_sequence seq;
	seq = random_sequence::type_id::create("seq");
	seq.starting_phase = phase;
	seq.start(m_env.m_seqr);
endtask: run_phase

