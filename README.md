# Verification with Chisel and UVM

This repo is for the project to explore the combination and interaction of Chisel
and UVM. The ultimate goal is a verification framework within Scala for digital
hardware described in Chisel also supporting legacy components in VHDL, Verilog,
or SystemVerilog.


# UVM Examples
In the [sv](sv) directory, a number of UVM examples are located. 

## Simple examples
 In `sv/uvm-simple-examples` a number of simple examples are located. These start with a very basic testbench with no DUT attached, and gradually transition into a complete testbench.

## Vivado UVM Examples
These examples assume that a copy of Xilinx Vivado is installed and present in the PATH. The examples are currently tested only on Linux.
* The first example is taken from [Vivado Design Suite Tutorial - Logic Simulation](https://www.xilinx.com/support/documentation/sw_manuals/xilinx2020_1/ug937-vivado-design-suite-simulation-tutorial.pdf)


# Leros ALU
In the directory `sv/leros`, the [Leros ALU](src/main/scala/leros/AluAccuChisel.scala) is tested using UVM, to showcase that Chisel and UVM can work together. This testbench is reused to also test a VHDL implementation of the ALU, to show that UVM is usable on mixed-language designs (when using a mixed-language simulator).

The VHDL implementaion is run by setting the makefile argument `TOP=top_vhd`.

# Example Use Cases

We will explore a handful of use cases to explore verification.

 * Leros ALU (basically done)
 * Heap preiority queue (from MicroSemi),
   see also https://www.hackerearth.com/practice/notes/heaps-and-priority-queues/
 * Network-on-chip (in Chisel), see https://github.com/schoeberl/soc-comm
 * Decimation filter from WSA (VHDL code plus testbench given)

# Meeting Notes

## Friday 2020-9-11

Discussion at our Friday meeting

 * Overall goals: workshop with industry, paper, DFF funding application
 * OCP like interface + AXI wrapper + test sequencer
 * Look what "standard" interfaces and driver are available form industry, e.g., Synopsis
 * MicroSemi example (Tjark)
 * Transaction level modeling (Enrico)
 * Bus function models (AMBA, Wishbone, ...) (Kartik)
 * Constraint random generation in Scala/Java, library available? (Enrico)
 * Coverage
   * in Treadle (Andrew)
   * Verilator coverage
   * Test plan with features (available in SV as covergroup)
 * How to use ScalaTest? (Hans)
 * Direct programming interface (DPI) and JVM based languages (Java, Scala) (Kasper)
   * Scala/Java model within SV
   * C model in Scala (DPI in Chisel/Scala)
 * We shall have a workshop end of October

We have on outsider interested to contribute: Kartik Samtani

# Documents

Collect pointers to relevant documents.

## General Verification Documents

## OVM Documents

## Directly Related Work

 * *Layering RTL, SAFL, Handel-C and BluespecConstructs on Chisel HCL*, David J Greaves,
 see http://koo.corpus.cam.ac.uk/drafts/tndjg-008-transactional-modelling-in-chisel.html

 

