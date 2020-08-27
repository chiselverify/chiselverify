#Location of UVM files
UVM?=$(UVM_HOME)

ms: #Compiles using built-in UVM
	vlib work
	vsim -c -do sim.do

vcs: #Compiles with VCS, explicitly pointing to UVM files
	vcs -sverilog -full64 -CFLAGS -DVCS +incdir+$(UVM)/src $(UVM)/src/dpi/uvm_dpi.cc $(UVM)/src/uvm.sv *.sv
	./simv