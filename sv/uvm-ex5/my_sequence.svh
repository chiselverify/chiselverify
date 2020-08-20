//The sequencer is generated using a typedef of a UVM sequencer parameterized to use my_transaction uvm_sequence_item's
//It is also possible to defined my_sequencer as a class that extends uvm_sequencer, but that is not necessary for this example
typedef uvm_sequencer #(my_transaction) my_sequencer;

//The sequence defines the transactions of type my_transaction that will be generated.
//These are all randomly generated, but they may be customized based on the current state of the DUT or other parameters
class my_sequence extends uvm_sequence #(my_transaction);

	 `uvm_object_utils(my_sequence)
	global_config g_cfg;
    
    function new (string name = "");
        super.new(name);
    endfunction

    //All uvm_sequence implementations must have a 'task body'
	 task body;
		if (!uvm_config_db#(global_config)::get(null, "", "g_cfg", g_cfg))
			`uvm_error(get_name(), "Unable to get g_cfg")
					

        //starting_phase is a variable defined in uvm_sequence. 
        //If the sequence instantiates other child sequences, these children will *not* raise an objection
        if (starting_phase != null)
            starting_phase.raise_objection(this);

        //Here, the sequence generates the transactions of type my_transaction, randomizes them and passes them onto the driver
        repeat(g_cfg.no_runs)
        begin
            req = my_transaction::type_id::create("req");
            start_item(req);
            if( !req.randomize() )
                `uvm_error("", "Randomize failed")
            finish_item(req);
        end
        
        if (starting_phase != null)
            starting_phase.drop_objection(this);
    endtask: body

endclass: my_sequence