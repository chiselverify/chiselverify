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
			int'(-1):=1,
			32'h80000000:=1, //Min value
			32'h7fffffff:=1  //Max value
		}; 
	}

	//c_op and c_reset are unchanged

	//  Constructor: new
	function new(string name = "edge_transaction");
		super.new(name);
	endfunction: new
	
endclass: edge_transaction





