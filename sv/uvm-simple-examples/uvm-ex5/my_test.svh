
class my_test extends uvm_test;

    `uvm_component_utils(my_test)
    
    my_env m_env;
	 global_config g_cfg;

    function new(string name, uvm_component parent);
		  super.new(name, parent);
    endfunction
    
    function void build_phase(uvm_phase phase);
		  m_env = my_env::type_id::create("m_env", this);
		  
		  g_cfg = new;
		  g_cfg.no_runs = 10;
		  uvm_config_db#(global_config)::set(null, "*", "g_cfg", g_cfg);
		  
    endfunction
    
    //During the run phase, the sequence is created and randomized, and the sequence is passed to the sequencer defined in the environment
    //Notice that seq is created here, and not in the build phase. This is because seq is not a component in the component hierarchy.
    task run_phase(uvm_phase phase);
        my_sequence seq;
        seq = my_sequence::type_id::create("seq");
      //   if( !seq.randomize() ) 
      //       `uvm_error("", "Randomize failed")
        seq.starting_phase = phase;
        seq.start( m_env.m_seqr );
    endtask
    
endclass: my_test