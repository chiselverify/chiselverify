//  Class: base_sequence
//
class base_sequence extends uvm_sequence #(base_transaction);
	`uvm_object_utils(base_sequence);

	//  Group: Variables
	base_transaction tx;


	//  Group: Constraints


	//  Group: Functions

	//  Constructor: new
	function new(string name = "base_sequence");
		super.new(name);
	endfunction: new

	//  Task: pre_body
	//  This task is a user-definable callback that is called before the execution 
	//  of <body> ~only~ when the sequence is started with <start>.
	//  If <start> is called with ~call_pre_post~ set to 0, ~pre_body~ is not called.
	extern virtual task pre_body();

	//  Task: body
	//  This is the user-defined task where the main sequence code resides.
	extern virtual task body();

	//  Task: post_body
	//  This task is a user-definable callback task that is called after the execution 
	//  of <body> ~only~ when the sequence is started with <start>.
	//  If <start> is called with ~call_pre_post~ set to 0, ~post_body~ is not called.
	extern virtual task post_body();
	
endclass: base_sequence

task base_sequence::pre_body();
	if(starting_phase != null)
		starting_phase.raise_objection(this);
endtask: pre_body

task base_sequence::body();
	//We always wish to start by executing a reset (unless expressly told not to)
	tx = base_transaction::type_id::create("tx");
	start_item(tx);
	tx.is_reset = 1;
	finish_item(tx);
endtask: body


task base_sequence::post_body();
	if(starting_phase != null)
		starting_phase.drop_objection(this);
endtask: post_body

