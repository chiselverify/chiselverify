//  Class: runall_sequence
//  Notice: Not deriver from base_sequence
class runall_sequence extends uvm_sequence #(base_transaction);
	`uvm_object_utils(runall_sequence);

	//  Group: Variables
	base_sequence base_seq;
	addonly_sequence add_seq;
	subonly_sequence sub_seq;

	//  Constructor: new
	function new(string name = "runall_sequence");
		super.new(name);
		base_seq = base_sequence::type_id::create("base_seq");
		add_seq  = addonly_sequence::type_id::create("add_seq");
		sub_seq  = subonly_sequence::type_id::create("sub_seq");
	endfunction: new

	virtual task pre_body();
		if(starting_phase != null)
			starting_phase.raise_objection(this);
	endtask

	// Sequencer 'm_sequencer' is defined in uvm_sequence, and is a handle to whichever
	// sequencer is running these child sequences. This way, we can send them up the hierarchy
	virtual task body();
		base_seq.start(m_sequencer);
		add_seq.start(m_sequencer);
		sub_seq.start(m_sequencer);
	endtask

	virtual task post_body();
		if(starting_phase != null)
			starting_phase.drop_objection(this);
	endtask
	
endclass: runall_sequence