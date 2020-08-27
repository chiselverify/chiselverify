`ifndef ADDER_4_BIT_AGENT 
`define ADDER_4_BIT_AGENT

class adder_4_bit_agent extends uvm_agent;
  ///////////////////////////////////////////////////////////////////////////////
  // Declaration of UVC components such as.. driver,monitor,sequencer..etc
  ///////////////////////////////////////////////////////////////////////////////
  adder_4_bit_driver    driver;
  adder_4_bit_sequencer sequencer;
  adder_4_bit_monitor   monitor;
  ///////////////////////////////////////////////////////////////////////////////
  // Declaration of component utils 
  ///////////////////////////////////////////////////////////////////////////////
  `uvm_component_utils(adder_4_bit_agent)
  ///////////////////////////////////////////////////////////////////////////////
  // Method name : new 
  // Description : constructor
  ///////////////////////////////////////////////////////////////////////////////
  function new (string name, uvm_component parent);
    super.new(name, parent);
  endfunction : new
  ///////////////////////////////////////////////////////////////////////////////
  // Method name : build-phase 
  // Description : construct the components such as.. driver,monitor,sequencer..etc
  ///////////////////////////////////////////////////////////////////////////////
  function void build_phase(uvm_phase phase);
    super.build_phase(phase);
    driver = adder_4_bit_driver::type_id::create("driver", this);
    sequencer = adder_4_bit_sequencer::type_id::create("sequencer", this);
    monitor = adder_4_bit_monitor::type_id::create("monitor", this);
  endfunction : build_phase
  ///////////////////////////////////////////////////////////////////////////////
  // Method name : connect_phase 
  // Description : connect tlm ports ande exports (ex: analysis port/exports) 
  ///////////////////////////////////////////////////////////////////////////////
  function void connect_phase(uvm_phase phase);
      driver.seq_item_port.connect(sequencer.seq_item_export);
  endfunction : connect_phase
 
endclass : adder_4_bit_agent

`endif
