
TEST_NAME?=base_test

VERBOSITY?=UVM_MEDIUM

ms:
	vlib work
	# Compile with coverage for b=branch, c=condition, c=statement and t=toggle
	#By storing the files in files.f, the compilation order is guaranteed to be correct
	vlog -cover bcst -f files.f 

	#Command line, with coverage, random seed, top component, load more from sim.do
	vsim -c -coverage -sv_seed random +UVM_TESTNAME=$(TEST_NAME) +UVM_VERBOSITY=$(VERBOSITY) top -do sim.do