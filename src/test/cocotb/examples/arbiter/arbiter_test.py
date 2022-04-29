import random
import cocotb
from cocotb_coverage.coverage import *
from cocotb.clock import Clock
from cocotb.triggers import RisingEdge

range_relation = lambda val_, bin_ : bin_[0] <= val_ <= bin_[1]
Arbiter_Coverage = coverage_section(
    CoverPoint("top.io_out_ready", vname="io_out_ready", rel = range_relation, 
        bins = [(0,1), 0, 1], bins_labels = ["ReadyStates", "ready0", "ready1"]),
    CoverPoint("top.io_out_valid", vname="top.io_out_valid", rel = range_relation, 
        bins = [(0,1)], bins_labels = ["ValidStates"]),
    CoverCross("top.io_out_ready", items = ["top.io_out_ready", "top.io_out_ready"],
        ign_bins = [("ready0", "ready1"), ("ready1", "ready0")])
)

@cocotb.test()
async def basic_arbiter_test(dut):
    # Setup clock and log
    log = cocotb.logging.getLogger("cocotb.test")
    clock = Clock(dut.clock, 2, units="ns")  # Create a 500MHz period clock on port clock
    cocotb.start_soon(clock.start())  # Start the clock

    await RisingEdge(dut.clock)  # Synchronize with the clock

    @Arbiter_Coverage
    @cocotb.coroutine
    async def test():
        dut.io_in_0_valid.value = 0
        dut.io_in_1_valid.value = 0
        dut.io_in_2_valid.value = 0
        dut.io_in_3_valid.value = 0

        dut.io_out_ready.value = 0
        dut.io_in_2_valid.value = 1
        dut.io_in_2_bits.value = 2

        while dut.io_in_2_ready.value == 0:
            yield RisingEdge(dut.clock)
        yield RisingEdge(dut.clock)

        dut.io_in_2_valid.value = 0

        # Step 10 cycles
        for _ in range(10):
            yield RisingEdge(dut.clock)
    
        assert dut.io_out_bits == 2

    await test()
    coverage_db.report_coverage(log.info, bins=True)
