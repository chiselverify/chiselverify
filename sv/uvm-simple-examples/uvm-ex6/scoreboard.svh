
//Using this macro to generate an analysis port that can receive the results
`uvm_analysis_imp_decl(_1)
// The scoreboard tests whether the results obtained from the DUT match with the expected results
class my_scoreboard extends uvm_scoreboard;
	`uvm_component_utils(my_scoreboard);

	uvm_analysis_imp_1#(shortint, my_scoreboard) rslt_imp; //Instantiating the analysis port declared with the macro above
	uvm_tlm_analysis_fifo#(command) cmd_fifo; //And a FIFO to store command information

	int good = 0;
	int bad = 0;
	int total = 0;
	int max = 1;
	global_config g_cfg;
	
	function new(string name, uvm_component parent);
		super.new(name, parent);
		rslt_imp = new("rslt_imp", this);
		cmd_fifo = new("cmd_fifo", this);
		
		if (!uvm_config_db#(global_config)::get(this, "", "g_cfg", g_cfg))
			`uvm_error(get_name(), "Unable to get g_cfg")
		else
			max = g_cfg.no_runs;
		  
    endfunction: new

    // This is some housekeeping functionality
    // The sequence/driver drops its objection on the rising edge of 'done', but the result_monitor can't sample it yet
    // This function is called by UVM whenever a phase ends. When we're leaving the run phase and have yet to check all combinations,
    // an objection is raised and we step for #2 until the total increases. 
    function void phase_ready_to_end (uvm_phase phase);
		if(phase.is(uvm_run_phase::get)) begin
			if (total < max) begin
				phase.raise_objection(this);
				fork begin
						while(total < max) begin
							#2;
						end
						phase.drop_objection(this);
				end
				join_none
			end
		end
	 endfunction
    

    //Declaring the write() function for the rslt_imp
    function void write_1(shortint t);
        //Try to get a command from the fifo
        command cmd = new;
        shortint unsigned expected_result;
        if(!cmd_fifo.try_get(cmd)) begin
            `uvm_error(get_name(), "Could not get CMD from Fifo");
            return;
        end
            
        case(cmd.op)
            ADD: expected_result = cmd.a + cmd.b;
            SUB: expected_result = cmd.a - cmd.b;
            XOR: expected_result = cmd.a ^ cmd.b;
            MUL: expected_result = cmd.a * cmd.b;
        endcase //cmd.op

        if(expected_result != t) begin
            `uvm_error(get_name(), $sformatf("Result did not match. a=%d, b=%d, op=%s, res=%d. Expected %d", cmd.a, cmd.b, cmd.op.name, t, expected_result));
            bad++;
        end else begin
				good++;
				`uvm_info(get_name(), $sformatf("Result matched. a=%d, b=%d, op=%s, res=%d", cmd.a, cmd.b, cmd.op, expected_result), UVM_HIGH)
		  end
            
        total++;
    endfunction

    function void report_phase(uvm_phase phase);
        super.report_phase(phase);
        `uvm_info(get_name(), $sformatf("Scoreboard finished\nGood: %3d, bad: %3d, total: %3d", good, bad, total), UVM_MEDIUM);
    endfunction: report_phase
    
endclass: my_scoreboard