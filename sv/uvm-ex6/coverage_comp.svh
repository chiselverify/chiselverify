//Creating a coverage component which takes objects of type 'command' and processes them
//By extending uvm_subscriber, we automatically gain an analysis_export
class coverage_comp extends uvm_subscriber #(command);
    `uvm_component_utils(coverage_comp)
    command cmd = new;

    //Set up coverage and covergroups
    //We want to try all operations with all zeros and all ones and random data
    covergroup zeros_and_ones;
        as: coverpoint cmd.a {
            bins zeros = {'0};
            bins ones = {'1};
            bins others = {[8'h1:8'hfe]};
        }
        bs: coverpoint cmd.b {
            bins zeros = {'0};
            bins ones = {'1};
            bins others = {[8'h1:8'hfe]};
        }

        ops: coverpoint cmd.op;

        cross as, bs, ops; 
    endgroup

    function new(string name, uvm_component parent);
        super.new(name,parent);
        zeros_and_ones = new; //We have to instantiate the covergroup
    endfunction

    function void build_phase(uvm_phase phase);
        super.build_phase(phase);
    endfunction

    //Gets called every time the analysis port we are subscribing to calls its write() method.
    function void write(command t);
        cmd.a = t.a;
        cmd.b = t.b;
        cmd.op = t.op;
        zeros_and_ones.sample();
    endfunction
endclass