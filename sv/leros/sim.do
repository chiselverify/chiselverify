#Enable VCD output and add all signals
vcd file output.vcd
vcd add dif/*
vcd add mydut/alu/*

# When finished, don't exit but remain running
onfinish stop
run -all


# Generate coverage report and export to coverage.txt
#For some reason, this requires another "run" command. But if we remove "onfinish stop" it won't execute
coverage report -detail -cvg -file coverage.txt

exit