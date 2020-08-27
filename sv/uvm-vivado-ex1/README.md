# Vivado UVM Example

This example was taken from [Vivado Design Suite Tutorial - Logic Simulation](https://www.xilinx.com/support/documentation/sw_manuals/xilinx2020_1/ug937-vivado-design-suite-simulation-tutorial.pdf)

## Run the tests from the command line
To run the tests from the command line in Linux:
```sh
cd run/
sh run_xsim.csh
```


```
[................]
UVM_INFO ../example_uvm.srcs/sim_1/imports/verif/env/ref_model/adder_4_bit_ref_model.sv(53) @ 40045000: uvm_test_top.env.ref_model [uvm_test_top.env.ref_model] EXPECTED TRANSACTION FROM REF MODEL
-----------------------------------------------------
Name          Type                     Size  Value   
-----------------------------------------------------
req           adder_4_bit_transaction  -     @48871  
  x           integral                 4     'h9     
  y           integral                 4     'h1     
  cin         integral                 1     'h0     
  sum         integral                 4     'h0     
  cout        integral                 1     'h0     
  carry_out   integral                 3     'h0     
  begin_time  time                     64    40025000
-----------------------------------------------------
UVM_INFO ../example_uvm.srcs/sim_1/imports/verif/env/top/adder_4_bit_scoreboard.sv(71) @ 40045000: uvm_test_top.env.sb [uvm_test_top.env.sb] expected adder_4_bit SUM =10 , actual adder_4_bit SUM =10 
UVM_INFO ../example_uvm.srcs/sim_1/imports/verif/env/top/adder_4_bit_scoreboard.sv(72) @ 40045000: uvm_test_top.env.sb [uvm_test_top.env.sb] expected adder_4_bit cout =0 , actual adder_4_bit cout =0 
UVM_INFO ../example_uvm.srcs/sim_1/imports/verif/env/top/adder_4_bit_scoreboard.sv(74) @ 40045000: uvm_test_top.env.sb [uvm_test_top.env.sb] SUM MATCHES
UVM_INFO ../example_uvm.srcs/sim_1/imports/verif/env/top/adder_4_bit_scoreboard.sv(80) @ 40045000: uvm_test_top.env.sb [uvm_test_top.env.sb] COUT MATCHES
UVM_INFO /proj/xbuilds/SWIP/2020.1_0223_2001/installs/lin64/Vivado/2020.1/data/system_verilog/uvm_1.2/xlnx_uvm_package.sv(19968) @ 40045000: reporter [TEST_DONE] 'run' phase is ready to proceed to the 'extract' phase
-------------------------------------------------
------ INFO : TEST CASE PASSED ------------------
-----------------------------------------
UVM_INFO /proj/xbuilds/SWIP/2020.1_0223_2001/installs/lin64/Vivado/2020.1/data/system_verilog/uvm_1.2/xlnx_uvm_package.sv(13673) @ 40045000: reporter [UVM/REPORT/SERVER] [uvm_test_top.env.sb]  8000
[uvm_test_top.env.ref_model]  2000
[uvm_test_top.env.adder_4_bit_agent.sequencer.seq]  2000
[uvm_test_top.env.adder_4_bit_agent.monitor]  2000
[uvm_test_top.env.adder_4_bit_agent.driver]  2000
[UVM/RELNOTES]     1
[UVM/COMP/NAMECHECK]     1
[TEST_DONE]     1
[RNTST]     1
[NO_DPI_TSTNAME]     1
** Report counts by id
UVM_FATAL :    0
UVM_ERROR :    0
UVM_WARNING :    0
UVM_INFO :16005
** Report counts by severity

--- UVM Report Summary ---

$finish called at time : 40045 ns : File "/opt/Vivado/Vivado/2020.1/data/system_verilog/uvm_1.2/xlnx_uvm_package.sv" Line 18699
exit
INFO: [Common 17-206] Exiting xsim at Tue Aug 25 17:23:50 2020...
```
## Run the tests in Vivado
In Vivado the tests can be run by running the behavioral simulation
