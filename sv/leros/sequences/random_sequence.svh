//  Class: random_sequence
//
class random_sequence extends base_sequence;
	`uvm_object_utils(random_sequence);

	//  Group: Variables
	rand int num_repeats; //Magic number

	constraint c_repeats {
		num_repeats inside {[100:300]};
	}

	//  Group: Functions

	//  Constructor: new
	function new(string name = "random_sequence");
		super.new(name);
	endfunction: new

	//  Task: body
	extern virtual task body();

endclass: random_sequence

task random_sequence::body();
	//Perform the sequence
	repeat(num_repeats) begin
		tx = base_transaction::type_id::create("tx");
		start_item(tx);
		if (!tx.randomize() )
			`uvm_error(get_name(), "Randomize failed")

		finish_item(tx);
	end
endtask: body
