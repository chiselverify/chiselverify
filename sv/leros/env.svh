//  Class: env
//
class env extends uvm_env;
	`uvm_component_utils(env);

	//  Group: Configuration Object(s)


	//  Group: Components
	driver m_driv;
	my_sequencer m_seqr;

	//  Group: Variables


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
	//  Function: end_of_elaboration_phase
	extern function void end_of_elaboration_phase(uvm_phase phase);

	/*---  UVM Run Phases              ---*/
	/*------------------------------------*/
	//  Function: start_of_simulation_phase
	extern function void start_of_simulation_phase(uvm_phase phase);
	//  Function: reset_phase
	extern task reset_phase(uvm_phase phase);
	//  Function: configure_phase
	extern task configure_phase(uvm_phase phase);
	//  Function: main_phase
	extern task main_phase(uvm_phase phase);
	//  Function: shutdown_phase
	extern task shutdown_phase(uvm_phase phase);

	/*---  UVM Cleanup Phases          ---*/
	/*------------------------------------*/
	//  Function: extract_phase
	extern function void extract_phase(uvm_phase phase);
	//  Function: report_phase
	extern function void report_phase(uvm_phase phase);
	
endclass: env


/*----------------------------------------------------------------------------*/
/*  UVM Build Phases                                                          */
/*----------------------------------------------------------------------------*/
function void env::build_phase(uvm_phase phase);
	/*  note: Do not call super.build_phase() from any class that is extended from an UVM base class!  */
	/*  For more information see UVM Cookbook v1800.2 p.503  */
	// super.build_phase(phase);

	m_driv = driver::type_id::create(.name("m_driv"), .parent(this));
	m_seqr = my_sequencer::type_id::create(.name("m_seqr"), .parent(this));
	
endfunction: build_phase


function void env::connect_phase(uvm_phase phase);
	super.connect_phase(phase);
	// m_driv.seq_item_port.connect(m_seqr.seq_item_export);
	m_driv.seq_item_port.connect( m_seqr.seq_item_export );
endfunction: connect_phase

function void env::end_of_elaboration_phase(uvm_phase phase);
	super.end_of_elaboration_phase(phase);
endfunction: end_of_elaboration_phase


/*----------------------------------------------------------------------------*/
/*  UVM Run Phases                                                            */
/*----------------------------------------------------------------------------*/
function void env::start_of_simulation_phase(uvm_phase phase);
	super.start_of_simulation_phase(phase);
endfunction: start_of_simulation_phase


task env::reset_phase(uvm_phase phase);
endtask: reset_phase


task env::configure_phase(uvm_phase phase);
endtask: configure_phase


task env::main_phase(uvm_phase phase);
endtask: main_phase


task env::shutdown_phase(uvm_phase phase);
endtask: shutdown_phase


/*----------------------------------------------------------------------------*/
/*  UVM Cleanup Phases                                                        */
/*----------------------------------------------------------------------------*/
function void env::report_phase(uvm_phase phase);
	super.report_phase(phase);
endfunction: report_phase


function void env::extract_phase(uvm_phase phase);
	super.extract_phase(phase);
endfunction: extract_phase

