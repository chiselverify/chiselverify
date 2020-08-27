`ifndef ADDER_4_BIT_BASIC_TEST 
`define ADDER_4_BIT_BASIC_TEST

class adder_4_bit_basic_test extends uvm_test;
 
  ////////////////////////////////////////////////////////////////////
  //declaring component utils for the basic test-case 
  ////////////////////////////////////////////////////////////////////
  `uvm_component_utils(adder_4_bit_basic_test)
 
  adder_4_bit_environment     env;
  adder_4_bit_basic_seq       seq;
  ////////////////////////////////////////////////////////////////////
  // Method name : new
  // Decription: Constructor 
  ////////////////////////////////////////////////////////////////////
  function new(string name = "adder_4_bit_basic_test",uvm_component parent=null);
    super.new(name,parent);
  endfunction : new
  ////////////////////////////////////////////////////////////////////
  // Method name : build_phase 
  // Decription: Construct the components and objects 
  ////////////////////////////////////////////////////////////////////
  virtual function void build_phase(uvm_phase phase);
    super.build_phase(phase);
 
    env = adder_4_bit_environment::type_id::create("env", this);
    seq = adder_4_bit_basic_seq::type_id::create("seq");
  endfunction : build_phase
  ////////////////////////////////////////////////////////////////////
  // Method name : run_phase 
  // Decription: Trigger the sequences to run 
  ////////////////////////////////////////////////////////////////////
  task run_phase(uvm_phase phase);
    phase.raise_objection(this);
     seq.start(env.adder_4_bit_agnt.sequencer);
    phase.drop_objection(this);
  endtask : run_phase
 
endclass : adder_4_bit_basic_test

`endif












