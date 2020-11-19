# UVM Testbench for the Leros ALU
This is a UVM testbench for the Leros ALU. The original ALU is described in [this Scala file](../../src/main/scala/examples/leros/AluAccuChisel.scala). Two versions of the ALU are used. One is the generated Verilog version in `AluAccu.v`, the other is the VHDL version in `AccuAlu.vhd`. Both have the same functionality.

# Structure
The files are separated into the files located in the main directory, and files in subdirectories. Any class which has a factory override has been moved into a directory with all extending classes. 

# How to run
To run the tests, you must have either ModelSim or Synopsys VCS installed and in your path. A simple

```
make
```
will run the ModelSim version. To run with vcs, type `make vcs`.

## Arguments
A number of arguments are available for the Makefile. They are as follows:
* TESTNAME - The name of the UVM test to run. Legal values are: `base_test` (default), `random_test` and `edge_test`.
* VERBOSITY - A slightly easier way of changing the UVM verbosity level. All UVM_VERBOSITY levels are valid. Default is `UVM_MEDIUM`.
* PLUSARGS - Let's you pass additional UVM plusargs to the simulator. Default is no arguments. 
* UVM - The directory in which the UVM files can be found. Default is `$UVM_HOME`
* TOP - The name of the top-component which the simulator should compile and run. Legal values are `top` (default, the Verilog version) and `top_vhd` (the VHDL version).

## Overrides
Most of the factory overrides are explicitly declared in the tests which make use of the extending classes, eg. `edge_test` overrides `base_transaction` to `edge_transaction`. 
To override the scoreboard and use the DPI version instead, you should include `PLUSARGS="+uvm_set_type_override=scoreboard,scoreboard_dpi"` in your call. 

# Problems
As of now, I haven't figured out how to make VCS generate a random seed every time. Should hopefully come soon ...
