`ifndef ADDER_4_BIT_ENV
`define ADDER_4_BIT_ENV

class adder_4_bit_environment extends uvm_env;
 
  //////////////////////////////////////////////////////////////////////////////
  //Declaration components
  //////////////////////////////////////////////////////////////////////////////
  adder_4_bit_agent adder_4_bit_agnt;
  adder_4_bit_ref_model ref_model;
  adder_4_bit_coverage#(adder_4_bit_transaction) coverage;
  adder_4_bit_scoreboard  sb;
   
  //////////////////////////////////////////////////////////////////////////////
  //Declaration of component utils to register with factory
  //////////////////////////////////////////////////////////////////////////////
  `uvm_component_utils(adder_4_bit_environment)
     
  //////////////////////////////////////////////////////////////////////////////
  // Method name : new 
  // Description : constructor
  //////////////////////////////////////////////////////////////////////////////
  function new(string name, uvm_component parent);
    super.new(name, parent);
  endfunction : new
  //////////////////////////////////////////////////////////////////////////////
  // Method name : build_phase 
  // Description : constructor
  //////////////////////////////////////////////////////////////////////////////
  function void build_phase(uvm_phase phase);
    super.build_phase(phase);
    adder_4_bit_agnt = adder_4_bit_agent::type_id::create("adder_4_bit_agent", this);
    ref_model = adder_4_bit_ref_model::type_id::create("ref_model", this);
    coverage = adder_4_bit_coverage#(adder_4_bit_transaction)::type_id::create("coverage", this);
    sb = adder_4_bit_scoreboard::type_id::create("sb", this);
  endfunction : build_phase
  //////////////////////////////////////////////////////////////////////////////
  // Method name : build_phase 
  // Description : constructor
  //////////////////////////////////////////////////////////////////////////////
  function void connect_phase(uvm_phase phase);
    super.connect_phase(phase);
    adder_4_bit_agnt.driver.drv2rm_port.connect(ref_model.rm_export);
    adder_4_bit_agnt.monitor.mon2sb_port.connect(sb.mon2sb_export);
    ref_model.rm2sb_port.connect(coverage.analysis_export);
    ref_model.rm2sb_port.connect(sb.rm2sb_export);
  endfunction : connect_phase

endclass : adder_4_bit_environment

`endif




