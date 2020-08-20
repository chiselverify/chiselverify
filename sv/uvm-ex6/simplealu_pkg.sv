package simplealu_pkg;
	import uvm_pkg::*;
	`include "uvm_macros.svh"

	/*
	==== TYPEDEFS
	*/
	typedef enum logic [1:0] { ADD, SUB, XOR, MUL } op_t;

	/*
	==== HELPER CLASSES
	*/
	class command;
		logic[7:0] a,b;
		op_t op;
	endclass

	/*
	==== CONFIG CLASSES
	*/
	class global_config;
		int no_runs;
	endclass


	/*
	==== FILE INCLUDES
	*/
	//Notice that the order of inclusion is non-trivial. We cannot compile "my_test" before "my_env", as my_test instantiates my_env
	`include "base_transaction.svh"
	`include "largenums_transaction.svh"
	typedef uvm_sequencer #(base_transaction) my_sequencer;

	`include "my_driver.svh"
	`include "base_sequence.svh"
	`include "addonly_sequence.svh"
	`include "subonly_sequence.svh"
	`include "runall_sequence.svh"
	
	`include "command_monitor.svh"
	`include "result_monitor.svh"

	`include "coverage_comp.svh"
	`include "scoreboard.svh"
	
	`include "my_env.svh"

	`include "base_test.svh"
	`include "runall_test.svh"
	`include "largenums_test.svh"

endpackage