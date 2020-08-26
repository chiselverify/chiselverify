# chisel-uvm

This repo is for the project to explore the combination and interaction of Chisel
and UVM.


# UVM Examples
In the [sv](sv) directory, a number of UVM examples are located.

* The first example uses a bare-bones testbench and an empty DUT to showcase the basic parts of a UVM testbench (the test and environment). The testbench is heavily commented to explain usage.
* The second example shows how to use a driver and interface to drive values onto the DUT. 
* The third example shows how to use sequences and sequencers to drive randomized transactions onto the DUT.
* The fourth example uses a monitor and a `uvm_subscriber` to create a coverage report, alongside a scoreboard that checks the output
* The fifth example replicates the functionality of #4, but each class has been put into its own file.
* The sixth example includes more sequences and tests that leverage these sequences. Each sequence derives from the basic sequence.

## Vivado UVM Examples
These examples assume that a copy of Xilinx Vivado is installed and present in the PATH. The examples are currently tested only on Linux.
* The first example is taken from [Vivado Design Suite Tutorial - Logic Simulation](https://www.xilinx.com/support/documentation/sw_manuals/xilinx2020_1/ug937-vivado-design-suite-simulation-tutorial.pdf)


In the directory `leros`, the ALU desribed in `src/main/scala/leros/AluAccu.scala` is tested using UVM, to showcase that Chisel and UVM can work together.
