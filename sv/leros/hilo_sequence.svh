//  Class: hilo_sequence
//
class hilo_sequence extends base_sequence;
	`uvm_object_utils(hilo_sequence);

	//  Group: Variables
	// hilo_transaction tx;


	//  Group: Constraints


	//  Group: Functions

	//  Constructor: new
	function new(string name = "hilo_sequence");
		super.new(name);
	endfunction: new

	//  Task: body
	extern virtual task body();

endclass: hilo_sequence

//Should generate hilo_transactions
task hilo_sequence::body();
	super.body(); //Execute reset

	repeat(10) begin
		tx = hilo_transaction::type_id::create("hilo");
		start_item(tx);
		if( !tx.randomize())
			`uvm_error(get_name(), "Randomize failed")
		
		finish_item(tx);
	end

endtask