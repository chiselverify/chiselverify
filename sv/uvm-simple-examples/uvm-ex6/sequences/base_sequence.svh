
//  Class: base_sequence
//
class base_sequence extends uvm_sequence #(base_transaction);
	`uvm_object_utils(base_sequence);

	//  Group: Variables
	global_config g_cfg;

	//  Constructor: new
	function new(string name = "base_sequence");
		super.new(name);
	endfunction: new

	//Raise an objection if this is the sequence at the top of the hierarchy
	virtual task pre_body();
		if(starting_phase != null)
			starting_phase.raise_objection(this);
	endtask

	virtual task body();
		if (!uvm_config_db#(global_config)::get(null, "", "g_cfg", g_cfg))
			`uvm_error(get_name(), "Unable to get g_cfg")

		//Here, the sequence generates the transactions of type base_transaction, randomizes them and passes them onto the driver
		repeat(g_cfg.no_runs)
		begin
			 req = base_transaction::type_id::create("req");
			 start_item(req); //Signal sequencer (and thus driver) that the transaction is ready
			 if( !req.randomize() )
				  `uvm_error("", "Randomize failed")
			 finish_item(req); //Pass the transaction on to the driver
		end
	endtask

	//Drop the objection if this is the sequence at the top of the hierarchy
	virtual task post_body();
		if(starting_phase != null)
			starting_phase.drop_objection(this);
	endtask
	
endclass: base_sequence