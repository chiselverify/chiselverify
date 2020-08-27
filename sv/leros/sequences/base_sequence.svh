//  Class: base_sequence
//
class base_sequence extends uvm_sequence #(base_transaction);
	`uvm_object_utils(base_sequence);

	//  Group: Variables
	base_transaction tx; //Transaction object that all extending classes can access

	//  Constructor: new
	function new(string name = "base_sequence");
		super.new(name);
	endfunction: new

	//  Task: pre_body
	extern virtual task pre_body();

	//  Task: body
	extern virtual task body();

	//  Task: post_body
	extern virtual task post_body();
	
endclass: base_sequence

/**
 Raises an objection if this is the top sequence running on the sequencer
 Does nothing if this is a child sequence
*/
task base_sequence::pre_body();
	if(starting_phase != null)
		starting_phase.raise_objection(this);
endtask: pre_body

task base_sequence::body();
	//Empty on purpose
endtask: body

/**
 Drops an objection if this is the top sequence running on the sequencer
 Does nothing if this is a child sequence
*/
task base_sequence::post_body();
	if(starting_phase != null)
		starting_phase.drop_objection(this);
endtask: post_body