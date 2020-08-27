//  Class: scoreboard
//
`uvm_analysis_imp_decl(_1)
class scoreboard extends uvm_scoreboard;
	`uvm_component_utils(scoreboard);

	//  Group: Components
	uvm_analysis_imp_1#(leros_command, scoreboard) rslt_imp;

	//  Group: Variables
	shortint accu = 0;
	shortint accu_next = 0;

	int good = 0;
	int bad = 0;
	int total = 0;

	//  Group: Functions
	//  Function: build_phase
	extern function void build_phase(uvm_phase phase);
	
	//  Function: report_phase
	extern function void report_phase(uvm_phase phase);
	
	//  Function: write_1
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
		accu_next = 0;
	end
	else begin
		case(t.op)
			ADD: accu_next += t.din;
			SUB: accu_next -= t.din;
			AND: accu_next = accu & t.din;
			OR : accu_next = accu | t.din;
			XOR: accu_next = accu ^ t.din;
			LD : accu_next = t.din;
			SHR: accu_next = (accu >> 1);
		 // NOP: Do nothing
		endcase
	end

	if(accu_next != t.accu) begin //t=result from ALU
		`uvm_error(get_name(), $sformatf("Result did not match. accu=%d, din=%d, op=%s. Got %d, expected %d", accu, cmd.din, cmd.op.name, t, accu_next))
		bad++;
	end
	else begin
		good++;
	end	
	total++;
	accu = accu_next;
endfunction

function void scoreboard::report_phase(uvm_phase phase);
	super.report_phase(phase);
	`uvm_info(get_name(), $sformatf("Scoreboard finished\nGood: %3d, bad: %3d, total: %3d", good, bad, total), UVM_MEDIUM);
endfunction: report_phase