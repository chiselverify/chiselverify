`ifndef ADDER_4_BIT_INTERFACE
`define ADDER_4_BIT_INTERFACE

interface adder_4_bit_interface(input logic clk,reset);
  
  ////////////////////////////////////////////////////////////////////////////
  // Declaration of Signals
  ////////////////////////////////////////////////////////////////////////////
  logic [3:0] x,y;
  logic cin;
  logic [3:0] sum;
  logic cout;
  ////////////////////////////////////////////////////////////////////////////
  // clocking block and modport declaration for driver 
  ////////////////////////////////////////////////////////////////////////////
  clocking dr_cb@(posedge clk) ;
    output x; 
    output y;
    output cin;
    input  sum;
    input  cout;
  endclocking
  
  modport DRV (clocking dr_cb,input clk,reset) ;

  ////////////////////////////////////////////////////////////////////////////
  // clocking block and modport declaration for monitor 
  ////////////////////////////////////////////////////////////////////////////
  clocking rc_cb@(negedge clk) ;
    input x; 
    input y;
    input cin;
    input sum;
    input cout;
  endclocking
  
  modport RCV (clocking rc_cb,input clk,reset);

endinterface

`endif
