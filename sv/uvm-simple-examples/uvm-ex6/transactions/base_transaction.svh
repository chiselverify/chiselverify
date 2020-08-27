class base_transaction extends uvm_sequence_item;

    //Notice that the transaction is registered with uvm_object_utils and not uvm_component_utils
    `uvm_object_utils(base_transaction)

    //By adding 'rand' we can use the seq.randomize() method to randomize our transaction values
    rand op_t op;
    rand int a;
	 rand int b;

	 //These constraints are the most basic, good for coverage checking
    constraint c_op { 
		 op >= 0; 
		 op <= 3; 
		}

    constraint c_ab { 
        a dist {0:/1, [1:254]:/2, 255:/1};
        b dist {0:/1, [1:254]:/2, 255:/1};
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
        base_transaction tx;
        $cast(tx, rhs);
        a  = tx.a;
        b = tx.b;
        op = tx.op;
    endfunction
    
    function bit do_compare(uvm_object rhs, uvm_comparer comparer);
        base_transaction tx;
        bit status = 1;
        $cast(tx, rhs);
        status &= (a  == tx.a);
        status &= (b == tx.b);
        status &= (op == tx.op);
        return status;
    endfunction

endclass: base_transaction