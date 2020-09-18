//  Class: base_test
//
class base_test extends uvm_test;
	`uvm_component_utils(base_test);

	//  Group: config objects
	agent_config agent1_cfg = new;

	//  Group: Components
	env m_env;

	//  Function: build_phase
	extern function void build_phase(uvm_phase phase);
	
	//  Function: run_phase
	extern task run_phase(uvm_phase phase);

	//  Function: generate_reset
	extern task generate_reset(uvm_phase phase);
	
	//  Constructor: new
	function new(string name = "base_test", uvm_component parent);
		super.new(name, parent);
	endfunction: new
	
endclass: base_test

/*----------------------------------------------------------------------------*/
/*  UVM Build Phases                                                          */
/*----------------------------------------------------------------------------*/
function void base_test::build_phase(uvm_phase phase);
	//Build env, store agent cfg
	m_env = env::type_id::create(.name("m_env"), .parent(this));

	agent1_cfg.is_active = UVM_ACTIVE;
	uvm_config_db#(agent_config)::set(this, "m_env.m_agent", "agent_cfg", agent1_cfg);
endfunction: build_phase

/*----------------------------------------------------------------------------*/
/*  UVM Run Phases                                                            */
/*----------------------------------------------------------------------------*/

//Just generate a reset, and be done with it
task base_test::run_phase(uvm_phase phase);
	generate_reset(phase);
endtask: run_phase

task base_test::generate_reset(uvm_phase phase);
	reset_sequence seq;
	seq = reset_sequence::type_id::create("seq");
	seq.starting_phase = phase;
	seq.start(m_env.m_agent.m_seqr);
endtask;

