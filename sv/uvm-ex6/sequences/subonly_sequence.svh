//  Class: subonly_sequence
//
class subonly_sequence extends base_sequence;
	`uvm_object_utils(subonly_sequence);

	//  Constructor: new
	function new(string name = "subonly_sequence");
		super.new(name);
	endfunction: new

	//mid_do runs after randomization, but before the uvm_sequence_item is sent to the driver
	function void mid_do(uvm_sequence_item this_item);
		base_transaction trans;
		$cast(trans, this_item);
		trans.op = SUB;
	endfunction
	
endclass: subonly_sequence
