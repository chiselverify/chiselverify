//  Class: edge_sequence
//  A sequence only generating transactions which might identify edge-cases
class edge_sequence extends base_sequence;
	`uvm_object_utils(edge_sequence);

	//  Group: Variables
	// hilo_transaction tx;

	//  Constructor: new
	function new(string name = "edge_sequence");
		super.new(name);
	endfunction: new

	//  Task: body
	extern virtual task body();

endclass: edge_sequence

//Should generate edge_transaction
task edge_sequence::body();
	// super.body(); //Execute reset

	repeat(10) begin
		tx = edge_transaction::type_id::create("edge");
		start_item(tx);
		if( !tx.randomize())
			`uvm_error(get_name(), "Randomize failed")
		
		finish_item(tx);
	end

endtask