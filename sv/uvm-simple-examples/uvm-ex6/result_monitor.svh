class result_monitor extends uvm_monitor;

    `uvm_component_utils(result_monitor);

    virtual alu_if alif_v;

    //Create analysis port that transmits shortints (16 bits)
    uvm_analysis_port #(shortint) rslt_mon_ap;

    function new(string name, uvm_component parent);
        super.new(name, parent);
    endfunction

    function void build_phase(uvm_phase phase);
        // Get interface reference from config database
        if( !uvm_config_db #(virtual alu_if)::get(this, "", "alu_if", alif_v) )
            `uvm_error("", "uvm_config_db::get failed in result_monitor")
    
        //Build the analysis port
        rslt_mon_ap = new("rslt_mon_ap", this);
    endfunction

    virtual task run_phase(uvm_phase phase);
        shortint unsigned res;

        forever begin
            //On every rising edge of done, we have a valid result
            @(posedge alif_v.done);
            #1 //Wait for the value to be available
            res = alif_v.result;

            rslt_mon_ap.write(res);
        end
    endtask
endclass