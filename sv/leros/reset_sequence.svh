//  Class: reset_sequence
//
class reset_sequence extends base_sequence;
	`uvm_object_utils(reset_sequence);

	//  Constructor: new
	function new(string name = "reset_sequence");
		super.new(name);
	endfunction: new

	//  Task: body
	extern virtual task body();
	
endclass: reset_sequence

task reset_sequence::body();
	tx = base_transaction::type_id::create("base_transaction");
	start_item(tx);
	tx.op = NOP;
	tx.din = '0;
	tx.is_reset = 1;
	finish_item(tx);
endtask: body
