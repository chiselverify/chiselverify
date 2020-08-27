//  Class: largenums_transaction
//
class largenums_transaction extends base_transaction;
	typedef largenums_transaction this_type_t;
	`uvm_object_utils(largenums_transaction);

	//  Group: Constraints
	constraint c_ab {
		127 < a; a < 256;
		127 < b; b < 256;
	}

	//  Constructor: new
	function new(string name = "largenums_transaction");
		super.new(name);
	endfunction: new

endclass: largenums_transaction

