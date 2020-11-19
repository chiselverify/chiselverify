//  Class: env
//
class env extends uvm_env;
	`uvm_component_utils(env);

	agent m_agent;
	coverage m_cov;
	scoreboard m_scoreboard;

	//  Constructor: new
	function new(string name = "env", uvm_component parent);
		super.new(name, parent);
	endfunction: new

	//  Function: build_phase
	extern function void build_phase(uvm_phase phase);
	//  Function: connect_phase
	extern function void connect_phase(uvm_phase phase);
	
endclass: env

function void env::build_phase(uvm_phase phase);

	m_agent = agent::type_id::create(.name("m_agent"), .parent(this));
	m_cov = coverage::type_id::create(.name("m_cov"), .parent(this));
	m_scoreboard = scoreboard::type_id::create(.name("m_scoreboard"), .parent(this));
	
endfunction: build_phase

function void env::connect_phase(uvm_phase phase);
	super.connect_phase(phase);
	//Agent AP to coverage
	m_agent.agent_ap.connect(m_cov.analysis_export);

	//Agent AP to scoreboard
	m_agent.agent_ap.connect(m_scoreboard.rslt_imp);

endfunction: connect_phase
