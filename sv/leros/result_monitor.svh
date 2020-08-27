//  Class: result_monitor
//
class result_monitor extends uvm_monitor;
	`uvm_component_utils(result_monitor);

	//  Group: Components
	uvm_analysis_port #(shortint) rslt_mon_ap;

	//  Group: Variables
	virtual dut_if dif_v;

	//  Group: Functions

	//  Function: build_phase
	extern function void build_phase(uvm_phase phase);
	
	//  Function: run_phase
	extern task run_phase(uvm_phase phase);
	
	//  Constructor: new
	function new(string name = "result_monitor", uvm_component parent);
		super.new(name, parent);
	endfunction: new


endclass: result_monitor

function void result_monitor::build_phase(uvm_phase phase);
	/*  note: Do not call super.build_phase() from any class that is extended from an UVM base class!  */
	/*  For more information see UVM Cookbook v1800.2 p.503  */
	//super.build_phase(phase);

	if( !uvm_config_db#(virtual dut_if)::get(this, "", "dif_v", dif_v))
		`uvm_error(get_name(), "Unable to get interface in config DB")
	
	rslt_mon_ap = new("rslt_mon_ap", this);
endfunction: build_phase

task result_monitor::run_phase(uvm_phase phase);
	shortint res;
	forever begin
		@(posedge dif_v.ena, posedge dif_v.reset) //Wait for command or reset to be asserted
		@(posedge dif_v.clock) //Wait for clock to tick
		#1 //Wait until we can sample
		res = dif_v.accu;
		`uvm_info(get_name(), $sformatf("Result monitor got %d", res), UVM_HIGH)
		
		rslt_mon_ap.write(res);
	end
endtask: run_phase
