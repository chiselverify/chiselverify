
TESTNAME?=base_test

VERBOSITY?=UVM_MEDIUM

PLUSARGS?=

UVM?=$(UVM_HOME)

ms:
	vlib work
	# Compile with coverage for b=branch, c=condition, c=statement and t=toggle
	#By storing the files in files.f, the compilation order is guaranteed to be correct
	vlog -cover bcst -f files.f 

	#Command line, with coverage, random seed, top component, load more from sim.do
	vsim -c -coverage -sv_seed random +UVM_TESTNAME=$(TESTNAME) +UVM_VERBOSITY=$(VERBOSITY) $(PLUSARGS) top -do sim.do
	grep COVERGROUP coverage.txt

vcs: #Compiles with VCS, explicitly pointing to UVM files
	vcs -sverilog -full64 -CFLAGS -DVCS -debug_access+all +incdir+$(UVM)/src $(UVM)/src/dpi/uvm_dpi.cc $(UVM)/src/uvm.sv -f files.f
	./simv +UVM_TESTNAME=$(TESTNAME) +UVM_VERBOSITY=$(VERBOSITY) $(PLUSARGS) -ucli