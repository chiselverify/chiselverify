# Compile with verify.coverage for b=branch, c=condition, c=statement and t=toggle
vlog -cover bcst *.sv

# Simulate with verify.coverage
vsim -verify.coverage -sv_seed random top

#Enable VCD output and add all signals
vcd file output.vcd
vcd add alif/*

# When finished, don't exit but remain running
onfinish stop
run -all


# Generate verify.coverage report and export to verify.coverage.txt
#For some reason, this requires another "run" command. But if we remove "onfinish stop" it won't execute
verify.coverage report -detail -cvg -directive -comments -file verify.coverage.txt -noa /my_pkg/coverage_comp/zeros_and_ones