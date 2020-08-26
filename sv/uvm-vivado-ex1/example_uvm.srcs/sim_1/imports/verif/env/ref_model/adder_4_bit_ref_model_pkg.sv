`ifndef ADDER_4_BIT_REF_MODEL_PKG
`define ADDER_4_BIT_REF_MODEL_PKG

package adder_4_bit_ref_model_pkg;

   import uvm_pkg::*;
   `include "uvm_macros.svh"

   //////////////////////////////////////////////////////////
   // importing packages : agent,ref model, register ...
   /////////////////////////////////////////////////////////
   import adder_4_bit_agent_pkg::*;

   //////////////////////////////////////////////////////////
   // include ref model files 
   /////////////////////////////////////////////////////////
  `include "adder_4_bit_ref_model.sv"

endpackage

`endif



