//  Class: scoreboard_dpi
//  Uses the UVM DPI to implement the scoreboard checking function.

// Imports the scoreboard_check function defined in scoreboard.c
// DPI imports must happen outside of the class.
import "DPI-C" function int scoreboard_check(int din, int op, int reset, int fromDUT);

class scoreboard_dpi extends scoreboard;
	`uvm_component_utils(scoreboard_dpi);

	//  Function: write_1
	extern function void write_1(leros_command t);	

	//  Constructor: new
	function new(string name = "scoreboard_dpi", uvm_component parent);
		super.new(name, parent);		
	endfunction: new

endclass: scoreboard_dpi

function void scoreboard_dpi::write_1(leros_command t);
	int ret;

	ret = scoreboard_check(t.din, t.op, t.reset, t.accu);
	if(ret)
		good++;
	else begin
		bad++;
		`uvm_error(get_name(), $sformatf("Result did not match. accu=%d, din=%d, op=%s. Got %d, expected %d", accu, t.din, t.op.name, t.accu, accu))
	end
	total++;
endfunction