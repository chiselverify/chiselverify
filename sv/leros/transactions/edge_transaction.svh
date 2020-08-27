//  Class: edge_transaction
//  Generates transactions with a number of interesting values that might be edge cases
class edge_transaction extends base_transaction;
	typedef edge_transaction this_type_t;
	`uvm_object_utils(edge_transaction);

	//  Group: Constraints
	//We wish to generate a lot of all-zeros and all-ones transactions
	constraint c_din {
 		din dist {
			0:=1,
			1:=1,
			shortint'(-1):=1,
			shortint'(-32768):=1,
			32767:=1
			// [16'b1:16'hfffe]:/1
		}; 
	}

	//c_op and c_reset are unchanged

	//  Group: Functions

	//  Constructor: new
	function new(string name = "edge_transaction");
		super.new(name);
	endfunction: new
	
endclass: edge_transaction





