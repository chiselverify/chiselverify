//  Class: random_sequence
//
class random_sequence extends base_sequence;
	`uvm_object_utils(random_sequence);

	//  Group: Variables
	sequence_config seq_cfg;

	int no_repeats = 10; //Magic number

	//  Group: Functions

	//  Constructor: new
	function new(string name = "random_sequence");
		super.new(name);
	endfunction: new

	//  Task: body
	extern virtual task body();

endclass: random_sequence

task random_sequence::body();
	super.body(); //Execute reset

	//Get config for sequence
	if (!uvm_config_db#(sequence_config)::get(null, "uvm_test_top", "random_seq_cfg", seq_cfg) )
		`uvm_error(get_name(), "Unable to get random_seq config")
	else
		no_repeats = seq_cfg.no_repeats;

	//Perform the sequence
	repeat(no_repeats) begin
		tx = base_transaction::type_id::create("tx");
		start_item(tx);
		if (!tx.randomize() )
			`uvm_error(get_name(), "Randomize failed")
			
		finish_item(tx);
		
	end
endtask: body
