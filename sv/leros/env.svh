//  Class: env
//
class env extends uvm_env;
	`uvm_component_utils(env);

	//  Group: Components
	driver m_driv;
	my_sequencer m_seqr;
	command_monitor m_cmd_mon;
	result_monitor m_rslt_mon;
	coverage m_cov;
	scoreboard m_scoreboard;

	//  Group: Functions

	//  Constructor: new
	function new(string name = "env", uvm_component parent);
		super.new(name, parent);
	endfunction: new

	/*---  UVM Build Phases            ---*/
	/*------------------------------------*/
	//  Function: build_phase
	extern function void build_phase(uvm_phase phase);
	//  Function: connect_phase
	extern function void connect_phase(uvm_phase phase);
	
endclass: env


/*----------------------------------------------------------------------------*/
/*  UVM Build Phases                                                          */
/*----------------------------------------------------------------------------*/
function void env::build_phase(uvm_phase phase);
	m_driv = driver::type_id::create(.name("m_driv"), .parent(this));
	m_seqr = my_sequencer::type_id::create(.name("m_seqr"), .parent(this));
	m_cmd_mon = command_monitor::type_id::create(.name("m_cmd_mon"), .parent(this));
	m_rslt_mon = result_monitor::type_id::create(.name("m_rslt_mon"), .parent(this));
	m_cov = coverage::type_id::create(.name("m_cov"), .parent(this));
	m_scoreboard = scoreboard::type_id::create(.name("m_scoreboard"), .parent(this));
	
endfunction: build_phase

function void env::connect_phase(uvm_phase phase);
	super.connect_phase(phase);
	//Driver and sequencer
	m_driv.seq_item_port.connect( m_seqr.seq_item_export );

	//Command monitor to coverage
	m_cmd_mon.cmd_mon_ap.connect(m_cov.analysis_export);

	//Scoreboard with cmd monitor and result monitor
	m_cmd_mon.cmd_mon_ap.connect(m_scoreboard.cmd_fifo.analysis_export);
	m_rslt_mon.rslt_mon_ap.connect(m_scoreboard.rslt_imp);
endfunction: connect_phase
