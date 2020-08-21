//  Class: ldadd_sequence
//
class ldadd_sequence extends base_sequence;
	`uvm_object_utils(ldadd_sequence);

	//  Group: Variables
	base_transaction tx_ld;
	base_transaction tx_add;
	reset_sequence res;

	//  Constructor: new
	function new(string name = "ldadd_sequence");
		super.new(name);
	endfunction: new


	//  Task: body
	//  This is the user-defined task where the main sequence code resides.
	extern virtual task body();

endclass: ldadd_sequence

task ldadd_sequence::body();
	super.body();

	tx_ld = base_transaction::type_id::create("tx_ld");
	tx_add = base_transaction::type_id::create("tx_add");
	
	start_item(tx_ld);
	if (!tx_ld.randomize() )
		`uvm_error(get_name(), "Randomize failed")
	tx_ld.op = LD;
	tx_ld.is_reset = 0;
	finish_item(tx_ld);

	start_item(tx_add);
	if (!tx_add.randomize() with {op == ADD; } )
		`uvm_error(get_name(), "Randomize failed")
	// tx_add.op = ADD;
	finish_item(tx_add);

endtask: body
