//  Class: coverage
//
class coverage extends uvm_subscriber #(leros_command);
	`uvm_component_utils(coverage);

	//  Group: Variables
	leros_command cmd  = new;

	// Group: Covergroups

	//  Covergroup: cg_post_rst
	//  Ensures that all possible commands have been attempted after a reset
	covergroup cg_post_rst;
		
		UPDOWN: coverpoint cmd.reset {
			bins updown = (1 => 0);
		}

		OPS: coverpoint cmd.op;

		cross UPDOWN, OPS;
	endgroup: cg_post_rst

	//  Covergroup: cg_all_zeros_ones
	//  Ensures that all OPs have been attempted with zeros, ones and random values
	covergroup cg_all_zeros_ones;
		OPS: coverpoint cmd.op;

		DIN: coverpoint cmd.din {
			bins zeros = {'0};
			bins ones = {'1};
			bins others = default; //All the rest
		}
	endgroup: cg_all_zeros_ones

	//  Group: Functions
	//  Function: build_phase
	extern function void build_phase(uvm_phase phase);
		
	extern function void write(leros_command t);
	
	//  Constructor: new
	function new(string name = "coverage", uvm_component parent);
		super.new(name, parent);

		//Instantiate covergroups
		cg_post_rst = new;
		cg_all_zeros_ones = new;
	endfunction: new
	
endclass: coverage

/*----------------------------------------------------------------------------*/
/*  Functions                                                                 */
/*----------------------------------------------------------------------------*/

function void coverage::write(leros_command t);
	cmd.op = t.op;
	cmd.din = t.din;
	cmd.reset = t.reset;

	cg_all_zeros_ones.sample();
	cg_post_rst.sample();
endfunction;

/*----------------------------------------------------------------------------*/
/*  UVM Build Phases                                                          */
/*----------------------------------------------------------------------------*/
function void coverage::build_phase(uvm_phase phase);
	/*  note: Do not call super.build_phase() from any class that is extended from an UVM base class!  */
	/*  For more information see UVM Cookbook v1800.2 p.503  */
	super.build_phase(phase);

	cmd = new;
endfunction: build_phase



