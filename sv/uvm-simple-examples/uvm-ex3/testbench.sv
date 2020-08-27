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

	//A sequence item (transaction) is the item which a sequencer will generate 
	//In the transaction, the different values of the transaction are defined, and constraints are added to obtain sensible values
	class my_transaction extends uvm_sequence_item;
	
		//Notice that the transaction is registered with uvm_object_utils and not uvm_component_utils
		`uvm_object_utils(my_transaction)
	
		//By adding 'rand' we can use the seq.randomize() (below) method to randomize our transaction values
		rand bit cmd;
		rand int addr;
		rand int data;
		
		constraint c_addr { addr >= 0; addr < 256; }
		constraint c_data { data >= 0; data < 256; }
		
		//Since sequences are objects and not components, they do not have a parent in the UVM hierarchy
		//Thus, they only have one parameter for their constructor
		function new (string name = "");
			super.new(name);
		endfunction
		
		//The three following helper methods should be implemented for all classes that extend uvm_sequence_item
		function string convert2string;
			return $sformatf("cmd=%b, addr=%0d, data=%0d", cmd, addr, data);
		endfunction

		function void do_copy(uvm_object rhs);
			my_transaction tx;
			$cast(tx, rhs);
			cmd  = tx.cmd;
			addr = tx.addr;
			data = tx.data;
		endfunction
		
		function bit do_compare(uvm_object rhs, uvm_comparer comparer);
			my_transaction tx;
			bit status = 1;
			$cast(tx, rhs);
			status &= (cmd  == tx.cmd);
			status &= (addr == tx.addr);
			status &= (data == tx.data);
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
			repeat(8)
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

		virtual dut_if dut_vi;

		function new(string name, uvm_component parent);
			super.new(name, parent);
		endfunction
		
		function void build_phase(uvm_phase phase);
			// Get interface reference from config database
			if( !uvm_config_db #(virtual dut_if)::get(this, "", "dut_if", dut_vi) )
				`uvm_error("", "uvm_config_db::get failed")
		endfunction 
	
		task run_phase(uvm_phase phase);
			forever
			begin
				//Here, the driver gets the next transaction on its seq_item_port
				seq_item_port.get_next_item(req);

				// Wiggle pins of DUT
				@(posedge dut_vi.clock);
				dut_vi.cmd  = req.cmd;
				dut_vi.addr = req.addr;
				dut_vi.data = req.data;
				
				//And signals to the sequencer that it is finished
				seq_item_port.item_done();
			end
		endtask

	endclass: my_driver
	
	
	class my_env extends uvm_env;

		`uvm_component_utils(my_env)
		
		//Now, the sequencer and driver are instantiated in the environment
		my_sequencer m_seqr;
		my_driver    m_driv;
		
		function new(string name, uvm_component parent);
			super.new(name, parent);
		endfunction

		function void build_phase(uvm_phase phase);
			m_seqr = my_sequencer::type_id::create("m_seqr", this);
			m_driv = my_driver   ::type_id::create("m_driv", this);
		endfunction
		
		//During the connect_phase, the drivers seq_item_port is connected to the sequencers seq_item_export
		function void connect_phase(uvm_phase phase);
			m_driv.seq_item_port.connect( m_seqr.seq_item_export );
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
		
		//During the run phase, the sequence is created and randomized, and the sequence is started on the sequencer defined in the environment
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

//Unchanged from uvm-ex2
module top;

		import uvm_pkg::*;
		import my_pkg::*;
		
		dut_if dut_if1 ();
		
		dut dut1 ( .dif(dut_if1) );

		// Clock generator
		initial
		begin
			dut_if1.clock = 0;
			forever #5 dut_if1.clock = ~dut_if1.clock;
		end

		initial
		begin
			uvm_config_db #(virtual dut_if)::set(null, "*", "dut_if", dut_if1);
			uvm_top.finish_on_completion = 1;
			run_test("my_test");
		end

endmodule: top
