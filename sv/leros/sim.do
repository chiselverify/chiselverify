#Enable VCD output and add all signals
vcd file output.vcd
vcd add dif/*
vcd add mydut/alu/*

# When finished, don't exit but remain running
onfinish stop
run -all


# Generate verify.coverage report and export to verify.coverage.txt
#For some reason, this requires another "run" command. But if we remove "onfinish stop" it won't execute
verify.coverage report -detail -cvg -file verify.coverage.txt

exit