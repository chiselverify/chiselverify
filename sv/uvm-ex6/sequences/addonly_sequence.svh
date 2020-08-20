//  Class: addonly_sequence
//
class addonly_sequence extends base_sequence;
	`uvm_object_utils(addonly_sequence);

	//  Constructor: new
	function new(string name = "addonly_sequence");
		super.new(name);
	endfunction: new

	//mid_do runs after randomization, but before the uvm_sequence_item is sent to the driver
	function void mid_do(uvm_sequence_item this_item);
		base_transaction trans;
		$cast(trans, this_item);
		trans.op = ADD;
	endfunction
	
endclass: addonly_sequence
