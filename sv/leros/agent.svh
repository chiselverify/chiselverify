//  Class: agent
//
class agent extends uvm_agent;
    `uvm_component_utils(agent);

    //  Group: Configuration Object(s)
    agent_config m_agent_cfg;

    //  Group: Components
    monitor m_mon;
    my_sequencer m_seqr;
    driver m_driver;

	 //  Group: Variables
	 //Analysis port to forwarding items to scoreboard and coverage collector
    uvm_analysis_port #(leros_command) agent_ap;

    //  Function: build_phase
    extern function void build_phase(uvm_phase phase);

    //  Function: connect_phase
    extern function void connect_phase(uvm_phase phase);
    
    //  Constructor: new
    function new(string name = "agent", uvm_component parent);
        super.new(name, parent);
    endfunction: new
endclass: agent

function void agent::build_phase(uvm_phase phase);
    //Get agent config and set is_active
    if(! uvm_config_db#(agent_config)::get(this, "", "agent_cfg", m_agent_cfg))
        `uvm_fatal(get_name(), "Unable to get agent config")

    this.is_active = m_agent_cfg.is_active;

    //Always build the monitor and ap
    m_mon = monitor::type_id::create("m_mon", this);
    agent_ap = new("agent_ap", this);

    //Build driver and sequencer if necessary
    if(this.is_active) begin
        m_driver = driver::type_id::create("m_driver", this);
        m_seqr = my_sequencer::type_id::create("m_seqr", this);
    end
    
endfunction: build_phase

function void agent::connect_phase(uvm_phase phase);
    //Always forward the monitors connection outwards
    m_mon.mon_ap.connect(agent_ap);

    //Connect sequencer and driver if they exist
    if(this.is_active) begin
        m_driver.seq_item_port.connect(m_seqr.seq_item_export);
    end
endfunction: connect_phase