# ChiselVerify: A Hardware Verification Library for Chisel

In this repository, we proprose ChiselVerify, which is the begining of a verification library within Scala for digital hardware described in Chisel, but also upporting legacy components in VHDL, Verilog, or SystemVerilog. The library runs off of [ChiselTest](https://github.com/ucb-bar/chisel-testers2) for all of the DUT interfacing. 

A technical report describes the library in detail: [Open-Source Verification with Chisel and Scala](https://arxiv.org/abs/2102.13460)

When you use this library in a research project, please cite it as:

```
@misc{dobis2021opensource,
      title={Open-Source Verification with Chisel and Scala}, 
      author={Andrew Dobis and Tjark Petersen and Kasper Juul Hesse Rasmussen and Enrico Tolotto and Hans Jakob Damsgaard and Simon Thye Andersen and Richard Lin and Martin Schoeberl},
      year={2021},
      eprint={2102.13460},
      archivePrefix={arXiv},
      primaryClass={cs.PL}
}
```

ChiselVerify is published on Maven. To use it, add following line to your
```build.sbt```:

```
libraryDependencies += "io.github.chiselverify" % "chiselverify" % "0.1"
```

Run tests with
```
make
```  
This README contains a brief overview of the library and its functionalities. For a more in-depth tutorial, please check-out the [ChiselVerify Wiki](https://github.com/chiselverify/chiselverify/wiki).

**********************************

# Verification Library for Chisel
The library can be divided into 3 main parts:  
1. __Functional Coverage__: Enabling Functional Coverage features like Cover Points, Cross Coverage, Timed Coverage and Conditional Coverage.  
2. __Constrained Random Verification__: Allowing for constraints and random variables to be defined and used directly in Scala.  
3. __Bus Functional Models__: Enabling Transactional modeling for standardized Buses like _AXI4_. 

## Functional Coverage in Chisel
The idea is to implement functional coverage features directly in Chisel.   
The structure of the system can be seen in the diagram below.

![Structure of the Coverage system](CoverageChisel.png)  
  
#### Coverage Reporter
This is the heart of the system. It handles everything from registering the Cover Points to managing the Coverage DataBase. It will also generate the final coverage report. Registering _Cover Points_ together will group them into a same _Cover Group_.  
  
#### Coverage DB
This DataBase handles the maintenance of the values that were sampled for each of the Cover Point bins. This allows us to know how much of the verification plan was tested.  
The DB also keeps mappings linking _Cover Groups_ to their contained _Cover Points_.  

### How to use it  
The Functional coverage system is compatible with the chisel _testers2_ framework.  
1. The `CoverageReporter` must first be instanciated within a chisel test.  
2. `CoverGroup`s can then be created by using the `register` method of the coverage reporter. This takes as parameter a `List[Cover]`. `Cover` represents either a `CoverPoint` or a `CoverCondition` that contains a `port` that will be sampled, a `portName` that will be shown in the report and either a `List[Bins]`, created from a name and a scala range, or a `List[Condition]`, created from a name and an arbitrary condition function.  
3. `CoverGroups` may also contain a `List[Cross]` which represents a set of hit relations between two ports.  
4. The port must then be manually sampled by calling the `sample` method of the coverage reporter.  
5. Once the test is done, a coverage report can be generated by calling the `printReport` or `report` methods of the coverage reporter.  
    
An example of this can be found [here](https://github.com/chiselverify/chiselverify/blob/master/src/test/scala/examples/leros/AluAccuTester.scala).  
  
### Timed Cross Coverage  
__Idea__: We want to check the relationship between two ports with a delay of a certain amount of cycles.  
__Example__: We have the following situation, imagine we have a device that breaks if:    
- `dut.io.a` takes the value of `1.U` at cycle 1  
- `dut.io.b` takes the value of `1.U` the following cycle  

We want to verify that the above case was tested. This can be done by defining a `TimedCross` between the two points:  
```scala
val cr = new CoverageReporter(dut)
cr.register(
      //Declare CoverPoints
      CoverPoint("a", dut.io.a)()),
      CoverPoint("b", dut.io.b)()),
      //Declare timed cross point with a delay of 1 cycle
      TimedCross("timedAB", dut.io.a, dut.io.b)(Exactly(1))(
            CrossBin("both1", 1 to 1, 1 to 1)
      )
)
```  

Using that, we can check that we tested the above case in our test suite.  
This construct can be used to check delay between two cover points.  
  
#### Use  
To be able to use the timed coverage, stepping the clock must be done through the coverage reporter:  
```scala
dut.clock.step(nCycles) //Will trigger an exception if used with Timed Cross Coverage
cr.step(nCycles) //Works
```  
This is done in order ensure that the coverage database will always remain synchronized with the DUT's internal clock.
  
#### Delay Types
The current implementation allows for the following special types of timing:  
- `Eventually`: This sees if a cross hit was detected at any point in the next given amount of cycles.  
- `Always`: This only considers a hit if the it was detected every cycle in the next given amount of cycles.  
- `Exactly`: This only considers a hit if it was detected exactly after a given amount of cycles.  

### Timed Assertions  
Delay types can also be used in order to used `Timed Assertions` or `Timed Expect`. These can be used in order to check an assertion, in the form of an arbitrary function, with an added timing argument. We could thus check, for example, that two ports are equal two cycles appart. For example:  
```scala
AssertTimed(dut,() => dut.io.a.peek() == dut.io.b.peek(), "aEqb expected timing is wrong")(Exactly(2)).join()
```
This can also be done more naturally with the `Expect` interface:  
```scala
ExpectTimed(dut,dut.io.a, dut.io.b.peek().litValue(), "aEqb expected timing is wrong")(Exactly(2)).join()
```

### Cover Conditions  
__Idea__: A type of coverpoint that can apply arbitrary hit conditions to an arbitrary number of ports.  
```scala
CoverCondition(readableName: String, ports: Data*)(conditions: Condition*)
//where a condition is
Condition(name: String, func : Seq[BigInt] => Boolean)
```
__Example__:
```scala
val cr = new CoverageReporter(dut)
cr.register(
  //Declare CoverPoints
  CoverCondition("aAndB", dut.io.outA, dut.io.outB)(
    Condition("aeqb", { case Seq(a, b) => a == b })
))
```
Bins are thus defined using arbitrary functions of the type `List[BigInt] => Boolean` which represent different hit conditions.
No coverage percentage is given due to cartesian product complexity. Instead we offer the possibility to use a user-defined "expected number of Hits" to get a coverage percentage. This looks like the following:
```scala
val cr = new CoverageReporter(dut)
cr.register(
  //Declare CoverPoints
  CoverCondition("aAndB", dut.io.outA, dut.io.outB)(
    Condition("asuptobAtLeast100Times", { case Seq(a, b) => a > b }, Some(100))
))
```
The above example results in the following coverage report:
```
============ COVERAGE REPORT ============
============== GROUP ID: 1 ==============
COVER_CONDITION NAME: aAndB
CONDITION aeqb HAS 4 HITS
CONDITION asuptobAtLeast100 HAS 95 HITS EXPECTED 100 = 95.0%
=========================================
=========================================
```

## Constrained Random Verification
The CRV package inside this project aims to mimic the functionality of SystemVerilog constraint programming and integrates them into [chisel-tester2](https://github.com/ucb-bar/chisel-testers2).
The CRV package combines a Constraint Satisfactory Problem Solver, with some helper classes to create and use random objects inside your tests.
Currently, only the [jacop](https://github.com/radsz/jacop) backend is supported, but in the future other backends can be added.

## Comparison
### System Verilog

```systemverilog
class frame_t;
rand pkt_type ptype;
rand integer len;
randc bit [1:0] no_repeat;
// Constraint the members
constraint legal {
  len >= 2;
  len <= 5;
}
```

### CRV / jacop backend
```scala
class Frame extends RandObj(new Model) {
  val pkType: Rand = new Rand(0, 3)
  val len: Rand = new Rand(0, 10)
  val noRepeat: Randc = new Randc(0, 1)

  val legal: ConstraintGroup = new ConstraintGroup {
    len #>= 2
    len #<= 5
  }
}
```

## Random Objects
Random objects can be created by extending the RandObj trait. This class accepts one parameter which is a Model. A model
correspond to a database in which all the random variables and constraints declared inside the RandObj are stored.
```scala
class Frame extends RandObj(new Model)
```
A model can be initialized with a seed `new Model(42)`, which allows the user to create reproducible tests.

### Random Fields
A random field can be added to a `RandObj` by declaring a Rand variable.
```scala
  val len: Rand = new Rand(0, 10)
```

Random-cyclic variable can be added by declaring a `Randc` field inside a `RandObj`
```scala
  val noRepeat: Randc = Randc(0, 1)
```

### Constraints
Each variable can have one or multiple constraints. Constraint relations are usually preceded by the `#` symbol.
```scala
len #>= 2
```
In the previous block of code we are specifying that the variable `len` can only take values that are grater then 2. 
Each constraint can be assigned to a variable and  enabled or disabled at any time during the test
```scala
val lenConstraint = len #> 2
[....]
lenConstraint.disable()
[....]
lenConstraint.enable()
```

Constraints can also be grouped together in a `ConstraintGroup` and the group itself can be enabled or disabled.

```scala
val legal: ConstraintGroup = new ConstraintGroup {
  len #>= 2
  len #<= 5
  payload.size #= len
}
[...]
legal.disable()
[...]
legal.enable()
```

By default, constraints and constraint groups are enabled when they are declared. 


The list of operator used to construct constraints is the following:
`#<`, `#<=`, `#>`, `#>=`,`#=`, `div`, `#*`, `mod`, `#+`, `-`, `#\=`, `#^`, `in`, `inside`

It is also possible to declare conditional constraints with constructors like `IfCon` and `IfElseCon`.
```scala
val constraint1: crv.Constraint = IfCon(len #= 1) {
        payload.size #= 3
    } ElseC {
        payload.size #= 10
    }
```

### Usage
As in SystemVerilog, each random class exposes a method called `randomize()` this method automatically solves the
constraint specified in the class and assign to each random filed a random value. The method returns `true`  only if the
CSP found a set of values that satisfy the current constraints.
```scala
val myPacket = new Frame(new Model)
assert(myPacket.randomize)
```

Other usage examples can be found in `src/test/scala/backends/jacopsrc/test/scala/verifyTests/crv/backends/jacop/`

## Example Use Cases

We will explore a handful of use cases to explore verification.

 * Leros ALU (basically done)
 * Heap priority queue (from MicroSemi),
   see also https://www.hackerearth.com/practice/notes/heaps-and-priority-queues/
 * Network-on-chip (in Chisel), see https://github.com/schoeberl/soc-comm
 * Decimation filter from WSA (VHDL code plus testbench given)

*******************************

# UVM Examples
In the early stages of this project, we explored the possibilty of using UVM to verify Chisel designs. The [sv](sv) directory thus contains a number of UVM examples.

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

************************************

# Resources
If you're interested in learning more about the UVM, we recommend that you explore the repository, as well as some of the following links:
* [First steps with UVM](https://www.youtube.com/watch?v=qLr8ayWM_Ww)
* [UVM Cookbook (requires an account)](https://verificationacademy.com/cookbook/uvm)
* [ChipVerify.com UVM Tutorials](https://www.chipverify.com/table/uvm/)
* [Ray Salemi's UVM Primer videos](https://www.youtube.com/watch?v=eeU2zpgXv1A&list=PLigQ6Cc3qFpI_WTgqtDXi_Msk3yRuKGGJ)

# Documents

Collect pointers to relevant documents.

## Related Work

 * https://github.com/ekiwi/paso
 * https://github.com/TsaiAnson/verif
 * *Layering RTL, SAFL, Handel-C and BluespecConstructs on Chisel HCL*, David J Greaves,
 see http://koo.corpus.cam.ac.uk/drafts/tndjg-008-transactional-modelling-in-chisel.html

### CRV
- [Choco-Solver](https://github.com/chocoteam/choco-solver) Java library for solving CSP problems
- [QuickCheck](https://hackage.haskell.org/package/QuickCheck) Checker for Haskel, used Lava as example, the inspiration for ScalaCheck

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
