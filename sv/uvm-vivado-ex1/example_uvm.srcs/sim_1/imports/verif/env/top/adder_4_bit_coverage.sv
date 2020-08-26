`ifndef ADDER_4_BIT_COVERAGE
`define ADDER_4_BIT_COVERAGE

class adder_4_bit_coverage#(type T=adder_4_bit_transaction) extends uvm_subscriber#(T);

///////////////////////////////////////////////////////////////////////////////
// Declaration of Local fields
///////////////////////////////////////////////////////////////////////////////
adder_4_bit_transaction cov_trans;
`uvm_component_utils(adder_4_bit_coverage)
///////////////////////////////////////////////////////////////////////////////
// functional coverage: covergroup for adder_4_bit
///////////////////////////////////////////////////////////////////////////////
covergroup adder_4_bit_cg;
   option.per_instance=1;
   option.goal=100;

  adder_4_bit_x: coverpoint cov_trans.x {
                   bins x_values[] = {[0:$]};
                 }
  
  adder_4_bit_y: coverpoint cov_trans.y {
                   bins y_values[] = {[0:$]};
                 }

  adder_4_bit_cin : coverpoint cov_trans.cin {
                   bins cin_1 = {1};
                   bins cin_0  = {0};
                 }

  adder_4_bit_sum : coverpoint cov_trans.sum {
                   bins sum_values[] = {[0:$]};
                 }

  adder_4_bit_cout    : coverpoint cov_trans.cout { 
                   bins low  = {0};
                   bins high = {1};
                 }

endgroup
//////////////////////////////////////////////////////////////////////////////
//constructor
//////////////////////////////////////////////////////////////////////////////
function new(string name="adder_4_bit_ref_model", uvm_component parent);
 super.new(name,parent);
 adder_4_bit_cg =new();
 cov_trans =new();
endfunction
///////////////////////////////////////////////////////////////////////////////
// Method name : sample
// Description : sampling adder_4_bit coverage
///////////////////////////////////////////////////////////////////////////////
function void write(T t);
  this.cov_trans = t;
  adder_4_bit_cg.sample();
endfunction

endclass

`endif



