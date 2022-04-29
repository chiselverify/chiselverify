package examples

import chisel3.util.DecoupledIO
import chiseltest._
import chiselverify.coverage._
import chiselverify.coverage.{cover => ccover}
import chiselverify.timing._
import chisel3._
import org.scalatest.flatspec.AnyFlatSpec

class ArbiterTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Arbiter"

  it should "pass" in {
    test(new Arbiter(4, UInt(8.W))).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      /* SYSTEM_VERILOG VERIFICATION PLAN:
      covergroup cg_output;
      	OUT_READY: coverpoint dut.io_out_ready {
      		bins zero = {0};
            bins one = {1};
            bins transitionzto 0 => 1;
            bins transitionotz 1 => 0;
      	}
      	OUT_VALID: coverpoint dut.io_out_valid {
      	    bins zero = {0};
            bins one = {1};
            bins transitionzto 0 => 1;
            bins transitionotz 1 => 0;
      	}
      	OUT_BITS: coverpoint dut.io_out_bits;
      endgroup: cg_output
      covergroup cg_input0;
      	IN0_READY: coverpoint dut.io_in_0_ready {
      		bins zero = {0};
            bins one = {1};
            bins transitionzto 0 => 1;
            bins transitionotz 1 => 0;
      	}
      	IN1_VALID: coverpoint dut.io_in_0_valid {
      	    bins zero = {0};
            bins one = {1};
            bins transitionzto 0 => 1;
            bins transitionotz 1 => 0;
      	}
      	IN3_BITS: coverpoint dut.io_in_0_bits;
      endgroup: cg_input0
      covergroup cg_input1;
      	IN0_READY: coverpoint dut.io_in_0_ready {
      		bins zero = {0};
            bins one = {1};
            bins transitionzto 0 => 1;
            bins transitionotz 1 => 0;
      	}
      	IN1_VALID: coverpoint dut.io_in_0_valid {
      	    bins zero = {0};
            bins one = {1};
            bins transitionzto 0 => 1;
            bins transitionotz 1 => 0;
      	}
      	IN3_BITS: coverpoint dut.io_in_0_bits;
      endgroup: cg_input1
      covergroup cg_input2;
      	IN0_READY: coverpoint dut.io_in_0_ready {
      		bins zero = {0};
            bins one = {1};
            bins transitionzto 0 => 1;
            bins transitionotz 1 => 0;
      	}
      	IN1_VALID: coverpoint dut.io_in_0_valid {
      	    bins zero = {0};
            bins one = {1};
            bins transitionzto 0 => 1;
            bins transitionotz 1 => 0;
      	}
      	IN3_BITS: coverpoint dut.io_in_0_bits;
      endgroup: cg_input2
      covergroup cg_input3;
      	IN0_READY: coverpoint dut.io_in_0_ready {
      		bins zero = {0};
            bins one = {1};
            bins transitionzto 0 => 1;
            bins transitionotz 1 => 0;
      	}
      	IN1_VALID: coverpoint dut.io_in_0_valid {
      	    bins zero = {0};
            bins one = {1};
            bins transitionzto 0 => 1;
            bins transitionotz 1 => 0;
      	}
      	IN3_BITS: coverpoint dut.io_in_0_bits;
      endgroup: cg_input3
      covergroup cg_input4;
      	IN0_READY: coverpoint dut.io_in_0_ready {
      		bins zero = {0};
            bins one = {1};
            bins transitionzto 0 => 1;
            bins transitionotz 1 => 0;
      	}
      	IN1_VALID: coverpoint dut.io_in_0_valid {
      	    bins zero = {0};
            bins one = {1};
            bins transitionzto 0 => 1;
            bins transitionotz 1 => 0;
      	}
      	IN3_BITS: coverpoint dut.io_in_0_bits;
      endgroup: cg_input4
      covergroup cg_input5;
      	IN0_READY: coverpoint dut.io_in_0_ready {
      		bins zero = {0};
            bins one = {1};
            bins transitionzto 0 => 1;
            bins transitionotz 1 => 0;
      	}
      	IN1_VALID: coverpoint dut.io_in_0_valid {
      	    bins zero = {0};
            bins one = {1};
            bins transitionzto 0 => 1;
            bins transitionotz 1 => 0;
      	}
      	IN3_BITS: coverpoint dut.io_in_0_bits;
      endgroup: cg_input5
      covergroup cg_input6;
      	IN0_READY: coverpoint dut.io_in_0_ready {
      		bins zero = {0};
            bins one = {1};
            bins transitionzto 0 => 1;
            bins transitionotz 1 => 0;
      	}
      	IN1_VALID: coverpoint dut.io_in_0_valid {
      	    bins zero = {0};
            bins one = {1};
            bins transitionzto 0 => 1;
            bins transitionotz 1 => 0;
      	}
      	IN3_BITS: coverpoint dut.io_in_0_bits;
      endgroup: cg_input6
      covergroup cg_input7;
      	IN0_READY: coverpoint dut.io_in_0_ready {
      		bins zero = {0};
            bins one = {1};
            bins transitionzto 0 => 1;
            bins transitionotz 1 => 0;
      	}
      	IN1_VALID: coverpoint dut.io_in_0_valid {
      	    bins zero = {0};
            bins one = {1};
            bins transitionzto 0 => 1;
            bins transitionotz 1 => 0;
      	}
      	IN3_BITS: coverpoint dut.io_in_0_bits;
      endgroup: cg_input7


      //WITH LOOPS AND ARRAYS (NOT REAL)
      covergroup cg_input_bits(ref bit [32:0] in_bits);
        IN_BITS: coverpoint in_bits;
      endgroup: cg_input_bits
      covergroup cg_input_ready(ref bit [0:0] in_ready);
        IN_READY: coverpoint in_ready {
            bins zero = {0};
            bins one = {1};
            bins transitionzto 0 => 1;
            bins transitionotz 1 => 0;
        }
      endgroup: cg_input_ready
      covergroup cg_input_valid(ref bit [0:0] in_valid);
        IN_VALID: coverpoint in_valid {
            bins zero = {0};
            bins one = {1};
            bins transitionzto 0 => 1;
            bins transitionotz 1 => 0;
        }
      endgroup: cg_input_valid
      cg_input_bits input_bits_cg[$size(dut.in_bits)];
      initial begin
         foreach (in_bits[i])
            input_bits_cg[i] = new(in_bits[i]);
      end
      cg_input_ready input_ready_cg[$size(dut.in_ready)];
      initial begin
         foreach (in_ready[i])
            input_ready_cg[i] = new(in_ready[i]);
      end
      cg_input_valid input_valid_cg[$size(dut.in_valid)];
      initial begin
         foreach (in_valid[i])
            input_ready_cg[i] = new(in_valid[i]);
      end
      */
      //Define Verification plan
      val cr = new CoverageReporter(dut)
      //Register output covers
      cr.register(
        ccover("out.ready", dut.io.out.ready)(
          bin("ReadyStates", 0 to 1)),
        ccover("out.valid", dut.io.out.valid)(
          bin("ValidStates", 0 to 1)),
        ccover("out.ready.transitions", dut.io.out.ready, dut.io.out.ready)(Exactly(1))(
          cross("outready0to1", Seq(0 to 0, 1 to 1), 1),
          cross("outready1to0", Seq(1 to 1, 0 to 0), 1)),
        ccover("out.valid.transitions", dut.io.out.valid, dut.io.out.valid)(Exactly(1))(
          cross("outvalid0to1", Seq(0 to 0, 1 to 1), 1),
          cross("outvalid1to0", Seq(1 to 1, 0 to 0), 1)),
        ccover("out.bits", dut.io.out.bits)(DefaultBin(dut.io.out.bits))
      )
      //Register input covers
      dut.io.in.foreach((input: DecoupledIO[UInt]) => {
        cr.register(
          ccover(s"in${input.hashCode()}.ready", input.ready)(
            bin("ReadyStates", 0 to 1)),
          ccover(s"in${input.hashCode()}.valid", input.valid)(
            bin("ValidStates", 0 to 1)),
          ccover(s"in${input.hashCode()}.ready.transitions", input.ready, input.ready)(Exactly(1))(
            cross(s"in${input.hashCode()}ready0to1", Seq(0 to 0, 1 to 1), 1),
            cross(s"in${input.hashCode()}ready1to0", Seq(1 to 1, 0 to 0), 1)),
          ccover(s"in${input.hashCode()}.valid.transitions", input.valid, input.valid)(Exactly(1))(
            cross(s"in${input.hashCode()}valid0to1", Seq(0 to 0, 1 to 1), 1),
            cross(s"in${input.hashCode()}valid1to0", Seq(1 to 1, 0 to 0), 1)),
          ccover(s"in${input.hashCode()}.bits", input.bits)(DefaultBin(input.bits))
        )
      })

      // test(new Arbiter(4, UInt(8.W))) { dut =>
      for (i <- 0 until 4) {
        dut.io.in(i).valid.poke(false.B)
      }
      dut.io.out.ready.poke(false.B) // Keep the output till we read it
      dut.io.in(2).valid.poke(true.B)
      dut.io.in(2).bits.poke(2.U)
      while(!dut.io.in(2).ready.peek().litToBoolean) {
        cr.step()
      }
      cr.step()
      dut.io.in(2).valid.poke(false.B)
      cr.step(10)
      // disable for now to avoid Travis issue.
      dut.io.out.bits.expect(2.U)
      cr.printReport()
    }
  }

  // TODO: finish the test
  it should "be fair" in {
    test(new Arbiter(5, UInt(16.W))).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      for (i <- 0 until 5) {
        dut.io.in(i).valid.poke(true.B)
        dut.io.in(i).bits.poke((i*100).U)
      }
      // println("Result should be " + List(0, 100, 200, 300, 400).sum)
      dut.io.out.ready.poke(true.B)
      dut.clock.step()
      for (i <- 0 until 40) {
        if (dut.io.out.valid.peek().litToBoolean) {
          println(dut.io.out.bits.peek().litValue)
        }
        dut.clock.step()
      }
    }
  }
}

