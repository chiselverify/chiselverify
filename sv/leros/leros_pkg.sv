//  Package: leros_pkg
//
package leros_pkg;
	import uvm_pkg::*;
	`include "uvm_macros.svh"


	//  Group: Typedefs
	typedef enum logic[2:0] { NOP, ADD, SUB, AND, OR, XOR, LD, SHR } leros_op_t;

	//  Group: Parameters

	//  Group: Helper classes
	class leros_command;
		leros_op_t op;
		logic [31:0] din;
		logic reset;
		logic [31:0] accu;
	endclass

	class agent_config extends uvm_object;
		uvm_active_passive_enum is_active;
	endclass;

	class sequence_config;
		int no_repeats;
	endclass;


	//  Group: File includes
	`include "base_transaction.svh"
	`include "edge_transaction.svh"

	typedef uvm_sequencer#(base_transaction) my_sequencer;

	`include "base_sequence.svh"
	`include "reset_sequence.svh"
	`include "random_sequence.svh"
	`include "edge_sequence.svh"


	`include "driver.svh"
	`include "monitor.svh"
	`include "coverage.svh"
	`include "scoreboard.svh"
	`include "scoreboard_dpi.svh"

	`include "agent.svh"

	`include "env.svh"
	`include "base_test.svh"
	`include "edge_test.svh"
	`include "random_test.svh"
	
endpackage: leros_pkg
