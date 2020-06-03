
doit:
	vlib work
	vlog +acc -incr -lint *.sv
	vopt test -o test_opt
	vsim -do sim.do test_opt
