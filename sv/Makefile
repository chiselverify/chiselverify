
doit:
	vlib work
	vlog +acc -incr -lint *.sv
	vopt test -o test_opt
	vsim -do sim.do test_opt

vcs:
	vcs -sverilog -full64 -timescale=1ns/1ns *.sv
	./simv

clean:
	git clean -xfd

# Alberto's setup needs:
# tcsh
# source /apps/misc/02205/scripts/setup_synopsys_vcs.csh