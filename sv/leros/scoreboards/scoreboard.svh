//  Class: scoreboard
//
`uvm_analysis_imp_decl(_1)
class scoreboard extends uvm_scoreboard;
	`uvm_component_utils(scoreboard);

	//  Group: Components
	uvm_analysis_imp_1#(leros_command, scoreboard) rslt_imp;

	//  Group: Variables
	int accu = 0;
	int good = 0;
	int bad = 0;
	int total = 0;

	//  Group: Functions
	extern function void build_phase(uvm_phase phase);
	
	extern function void report_phase(uvm_phase phase);
	
	extern function void write_1(leros_command t);
	
	//  Constructor: new
	function new(string name = "scoreboard", uvm_component parent);
		super.new(name, parent);
	endfunction: new

endclass: scoreboard

function void scoreboard::build_phase(uvm_phase phase);
	rslt_imp = new("rslt_imp", this);
endfunction: build_phase

function void scoreboard::write_1(leros_command t);
	leros_command cmd = new;

	if(t.reset) begin
		accu = 0;
	end
	else begin
		case(t.op)
			ADD: accu += t.din;
			SUB: accu -= t.din;
			AND: accu = accu & t.din;
			OR : accu = accu | t.din;
			XOR: accu = accu ^ t.din;
			LD : accu = t.din;
			SHR: accu = (accu >> 1);
		 // NOP: Do nothing
		endcase
	end

	if(accu != t.accu) begin //t.accu=result from ALU
		`uvm_error(get_name(), $sformatf("Result did not match. accu=%d, din=%d, op=%s. Got %d, expected %d", accu, cmd.din, cmd.op.name, t.accu, accu))
		bad++;
	end
	else begin
		good++;
	end	
	total++;
endfunction

function void scoreboard::report_phase(uvm_phase phase);
	super.report_phase(phase);
	`uvm_info(get_name(), $sformatf("Scoreboard finished\nGood: %3d, bad: %3d, total: %3d", good, bad, total), UVM_MEDIUM);
endfunction: report_phase