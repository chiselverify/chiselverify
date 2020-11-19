//----------------------------------------------------------------------
//  Copyright (c) 2011-2012 by Doulos Ltd.
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//----------------------------------------------------------------------

// First Steps with UVM - Sequencer-Driver Communication
// See https://youtu.be/aXhHW000IeI

// Author: John Aynsley, Doulos
// Date:   1-May-2012

// Modified by: Kasper Juul Hesse Rasmussen, DTU
// Summary: Added more comments to further explain the usage
// Date: 2020/08/17


`include "uvm_macros.svh"


package my_pkg;

	import uvm_pkg::*;
	import simplealu_pkg::*;

	// The result monitor hooks into the interface and samples our results every time 'done' is asserted
	class result_monitor extends uvm_monitor;

		`uvm_component_utils(result_monitor);

		virtual alu_if alif_v;

		//Create analysis port that transmits shortints (16 bits)
		uvm_analysis_port #(shortint) rslt_mon_ap;
	
		function new(string name, uvm_component parent);
			super.new(name, parent);
		endfunction

		function void build_phase(uvm_phase phase);
			// Get interface reference from config database
			if( !uvm_config_db #(virtual alu_if)::get(this, "", "alu_if", alif_v) )
				`uvm_error("", "uvm_config_db::get failed in result_monitor")
		
			//Build the analysis port
			rslt_mon_ap = new("rslt_mon_ap", this);
		endfunction

		virtual task run_phase(uvm_phase phase);
			shortint res;
			forever begin
				//On every rising edge of done, we have a valid result
				@(posedge alif_v.done);
				#1 //Wait for the value to be updated
				res = alif_v.result;

				rslt_mon_ap.write(res);
			end
		endtask
	endclass

	//Using this macro to generate an analysis port that can receive the results
	`uvm_analysis_imp_decl(_1)
	// The scoreboard tests whether the results obtained from the DUT match with the expected results
	class my_scoreboard extends uvm_scoreboard;
		`uvm_component_utils(my_scoreboard);

		uvm_analysis_imp_1#(shortint, my_scoreboard) rslt_imp; //Instantiating the analysis port declared with the macro
		uvm_tlm_analysis_fifo#(command) cmd_fifo;

		int good = 0;
		int bad = 0;
		int total = 0;
	
		function new(string name, uvm_component parent);
			super.new(name, parent);
			rslt_imp = new("rslt_imp", this);
			cmd_fifo = new("cmd_fifo", this);
		endfunction: new

		//Declaring the write() function for the analysis_imp
		function void write_1(shortint t);
			//Try to get a command from the fifo
			command cmd = new;
			shortint expected_result;
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
			end else
				good++;
				
			total++;
		endfunction

		function void report_phase(uvm_phase phase);
			super.report_phase(phase);
			`uvm_info(get_name(), $sformatf("Scoreboard finished\nGood: %3d, bad: %3d, total: %3d", good, bad, total), UVM_MEDIUM);
		endfunction: report_phase
		
	endclass: my_scoreboard
	

	//The command monitor hooks into the ALU interface. Every time "start" is asserted, it samples the values of a, b and op
	class command_monitor extends uvm_monitor;
		`uvm_component_utils(command_monitor);

		//Create a handle to the IF
		virtual alu_if alif_v;

		//Create analysis port that transmits "command" class objects.
		uvm_analysis_port #(command) cmd_mon_ap;
	
		function new(string name, uvm_component parent);
			super.new(name, parent);
		endfunction

		function void build_phase(uvm_phase phase);
			// Get interface reference from config database
			if( !uvm_config_db #(virtual alu_if)::get(this, "", "alu_if", alif_v) )
				`uvm_error("", "uvm_config_db::get failed in command_monitor")
		
			//Build the analysis port
			cmd_mon_ap = new("cmd_mon_ap", this);
		endfunction

		virtual task run_phase(uvm_phase phase);
			command cmd = new;
			forever begin
				//On every rising edge of start, we have a valid command which we choose to sample
				@(posedge alif_v.start);
				cmd.a = alif_v.a;
				cmd.b = alif_v.b;
				cmd.op = alif_v.op;

				cmd_mon_ap.write(cmd);

			end
		endtask

	endclass;

	//Creating a coverage component which takes objects of type 'command' and processes them
	//By extending uvm_subscriber, we automatically gain an analysis_export
	class coverage_comp extends uvm_subscriber #(command);
		`uvm_component_utils(coverage_comp)
		command cmd = new;

		//Set up coverage and covergroups
		//We want to try all operations with all zeros and all ones and random data
		covergroup zeros_and_ones;
			as: coverpoint cmd.a {
				bins zeros = {'0};
				bins ones = {'1};
				bins others = {[8'h1:8'hfe]};
			}
			bs: coverpoint cmd.b {
				bins zeros = {'0};
				bins ones = {'1};
				bins others = {[8'h1:8'hfe]};
			}

			ops: coverpoint cmd.op;

			cross as, bs, ops; 
		endgroup

		//====BOILERPLATE START====//
		function new(string name, uvm_component parent);
			super.new(name,parent);
			zeros_and_ones = new; //We have to instantiate the covergroup
		endfunction

		function void build_phase(uvm_phase phase);
			super.build_phase(phase);
		endfunction
		//===BOILERPLATE END ======//

		//Gets called every time the analysis port we are subscribing to calls its write() method.
		function void write(command t);
			cmd.a = t.a;
			cmd.b = t.b;
			cmd.op = t.op;
			zeros_and_ones.sample();
		endfunction
	endclass
	
	//A sequence item (transaction) is the item which a sequencer will generate 
	//In the transaction, the different values of the transaction are defined, and constraints are added to obtain sensible values
	class my_transaction extends uvm_sequence_item;
	
		//Notice that the transaction is registered with uvm_object_utils and not uvm_component_utils
		`uvm_object_utils(my_transaction)
	
		//By adding 'rand' we can use the seq.randomize() (below) method to randomize our transaction values
		rand op_t op;
		rand int a;
		rand int b;

		constraint c_op { op >= 0; op < 4; }
		constraint c_ab { 
			a dist {0:/1, [1:254]:/1, 255:/1};
			b dist {0:/1, [1:254]:/1, 255:/1};
		 }
		
		//Since sequences are objects and not components, they do not have a parent in the UVM hierarchy
		//Thus, they only have one parameter for their constructor
		function new (string name = "");
			super.new(name);
		endfunction
		
		//The three following helper methods should be implemented for all classes that extend uvm_sequence_item
		function string convert2string;
			return $sformatf("op=%b, a=%0d, b=%0d", op.name, a, b);
		endfunction

		function void do_copy(uvm_object rhs);
			my_transaction tx;
			$cast(tx, rhs);
			a  = tx.a;
			b = tx.b;
			op = tx.op;
		endfunction
		
		function bit do_compare(uvm_object rhs, uvm_comparer comparer);
			my_transaction tx;
			bit status = 1;
			$cast(tx, rhs);
			status &= (a  == tx.a);
			status &= (b == tx.b);
			status &= (op == tx.op);
			return status;
		endfunction

	endclass: my_transaction

	//The sequencer is generated using a typedef of a UVM sequencer parameterized to use my_transaction uvm_sequence_item's
	//It is also possible to defined my_sequencer as a class that extends uvm_sequencer, but that is not necessary for this example
	typedef uvm_sequencer #(my_transaction) my_sequencer;

	//The sequence defines the transactions of type my_transaction that will be generated.
	//These are all randomly generated, but they may be customized based on the current state of the DUT or other parameters
	class my_sequence extends uvm_sequence #(my_transaction);
	
		`uvm_object_utils(my_sequence)
		
		function new (string name = "");
			super.new(name);
		endfunction

		//All uvm_sequence implementations must have a 'task body'
		task body;

			//starting_phase is a variable defined in uvm_sequence. 
			//If the sequence instantiates other child sequences, these children will *not* raise an objection
			if (starting_phase != null)
				starting_phase.raise_objection(this);

			//Here, the sequence generates the transactions of type my_transaction, randomizes them and passes them onto the driver
			repeat(100)
			begin
				req = my_transaction::type_id::create("req");
				start_item(req);
				if( !req.randomize() )
					`uvm_error("", "Randomize failed")
				finish_item(req);
			end
			
			if (starting_phase != null)
				starting_phase.drop_objection(this);
		endtask: body
	
	endclass: my_sequence
	
	//Most of the driver code is similar to that contained in the driver from uvm-ex2
	//Notice that it has been parameterized to take transactions of type my_transaction
	class my_driver extends uvm_driver #(my_transaction);
	
		`uvm_component_utils(my_driver)

		virtual alu_if alif_v;

		function new(string name, uvm_component parent);
			super.new(name, parent);
		endfunction
		
		function void build_phase(uvm_phase phase);
			// Get interface reference from config database
			if( !uvm_config_db #(virtual alu_if)::get(this, "", "alu_if", alif_v) )
				`uvm_error("", "uvm_config_db::get failed")
		endfunction 
	
		task run_phase(uvm_phase phase);
			forever
			begin
				//Here, the driver gets the next transaction on its seq_item_port (a buit-in port)
				seq_item_port.get_next_item(req);

				// Wiggle pins of DUT in correct manner
				@(negedge alif_v.clock);
				alif_v.a = req.a;
				alif_v.b = req.b;
				alif_v.op = req.op;
				alif_v.start = '1;

				@(negedge alif_v.clock);
				alif_v.start = '0;

				@(posedge alif_v.done);
				//The result is ready		
				//And signals to the sequencer that it is finished driving the transaction
				seq_item_port.item_done();
			end
		endtask

	endclass: my_driver
	
	
	class my_env extends uvm_env;

		`uvm_component_utils(my_env)
		
		my_sequencer m_seqr;
		my_driver    m_driv;
		command_monitor cmd_monitor;
		coverage_comp cov_comp;
		result_monitor rslt_monitor;
		my_scoreboard scoreboard;
		
		function new(string name, uvm_component parent);
			super.new(name, parent);
		endfunction

		function void build_phase(uvm_phase phase);
			m_seqr = my_sequencer::type_id::create("m_seqr", this);
			m_driv = my_driver::type_id::create("m_driv", this);
			cmd_monitor = command_monitor::type_id::create("cmd_monitor", this);
			cov_comp = coverage_comp::type_id::create("cov_comp", this);
			rslt_monitor = result_monitor::type_id::create("rslt_monitor", this);
			scoreboard = my_scoreboard::type_id::create("scoreboard", this);
		endfunction
		
		//During the connect_phase, the drivers seq_item_port is connected to the sequencers seq_item_export
		function void connect_phase(uvm_phase phase);
			m_driv.seq_item_port.connect( m_seqr.seq_item_export );

			//And the analysis port of the cmd_monitor is connected to the export of cov_comp
			cmd_monitor.cmd_mon_ap.connect(cov_comp.analysis_export);

			//And the analysis port and FIFO of the scoreboard are connected as well
			cmd_monitor.cmd_mon_ap.connect(scoreboard.cmd_fifo.analysis_export);
			rslt_monitor.rslt_mon_ap.connect(scoreboard.rslt_imp);
		endfunction
		
	endclass: my_env
	
	class my_test extends uvm_test;
	
		`uvm_component_utils(my_test)
		
		my_env m_env;
		
		function new(string name, uvm_component parent);
			super.new(name, parent);
		endfunction
		
		function void build_phase(uvm_phase phase);
			m_env = my_env::type_id::create("m_env", this);
		endfunction
		
		//During the run phase, the sequence is created and randomized, and the sequence is passed to the sequencer defined in the environment
		//Notice that seq is created here, and not in the build phase. This is because seq is not a component in the component hierarchy.
		task run_phase(uvm_phase phase);
			my_sequence seq;
			seq = my_sequence::type_id::create("seq");
			if( !seq.randomize() ) 
				`uvm_error("", "Randomize failed")
			seq.starting_phase = phase;
			seq.start( m_env.m_seqr );
		endtask
		
	endclass: my_test
endpackage: my_pkg

module top;

		import uvm_pkg::*;
		import my_pkg::*;
		import simplealu_pkg::*;
		
		alu_if alif ();
		
		simplealu sa ( .alif );

		// Clock generator
		initial
		begin
			alif.clock = 0;
			forever #5 alif.clock = ~alif.clock;
		end

		initial
		begin
			uvm_config_db #(virtual alu_if)::set(null, "*", "alu_if", alif);
			uvm_top.finish_on_completion = 1;
			run_test("my_test");
		end

		always @(negedge alif.done) begin
			// `uvm_info("", $sformatf("a: %d, b: %d, op: %s, res: %d, time=%d", alif.a, alif.b, alif.op.name, alif.result, $time), UVM_MEDIUM);
		end

endmodule: top
