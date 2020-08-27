`ifndef ADDER_4_BIT_AGENT_PKG
`define ADDER_4_BIT_AGENT_PKG

package adder_4_bit_agent_pkg;
 
   import uvm_pkg::*;
   `include "uvm_macros.svh"

   //////////////////////////////////////////////////////////
   // include Agent components : driver,monitor,sequencer
   /////////////////////////////////////////////////////////
  `include "adder_4_bit_defines.svh"
  `include "adder_4_bit_transaction.sv"
  `include "adder_4_bit_sequencer.sv"
  `include "adder_4_bit_driver.sv"
  `include "adder_4_bit_monitor.sv"
  `include "adder_4_bit_agent.sv"

endpackage

`endif



