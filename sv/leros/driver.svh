//  Class: driver
//
class driver extends uvm_driver #(base_transaction);
	`uvm_component_utils(driver);

	//  Group: Configuration Object(s)


	//  Group: Components


	//  Group: Variables
	virtual dut_if dif_v;

	//  Group: Functions

	//  Function: build_phase
	extern function void build_phase(uvm_phase phase);

	//  Function: run_phase
	extern task run_phase(uvm_phase phase);

	//  Function: drive_item
	//  Param tx a handle to the transaction item that should be driven
	extern task drive_item(base_transaction tx);

	//  Constructor: new
	function new(string name = "driver", uvm_component parent);
		super.new(name, parent);
	endfunction: new

	
endclass: driver

task driver::drive_item(base_transaction tx);
	@(negedge dif_v.clock)

	dif_v.din = tx.din;
	dif_v.op = tx.op;
	dif_v.reset = tx.is_reset;
	dif_v.ena = '1;

	@(posedge dif_v.clock)
	#2
	dif_v.ena = '0;
endtask: drive_item

function void driver::build_phase(uvm_phase phase);
	if (!uvm_config_db#(virtual dut_if)::get(this, "", "dif_v", dif_v) )
		`uvm_error(get_name(), "Unable to retrieve dif_v from config_db")
endfunction: build_phase

task driver::run_phase(uvm_phase phase);
	base_transaction tx;
	forever begin
		seq_item_port.get_next_item(tx);
		drive_item(tx);
		seq_item_port.item_done();
	end
endtask: run_phase





