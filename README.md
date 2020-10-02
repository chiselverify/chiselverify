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


## Leros ALU
In the directory `sv/leros`, the [Leros ALU](src/main/scala/leros/AluAccuChisel.scala) is tested using UVM, to showcase that Chisel and UVM can work together. This testbench is reused to also test a VHDL implementation of the ALU, to show that UVM is usable on mixed-language designs (when using a mixed-language simulator).

The VHDL implementaion is run by setting the makefile argument `TOP=top_vhd`.

# Using the SV DPI and Javas JNI
Using the SystemVerilog DPI (Direct Programming Interface) to cosimulate with a golden model described in C is explored in the [scoreboard_dpi.svh file](sv/leros/scoreboards/scoreboard_dpi.svh). The C-model is implemented in `scoreboard.c`, and the checking functionality is called from the SystemVerilog code.

Implementing a similar functionality in Scala/Chisel has been explored via the JNI (Java Native Interface). In the directory `native`, the necessary code for a simple Leros tester using the JNI is implemented. 

To use the JNI functionality, first run `make jni` to generate the correct header files and shared libraries. Then, open sbt and type `project native` to access the native project. Then run `sbt test` to test the Leros ALU using a C model called from within Scala. To switch back, type `project chisel-uvm`. 
# Example Use Cases

We will explore a handful of use cases to explore verification.

 * Leros ALU (basically done)
 * Heap priority queue (from MicroSemi),
   see also https://www.hackerearth.com/practice/notes/heaps-and-priority-queues/
 * Network-on-chip (in Chisel), see https://github.com/schoeberl/soc-comm
 * Decimation filter from WSA (VHDL code plus testbench given)

# Resources
If yu're interested in learning more about the UVM, we recommend that you explore the repository, as well as some of the following links:
* [First steps with UVM](https://www.youtube.com/watch?v=qLr8ayWM_Ww)
* [UVM Cookbook (requires an account)](https://verificationacademy.com/cookbook/uvm)
* [ChipVerify.com UVM Tutorials](https://www.chipverify.com/table/uvm/)
* [Ray Salemi's UVM Primer videos](https://www.youtube.com/watch?v=eeU2zpgXv1A&list=PLigQ6Cc3qFpI_WTgqtDXi_Msk3yRuKGGJ)


# Meeting Notes

## Friday 2020-09-11

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

## Friday 2020-09-26

 * Code review of the priority sorting (Tjark)
 * Agreed that we will use two repos now: one for the example, one for the framework code
 * Kasper presented SV/C and Scala/C integration

# Documents

Collect pointers to relevant documents.

## General Verification Documents

## OVM Documents

## Directly Related Work

- *Layering RTL, SAFL, Handel-C and BluespecConstructs on Chisel HCL*, David J Greaves,
 see http://koo.corpus.cam.ac.uk/drafts/tndjg-008-transactional-modelling-in-chisel.html

### CRV
- [Choco-Solver](https://github.com/chocoteam/choco-solver) Java library for solving CSP problems

### Testing Framewrok / Simulation tools
#### Cocotb -- coroutine based cosimulation python library library for hardware development in Python
[Cocotb repository](https://github.com/cocotb/cocotb): cocotb is a coroutine based cosimulation library for writing VHDL and Verilog testbenches in Python.

##### Resources Related to Cocotb
  - Philipp Wagner (FOSSi Foundation, lowRISC) ["Cocotb: Python-powered hardware verification"](https://www.youtube.com/watch?v=GUcKJ5zXgPA) (WOSH 2019) (video)
  - Ben Rosser (University of Pennsylvania) ["Cocotb: a Python-based digital logic verification framework"](https://indico.cern.ch/event/776422/attachments/1769690/2874927/cocotb_talk.pdf) (CERN 2018) (pdf)
  - Torbjørn Viem Ness (NTNU) ["Low Power Floating-Point Unit for RISC-V"](https://brage.bibsys.no/xmlui/bitstream/handle/11250/2564801/19493_FULLTEXT.pdf?sequence=1&isAllowed=y) (2018) [PDF]
  - Andrey Filippov (Elphel) ["I will not have to learn SystemVerilog"](https://www.elphel.com/blog/2016/07/i-will-not-have-to-learn-systemverilog/) (2016) [Blog]
  - Chris Higgs (Potential Ventures) "Applying agile techniques to FPGA development" [Video](https://www.youtube.com/watch?v=Wi_1bUgHl8s), [Paper](http://www.testandverification.com/conferences/verification-futures/2015-europe/speaker-chris-higgs-potential-ventures/)
  - Chris Higgs (Potential Ventures) ["Rapid FPGA Verification"](https://docs.google.com/presentation/d/1U22Y_yyQRAecXXvOKFbHU8p_pGsB-u7iaK57Tw92U_A/embed?start=false&loop=false&delayms=3000) (NMI, February 2014) [Slides]
  - Smith, Andrew Michael; Mayo, Jackson; Armstrong, Robert C.; Schiek, Richard; Sholander, Peter E.; Mei, Ting (Sandia National Lab): ["Digital/Analog Cosimulation using CocoTB and Xyce"](https://www.osti.gov/biblio/1488489) (paper)

#### Extension of Cocotb
- [cocotb-coverage](https://github.com/mciepluc/cocotb-coverage): Extension that enables coverage and constrained random verification
  - Publication in iEEE [Paper](https://ieeexplore.ieee.org/document/7566600)
- [python-uvm](https://github.com/tpoikela/uvm-python): port of SystemVerilog (SV) Universal Verification Methodology (UVM) 1.2 to Python and cocotb
### Hwt --  Python library for hardware development
[hwt](https://github.com/Nic30/hwt):  one of the golas of this library is to implement some simulation feature similar to UVM



## Not strictly relevant resources
- [CRAVE: An advanced constrained random verification environment for SystemC](https://ieeexplore.ieee.org/document/6376356)
- [EnrichingUVM in SystemC with AMS extensions for randomization and functional coverage](https://www.researchgate.net/publication/267437715_Enriching_UVM_in_SystemC_with_AMS_extensions_for_randomization_and_coverage)
- [Coverage  directed  test  generation  for  functional  verification  using  Bayesian  network](https://www.research.ibm.com/haifa/dept/_svt/papers/simulation/18_2.pdf)
- [Introducing XCS to Coverage Directed test Generation](https://ieeexplore.ieee.org/document/6114166)

- [LiveHD](https://github.com/masc-ucsc/livehd) LiveHD is an infrastructure designed for Live Hardware Development. By live, we mean that small changes in the design should have the synthesis and simulation results in a few seconds, as the fast interactive systems usually response in sub-second.
