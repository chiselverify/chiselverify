`ifndef ADDER_4_BIT_SEQUENCER
`define ADDER_4_BIT_SEQUENCER

class adder_4_bit_sequencer extends uvm_sequencer#(adder_4_bit_transaction);
 
  `uvm_component_utils(adder_4_bit_sequencer)
 
  ///////////////////////////////////////////////////////////////////////////////
  //constructor
  ///////////////////////////////////////////////////////////////////////////////
  function new(string name, uvm_component parent);
    super.new(name,parent);
  endfunction
   
endclass

`endif




