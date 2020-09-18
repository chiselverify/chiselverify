
TESTNAME?=base_test

VERBOSITY?=UVM_MEDIUM

PLUSARGS?=

UVM?=$(UVM_HOME)

TOP?=top

ms:
	vlib work
# Compile with coverage for b=branch, c=condition, c=statement and t=toggle
#By storing the files in files.f, the compilation order is guaranteed to be correct
	vlog -cover bcst -f files.f 
# Compile our single VHDL file as well
	vcom -2008 accualu.vhd

#Command line, with coverage, random seed, UVM args, load more from sim.do
	vsim -c -coverage -sv_seed random +UVM_TESTNAME=$(TESTNAME) +UVM_VERBOSITY=$(VERBOSITY) $(PLUSARGS) $(TOP) -do sim.do

#Compiles with VCS, explicitly pointing to UVM files
vcs: 
# Compile VHDL files with 2008 compatibility
	vhdlan -vhdl08 -full64 accualu.vhd
# Compile SV files with UVM library
	vlogan -sverilog -full64 -CFLAGS -DVCS +incdir+$(UVM)/src +incdir+$(UVM)/src/dpi/uvm_dpi.cc $(UVM)/src/uvm.sv -f files.f
#ntb_opts uvm is necessary for compilation with VHDL DUT, though I don't know why
	vcs -full64 -ntb_opts uvm -top $(TOP)
#Runs the simulation
	./simv +UVM_TESTNAME=$(TESTNAME) +UVM_VERBOSITY=$(VERBOSITY) $(PLUSARGS)


clean:
	rm -rf 64
	rm -rf AN.DB
	rm -rf csrc
	rm -rf simv.*
	rm -rf work
	rm -rf simv
	rm -f .vlogansetup.args
	rm -f .vlogansetup.env