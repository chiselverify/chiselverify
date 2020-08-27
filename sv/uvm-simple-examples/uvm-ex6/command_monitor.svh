//The command monitor hooks into the ALU interface. 
//Every time "start" is asserted, it samples the values of a, b and op
class command_monitor extends uvm_monitor;
    `uvm_component_utils(command_monitor);

    virtual alu_if alif_v;

    //Create analysis port that transmits "command" objects.
    uvm_analysis_port #(command) cmd_mon_ap;

    function new(string name, uvm_component parent);
        super.new(name, parent);
    endfunction

    function void build_phase(uvm_phase phase);
        // Get interface reference from config database
        if( !uvm_config_db #(virtual alu_if)::get(this, "", "alu_if", alif_v) )
            `uvm_error("", "uvm_config_db::get failed in command_monitor")
    
        //Build the analysis port
        cmd_mon_ap = new("cmd_mon_ap", this);
    endfunction

    virtual task run_phase(uvm_phase phase);
        command cmd = new;
        forever begin
            //On every rising edge of start, we assume that command is valid
            @(posedge alif_v.start);
            cmd.a = alif_v.a;
            cmd.b = alif_v.b;
            cmd.op = alif_v.op;

            cmd_mon_ap.write(cmd);

        end
    endtask

endclass;