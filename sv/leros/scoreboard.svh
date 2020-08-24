//  Class: scoreboard
//
`uvm_analysis_imp_decl(_1)
class scoreboard extends uvm_scoreboard;
	`uvm_component_utils(scoreboard);

	//  Group: Components
	uvm_analysis_imp_1#(shortint, scoreboard) rslt_imp;
	uvm_tlm_analysis_fifo#(leros_command) cmd_fifo;	

	//  Group: Variables
	shortint unsigned accu = 0;
	shortint unsigned accu_next = 0;

	int good = 0;
	int bad = 0;
	int total = 0;

	//  Group: Functions
	//  Function: build_phase
	extern function void build_phase(uvm_phase phase);
	
	//  Function: report_phase
	extern function void report_phase(uvm_phase phase);
	
	//  Function: write_1
	extern function void write_1(shortint t);
	

	//  Constructor: new
	function new(string name = "scoreboard", uvm_component parent);
		super.new(name, parent);
	endfunction: new

	
endclass: scoreboard

function void scoreboard::build_phase(uvm_phase phase);
	rslt_imp = new("rslt_imp", this);
	cmd_fifo = new("cmd_fifo", this);
endfunction: build_phase

function void scoreboard::write_1(shortint t);
	leros_command cmd = new;
	shortint unsigned predicted_result;

	//Get command from FIFO
	if (!cmd_fifo.try_get(cmd)) begin
		`uvm_error(get_name(), "Unable to get cmd from fifo")
		return;
	end

	if(cmd.reset) begin
		accu_next = 0;
	end
	else begin
		case(cmd.op)
			ADD: accu_next += cmd.din;
			SUB: accu_next -= cmd.din;
			AND: accu_next = accu & cmd.din;
			OR : accu_next = accu | cmd.din;
			XOR: accu_next = accu ^ cmd.din;
			LD : accu_next = cmd.din;
			SHR: accu_next = (accu >> 1);
		 //NOP: Do nothing
		endcase
	end

	if(accu_next != t) begin //t=result from ALU
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