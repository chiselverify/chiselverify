//  Class: base_test
//
class base_test extends uvm_test;
	`uvm_component_utils(base_test);

	//  Group: config objects
	agent_config agent1_cfg = new;

	//  Group: Components
	env m_env;

	//  Group: Functions
	//  Function: build_phase
	extern function void build_phase(uvm_phase phase);
	
	//  Function: run_phase
	extern task run_phase(uvm_phase phase);
	
	//  Constructor: new
	function new(string name = "base_test", uvm_component parent);
		super.new(name, parent);
	endfunction: new

	
endclass: base_test

/*----------------------------------------------------------------------------*/
/*  UVM Build Phases                                                          */
/*----------------------------------------------------------------------------*/
function void base_test::build_phase(uvm_phase phase);
	m_env = env::type_id::create(.name("m_env"), .parent(this));

	agent1_cfg.is_active = UVM_ACTIVE;
	uvm_config_db#(agent_config)::set(this, "m_env.m_agent", "agent_cfg", agent1_cfg);
	
endfunction: build_phase

/*----------------------------------------------------------------------------*/
/*  UVM Run Phases                                                            */
/*----------------------------------------------------------------------------*/
task base_test::run_phase(uvm_phase phase);
	base_sequence seq;
	seq = base_sequence::type_id::create("seq");
	seq.starting_phase = phase;
	seq.start(m_env.m_agent.m_seqr);
endtask: run_phase

