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

// First Steps with UVM - The DUT Interface
// See https://youtu.be/FkclDiK4Oco

// Author: John Aynsley, Doulos
// Date:   1-May-2012

// Modified by: Kasper Juul Hesse Rasmussen, DTU
// Summary: Added more comments to further explain the usage
// Date: 2020/08/14


`include "uvm_macros.svh"

package my_pkg;

	import uvm_pkg::*;

	//The driver drives inputs onto the DUT's interface.
	//Using a virtual interface the two are not directly linked, allowing for a more generic structure
	class my_driver extends uvm_driver;

		//Register the driver in the factory
		`uvm_component_utils(my_driver)

		//Declare the virtual interface
		virtual dut_if dut_vi;

		//Boilerplate
		function new(string name, uvm_component parent);
			super.new(name, parent);
		endfunction
		
		//Boilerplate code to run during build_phase
		function void build_phase(uvm_phase phase);
			// Get interface reference from config database
			//Boilerplate. Parameters to get are (caller, path, name, value). In almost all cases, caller=this and path=""
			//The name in the DB is the same as the type of the interface. This is not a requirement
			//Value is the virtual interface we wish to link to dut_if
			if( !uvm_config_db #(virtual dut_if)::get(this, "", "dut_if", dut_vi) )
				`uvm_error("", "uvm_config_db::get failed")
		endfunction 
		
		//Boilerplate code to run during run_phase. Wiggling pins of interface
		task run_phase(uvm_phase phase);
			forever
			begin
			// Wiggle pins of DUT
			@(posedge dut_vi.clock);
			dut_vi.cmd  <= $urandom;
			dut_vi.addr <= $urandom;
			dut_vi.data <= $urandom;
			end
		endtask

	endclass: my_driver
	
	
	class my_env extends uvm_env;

		`uvm_component_utils(my_env)
		
		my_driver m_driv;
		
		function new(string name, uvm_component parent);
			super.new(name, parent);
		endfunction
	
		//Notice that the driver is created under the environment during the build_phase
		function void build_phase(uvm_phase phase);
			m_driv = my_driver::type_id::create("m_driv", this);
		endfunction
		
	endclass: my_env
	
	
	class my_test extends uvm_test;
	
		`uvm_component_utils(my_test)
		
		my_env m_env;
		
		function new(string name, uvm_component parent);
			super.new(name, parent);
		endfunction
		
		//Notice that the environment (containing the driver) is built under the test during the build_phase
		function void build_phase(uvm_phase phase);
			m_env = my_env::type_id::create("m_env", this);
		endfunction
		
		task run_phase(uvm_phase phase);
			phase.raise_objection(this);
			#80;
			phase.drop_objection(this);
		endtask
		
	endclass: my_test
	
  
endpackage: my_pkg


module top;
	import my_pkg::*;
	import uvm_pkg::*;

	
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
		//Here, the interface dut_if1 (which is connected to dut1), is stored in the DB under the name dut_if
		//Parameters are (caller, path, name, value)
		//Again, in most cases caller=null and path="*"
		uvm_config_db #(virtual dut_if)::set(null, "*", "dut_if", dut_if1);
		
		//This causes the SV $finish to be called once all objections have been dropped, stopping the clock generator and ending the test.
		uvm_top.finish_on_completion = 1;
		
		run_test("my_test");
	end

endmodule: top
