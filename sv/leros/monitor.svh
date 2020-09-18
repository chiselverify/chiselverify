//  Class: monitor
//
class monitor extends uvm_monitor;
    `uvm_component_utils(monitor);
    //  Group: Components
    uvm_analysis_port #(leros_command) mon_ap;

    //Virtual interface handle
    virtual dut_if dif_v;

    //  Function: build_phase
    extern function void build_phase(uvm_phase phase);
    
    //  Function: run_phase
    extern task run_phase(uvm_phase phase);

    //  Constructor: new
    function new(string name = "monitor", uvm_component parent);
        super.new(name, parent);
    endfunction: new
endclass: monitor

function void monitor::build_phase(uvm_phase phase);
    //Get virtual interface handle from DB
    if( !uvm_config_db#(virtual dut_if)::get(this, "", "dif_v", dif_v) )
        `uvm_error(get_name(), "Unable to get handle to virtual interface")
    
    mon_ap = new("mon_ap", this);
endfunction: build_phase

task monitor::run_phase(uvm_phase phase);
    leros_command cmd = new;
    forever begin
        @(posedge dif_v.ena, posedge dif_v.reset) //Sample operands
        cmd.op = dif_v.op;
        cmd.din = dif_v.din;
        cmd.reset = dif_v.reset;
        @(posedge dif_v.clock) //Sample result
        #1 //Wait for output to appear on register
        cmd.accu = dif_v.accu;
        mon_ap.write(cmd); //Write to listeners
    end
endtask: run_phase

