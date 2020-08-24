//  Class: command_monitor
//
class command_monitor extends uvm_monitor;
	`uvm_component_utils(command_monitor);

	//  Group: Components
	uvm_analysis_port #(leros_command) cmd_mon_ap;

	//  Group: Variables
	virtual dut_if dif_v;

	//  Group: Functions
	//  Function: build_phase
	extern function void build_phase(uvm_phase phase);
	
	//  Function: run_phase
	extern task run_phase(uvm_phase phase);
	
	//  Constructor: new
	function new(string name = "command_monitor", uvm_component parent);
		super.new(name, parent);
	endfunction: new

	
endclass: command_monitor

/*----------------------------------------------------------------------------*/
/*  UVM Build Phases                                                          */
/*----------------------------------------------------------------------------*/
function void command_monitor::build_phase(uvm_phase phase);
	//Get virtual interface handle from config DB
	if( !uvm_config_db#(virtual dut_if)::get(this, "", "dif_v", dif_v) )
		`uvm_error(get_name(), "Unable to get dif_v from config DB")

	cmd_mon_ap = new("cmd_mon_ap", this);
		
endfunction: build_phase

/*----------------------------------------------------------------------------*/
/*  UVM Run Phases                                                            */
/*----------------------------------------------------------------------------*/
task command_monitor::run_phase(uvm_phase phase);
	leros_command cmd = new;
	forever begin
		@(posedge dif_v.ena, posedge dif_v.reset)
		`uvm_info(get_name(), $sformatf("Command monitor got: [op=%s, din=%d, rst=%d]", dif_v.op.name, dif_v.din, dif_v.reset), UVM_HIGH)
		cmd.op = dif_v.op;
		cmd.din = dif_v.din;
		cmd.reset = dif_v.reset;

		cmd_mon_ap.write(cmd);
	end
endtask: run_phase
