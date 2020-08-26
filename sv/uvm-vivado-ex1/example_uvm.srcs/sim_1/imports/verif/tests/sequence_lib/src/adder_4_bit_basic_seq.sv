`ifndef ADDER_4_BIT_BASIC_SEQ 
`define ADDER_4_BIT_BASIC_SEQ

class adder_4_bit_basic_seq extends uvm_sequence#(adder_4_bit_transaction);
   
  ///////////////////////////////////////////////////////////////////////////////
  // Declaration of Sequence utils
  //////////////////////////////////////////////////////////////////////////////
  `uvm_object_utils(adder_4_bit_basic_seq)
  ///////////////////////////////////////////////////////////////////////////////
  // Method name : new
  // Description : sequence constructor
  //////////////////////////////////////////////////////////////////////////////
  function new(string name = "adder_4_bit_basic_seq");
    super.new(name);
  endfunction
  ///////////////////////////////////////////////////////////////////////////////
  // Method name : body 
  // Description : Body of sequence to send randomized transaction through
  // sequencer to driver
  //////////////////////////////////////////////////////////////////////////////
  virtual task body();
   for(int i=0;i<`NO_OF_TRANSACTIONS;i++) begin
      req = adder_4_bit_transaction::type_id::create("req");
      start_item(req);
      assert(req.randomize());  
      `uvm_info(get_full_name(),$sformatf("RANDOMIZED TRANSACTION FROM SEQUENCE"),UVM_LOW);
      req.print();
      finish_item(req);
      get_response(rsp);
    end
  endtask
   
endclass

`endif


