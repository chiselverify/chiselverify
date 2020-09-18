//  Class: base_transaction
class base_transaction extends uvm_sequence_item;
	typedef base_transaction this_type_t;
	`uvm_object_utils(base_transaction);

	//  Group: Variables
	rand bit [31:0] din;
	rand leros_op_t op;
	rand bit is_reset;

	//  Group: Constraints
	constraint c_din {'0 <= din; din <= '1; } //All possible values

	constraint c_op {'0 <= op; op <= '1; } //All possible values

	constraint c_reset { // Reset every once in a while
		is_reset dist {
			0:=9,
			1:=1
		};
	}

	//  Constructor: new
	function new(string name = "base_transaction");
		super.new(name);
	endfunction: new

	//  Function: do_copy
	extern function void do_copy(uvm_object rhs);
	//  Function: do_compare
	extern function bit do_compare(uvm_object rhs, uvm_comparer comparer);
	//  Function: convert2string
	extern function string convert2string();
	
endclass: base_transaction

/*----------------------------------------------------------------------------*/
/*  Functions                                                                 */
/*----------------------------------------------------------------------------*/

function string base_transaction::convert2string();
	string s;

	/*  chain the convert2string with parent classes  */
	s = super.convert2string();

	/*  list of local properties to be printed:  */
	   s = {s, $sformatf("op: %s, din: %b, reset: %d", op.name, din, is_reset)};

	return s;
endfunction: convert2string

function void base_transaction::do_copy(uvm_object rhs);
	this_type_t rhs_;

	if (!$cast(rhs_, rhs)) begin
		`uvm_error({this.get_name(), ".do_copy()"}, "Cast failed!");
		return;
	end

	/*  chain the copy with parent classes  */
	super.do_copy(rhs);

	/*  list of local properties to be copied  */
	this.op = rhs_.op;
	this.din = rhs_.din;
	this.is_reset = rhs_.is_reset;
endfunction: do_copy

function bit base_transaction::do_compare(uvm_object rhs, uvm_comparer comparer);
	this_type_t rhs_;

	if (!$cast(rhs_, rhs)) begin
		`uvm_error({this.get_name(), ".do_compare()"}, "Cast failed!");
		return 0;
	end

	/*  chain the compare with parent classes  */
	do_compare = super.do_compare(rhs, comparer);

	/*  list of local properties to be compared:  */
	do_compare &= (
		this.op == rhs_.op &&
		this.din == rhs_.din &&
		this.is_reset == rhs_.is_reset
	);
endfunction: do_compare