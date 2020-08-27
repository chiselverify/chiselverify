`ifndef ADDER_4_BIT_SEQ_LIST 
`define ADDER_4_BIT_SEQ_LIST

package adder_4_bit_seq_list;

 import uvm_pkg::*;
 `include "uvm_macros.svh"

 import adder_4_bit_agent_pkg::*;
 import adder_4_bit_ref_model_pkg::*;
 import adder_4_bit_env_pkg::*;

 //////////////////////////////////////////////////////////////////////////////
 // including adder_4_bit test list
 //////////////////////////////////////////////////////////////////////////////

 `include "adder_4_bit_basic_seq.sv"

endpackage

`endif
