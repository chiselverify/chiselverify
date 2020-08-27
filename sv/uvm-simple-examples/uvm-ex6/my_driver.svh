

class my_driver extends uvm_driver #(base_transaction);
    `uvm_component_utils(my_driver)

    virtual alu_if alif_v;

    function new(string name, uvm_component parent);
        super.new(name, parent);
    endfunction
    
    function void build_phase(uvm_phase phase);
        // Get interface reference from config database
        if( !uvm_config_db #(virtual alu_if)::get(this, "", "alu_if", alif_v) )
            `uvm_error("", "uvm_config_db::get failed")
    endfunction 

    task run_phase(uvm_phase phase);
        forever
        begin
            //Here, the driver gets the next transaction on its seq_item_port (a buit-in port)
            seq_item_port.get_next_item(req);

            // Wiggle pins of DUT in correct manner
            @(negedge alif_v.clock);
            alif_v.a = req.a;
            alif_v.b = req.b;
            alif_v.op = req.op;
            alif_v.start = '1;

            @(negedge alif_v.clock);
            alif_v.start = '0;

            @(posedge alif_v.done);
            //The result is ready		
            //And signals to the sequencer that it is finished driving the transaction
            seq_item_port.item_done();
        end
    endtask

endclass: my_driver