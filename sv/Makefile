#Do it with ModelSim
doit:
	vlib work
#	vlog +acc -incr -lint *.sv
	vlog *.sv
	vsim -do sim.do test

#Do it with Synopsys VCS
vcs:
	vcs -sverilog -debug_access+all -full64 -timescale=1ns/1ns *.sv
	./simv

clean:
	git clean -xfd

# Alberto's setup needs:
# tcsh
# source /apps/misc/02205/scripts/setup_synopsys_vcs.csh
