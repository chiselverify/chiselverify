
class my_env extends uvm_env;

    `uvm_component_utils(my_env)
    
    my_sequencer m_seqr;
    my_driver    m_driv;
    command_monitor cmd_monitor;
    coverage_comp cov_comp;
    result_monitor rslt_monitor;
    my_scoreboard scoreboard;
    
    function new(string name, uvm_component parent);
        super.new(name, parent);
    endfunction

    function void build_phase(uvm_phase phase);
        m_seqr = my_sequencer::type_id::create("m_seqr", this);
        m_driv = my_driver::type_id::create("m_driv", this);
        cmd_monitor = command_monitor::type_id::create("cmd_monitor", this);
        cov_comp = coverage_comp::type_id::create("cov_comp", this);
        rslt_monitor = result_monitor::type_id::create("rslt_monitor", this);
        scoreboard = my_scoreboard::type_id::create("scoreboard", this);
    endfunction
    
    //During the connect_phase, the drivers seq_item_port is connected to the sequencers seq_item_export
    function void connect_phase(uvm_phase phase);
        m_driv.seq_item_port.connect( m_seqr.seq_item_export );

        //And the analysis port of the cmd_monitor is connected to the export of cov_comp
        cmd_monitor.cmd_mon_ap.connect(cov_comp.analysis_export);

        //And the analysis port and FIFO of the scoreboard are connected as well
        cmd_monitor.cmd_mon_ap.connect(scoreboard.cmd_fifo.analysis_export);
        rslt_monitor.rslt_mon_ap.connect(scoreboard.rslt_imp);
    endfunction
    
endclass: my_env