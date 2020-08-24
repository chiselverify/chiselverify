//  Class: hilo_transaction
//
class hilo_transaction extends base_transaction;
	typedef hilo_transaction this_type_t;
	`uvm_object_utils(hilo_transaction);

	//  Group: Constraints
	//We wish to generate a lot of all-zeros and all-ones transactions
	constraint c_din {
 		din dist {
			'0:/1,
			'1:/1,
			[16'b1:16'hfffe]:/1
		}; 
	}

	//Unchanged
	constraint c_op {
		0 <= op; op <= 'b111; 
	}

	//Generate a reset approx 1/8 of the time.
	constraint c_reset {
		is_reset dist {
			0:=7,
			1:=1
		};
	}

	//  Group: Functions

	//  Constructor: new
	function new(string name = "hilo_transaction");
		super.new(name);
	endfunction: new
	
endclass: hilo_transaction





