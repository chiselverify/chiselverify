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

// First Steps with UVM - Hello World
// See https://youtu.be/qLr8ayWM_Ww

// Author: John Aynsley, Doulos
// Date:   1-May-2012

// Modified by: Kasper Juul Hesse Rasmussen, DTU
// Summary: Added more comments to further explain the usage
// Date: 2020/08/14


`include "uvm_macros.svh" //A necessary part of all SV files that use UVM

package my_pkg;

	import uvm_pkg::*;

	//The environment is the container which stores the drivers, agenst, sequencers etc.
	//In this bare-bones example, the environment is empty
	class my_env extends uvm_env; 

		//Boilerplate. This macro is necessary to register the class in the UVM factory
		`uvm_component_utils(my_env)
		
		//Boilerplate. UVM requires that a class call its parent constructor
		function new(string name, uvm_component parent);
			super.new(name, parent);
		endfunction
	
  	endclass: my_env
  
	//The test is the highest-ranking UVM class, which instantiates the environment.
	//This is done such that the same test may be used on different environments
	class my_test extends uvm_test;
	
		//Boilerplate
		`uvm_component_utils(my_test)
		
		//Boilerplate
		function new(string name, uvm_component parent);
			super.new(name, parent);
		endfunction


		
		//Boilerplate. This section declares a my_env environment which is constructed during the build_phase
		my_env m_env;

		function void build_phase(uvm_phase phase);
			m_env = my_env::type_id::create("m_env", this);
		endfunction
		
		//The actual test, which is run when UVM enters the run_phase.
		//Objections are used for flow control. The test finishes when all objections have been dropped
		task run_phase(uvm_phase phase);
			phase.raise_objection(this);
			#10;
			`uvm_info("", "Hello World", UVM_MEDIUM)
			phase.drop_objection(this);
		endtask
     
  endclass: my_test
endpackage: my_pkg


module top;

  import uvm_pkg::*;
  import my_pkg::*;
  
  //In this example, the DUT and DUT Interface are actually not necessary, as the test does not rely on them.
  dut_if dut_if1 ();
  dut dut1 ( .dif(dut_if1) );

  initial
  begin
    run_test("my_test"); //Executes the test defined in the class my_test
  end

endmodule: top
