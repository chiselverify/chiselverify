`include "uvm_macros.svh"

module top;

    import uvm_pkg::*;
    import simplealu_pkg::*;
    
    alu_if alif ();
    
    simplealu sa ( .alif );

    // Clock generator
    initial
    begin
        alif.clock = 0;
        forever #5 alif.clock = ~alif.clock;
    end

    initial
    begin
        uvm_config_db #(virtual alu_if)::set(null, "*", "alu_if", alif);
        uvm_top.finish_on_completion = 1;
        run_test("my_test");
    end

    always @(negedge alif.done) begin
        // `uvm_info("", $sformatf("a: %d, b: %d, op: %s, res: %d, time=%d", alif.a, alif.b, alif.op.name, alif.result, $time), UVM_MEDIUM);
    end

endmodule: top