module Arbiter(
  input        clock,
  input        reset,
  output       io_in_0_ready,
  input        io_in_0_valid,
  input  [7:0] io_in_0_bits,
  output       io_in_1_ready,
  input        io_in_1_valid,
  input  [7:0] io_in_1_bits,
  output       io_in_2_ready,
  input        io_in_2_valid,
  input  [7:0] io_in_2_bits,
  output       io_in_3_ready,
  input        io_in_3_valid,
  input  [7:0] io_in_3_bits,
  output       io_in_4_ready,
  input        io_in_4_valid,
  input  [7:0] io_in_4_bits,
  output       io_in_5_ready,
  input        io_in_5_valid,
  input  [7:0] io_in_5_bits,
  output       io_in_6_ready,
  input        io_in_6_valid,
  input  [7:0] io_in_6_bits,
  input        io_out_ready,
  output       io_out_valid,
  output [7:0] io_out_bits
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
  reg [31:0] _RAND_2;
  reg [31:0] _RAND_3;
  reg [31:0] _RAND_4;
  reg [31:0] _RAND_5;
  reg [31:0] _RAND_6;
  reg [31:0] _RAND_7;
  reg [31:0] _RAND_8;
  reg [31:0] _RAND_9;
  reg [31:0] _RAND_10;
  reg [31:0] _RAND_11;
`endif // RANDOMIZE_REG_INIT
  reg [7:0] io_out_regData; // @[Arbiter.scala 40:22]
  reg [1:0] io_out_regState; // @[Arbiter.scala 41:27]
  wire  io_out_out_valid = io_out_regState == 2'h2 | io_out_regState == 2'h3; // @[Arbiter.scala 46:37]
  reg [1:0] io_out_regState_3; // @[Arbiter.scala 41:27]
  wire  io_out_out_ready = io_out_regState_3 == 2'h1; // @[Arbiter.scala 45:25]
  wire [1:0] _GEN_4 = io_out_out_ready ? 2'h1 : io_out_regState; // @[Arbiter.scala 66:26 67:20 41:27]
  wire [1:0] _GEN_5 = io_out_out_ready ? 2'h0 : io_out_regState; // @[Arbiter.scala 71:26 72:20 41:27]
  wire [1:0] _GEN_6 = 2'h3 == io_out_regState ? _GEN_5 : io_out_regState; // @[Arbiter.scala 48:22 41:27]
  reg [7:0] io_out_regData_1; // @[Arbiter.scala 40:22]
  reg [1:0] io_out_regState_1; // @[Arbiter.scala 41:27]
  wire  io_out_out_1_valid = io_out_regState_1 == 2'h2 | io_out_regState_1 == 2'h3; // @[Arbiter.scala 46:37]
  reg [1:0] io_out_regState_4; // @[Arbiter.scala 41:27]
  wire  io_out_out_1_ready = io_out_regState_4 == 2'h0; // @[Arbiter.scala 44:25]
  wire [1:0] _GEN_16 = io_out_out_1_ready ? 2'h1 : io_out_regState_1; // @[Arbiter.scala 66:26 67:20 41:27]
  wire [1:0] _GEN_17 = io_out_out_1_ready ? 2'h0 : io_out_regState_1; // @[Arbiter.scala 71:26 72:20 41:27]
  wire [1:0] _GEN_18 = 2'h3 == io_out_regState_1 ? _GEN_17 : io_out_regState_1; // @[Arbiter.scala 48:22 41:27]
  reg [7:0] io_out_regData_2; // @[Arbiter.scala 40:22]
  reg [1:0] io_out_regState_2; // @[Arbiter.scala 41:27]
  wire  io_out_out_2_valid = io_out_regState_2 == 2'h2 | io_out_regState_2 == 2'h3; // @[Arbiter.scala 46:37]
  wire  io_out_out_2_ready = io_out_regState_4 == 2'h1; // @[Arbiter.scala 45:25]
  wire [1:0] _GEN_28 = io_out_out_2_ready ? 2'h1 : io_out_regState_2; // @[Arbiter.scala 66:26 67:20 41:27]
  wire [1:0] _GEN_29 = io_out_out_2_ready ? 2'h0 : io_out_regState_2; // @[Arbiter.scala 71:26 72:20 41:27]
  wire [1:0] _GEN_30 = 2'h3 == io_out_regState_2 ? _GEN_29 : io_out_regState_2; // @[Arbiter.scala 48:22 41:27]
  reg [7:0] io_out_regData_3; // @[Arbiter.scala 40:22]
  wire  io_out_out_3_valid = io_out_regState_3 == 2'h2 | io_out_regState_3 == 2'h3; // @[Arbiter.scala 46:37]
  reg [1:0] io_out_regState_5; // @[Arbiter.scala 41:27]
  wire  io_out_out_3_ready = io_out_regState_5 == 2'h0; // @[Arbiter.scala 44:25]
  wire [1:0] _GEN_40 = io_out_out_3_ready ? 2'h1 : io_out_regState_3; // @[Arbiter.scala 66:26 67:20 41:27]
  wire [1:0] _GEN_41 = io_out_out_3_ready ? 2'h0 : io_out_regState_3; // @[Arbiter.scala 71:26 72:20 41:27]
  wire [1:0] _GEN_42 = 2'h3 == io_out_regState_3 ? _GEN_41 : io_out_regState_3; // @[Arbiter.scala 48:22 41:27]
  reg [7:0] io_out_regData_4; // @[Arbiter.scala 40:22]
  wire  io_out_out_4_valid = io_out_regState_4 == 2'h2 | io_out_regState_4 == 2'h3; // @[Arbiter.scala 46:37]
  wire  io_out_out_4_ready = io_out_regState_5 == 2'h1; // @[Arbiter.scala 45:25]
  wire [1:0] _GEN_52 = io_out_out_4_ready ? 2'h1 : io_out_regState_4; // @[Arbiter.scala 66:26 67:20 41:27]
  wire [1:0] _GEN_53 = io_out_out_4_ready ? 2'h0 : io_out_regState_4; // @[Arbiter.scala 71:26 72:20 41:27]
  wire [1:0] _GEN_54 = 2'h3 == io_out_regState_4 ? _GEN_53 : io_out_regState_4; // @[Arbiter.scala 48:22 41:27]
  reg [7:0] io_out_regData_5; // @[Arbiter.scala 40:22]
  wire [1:0] _GEN_64 = io_out_ready ? 2'h1 : io_out_regState_5; // @[Arbiter.scala 66:26 67:20 41:27]
  wire [1:0] _GEN_65 = io_out_ready ? 2'h0 : io_out_regState_5; // @[Arbiter.scala 71:26 72:20 41:27]
  wire [1:0] _GEN_66 = 2'h3 == io_out_regState_5 ? _GEN_65 : io_out_regState_5; // @[Arbiter.scala 48:22 41:27]
  assign io_in_0_ready = io_out_regState_3 == 2'h0; // @[Arbiter.scala 44:25]
  assign io_in_1_ready = io_out_regState == 2'h0; // @[Arbiter.scala 44:25]
  assign io_in_2_ready = io_out_regState == 2'h1; // @[Arbiter.scala 45:25]
  assign io_in_3_ready = io_out_regState_1 == 2'h0; // @[Arbiter.scala 44:25]
  assign io_in_4_ready = io_out_regState_1 == 2'h1; // @[Arbiter.scala 45:25]
  assign io_in_5_ready = io_out_regState_2 == 2'h0; // @[Arbiter.scala 44:25]
  assign io_in_6_ready = io_out_regState_2 == 2'h1; // @[Arbiter.scala 45:25]
  assign io_out_valid = io_out_regState_5 == 2'h2 | io_out_regState_5 == 2'h3; // @[Arbiter.scala 46:37]
  assign io_out_bits = io_out_regData_5; // @[Arbiter.scala 42:19 77:14]
  always @(posedge clock) begin
    if (2'h0 == io_out_regState) begin // @[Arbiter.scala 48:22]
      if (io_in_1_valid) begin // @[Arbiter.scala 50:24]
        io_out_regData <= io_in_1_bits; // @[Arbiter.scala 51:19]
      end
    end else if (2'h1 == io_out_regState) begin // @[Arbiter.scala 48:22]
      if (io_in_2_valid) begin // @[Arbiter.scala 58:24]
        io_out_regData <= io_in_2_bits; // @[Arbiter.scala 59:19]
      end
    end
    if (reset) begin // @[Arbiter.scala 41:27]
      io_out_regState <= 2'h0; // @[Arbiter.scala 41:27]
    end else if (2'h0 == io_out_regState) begin // @[Arbiter.scala 48:22]
      if (io_in_1_valid) begin // @[Arbiter.scala 50:24]
        io_out_regState <= 2'h2; // @[Arbiter.scala 52:20]
      end else begin
        io_out_regState <= 2'h1; // @[Arbiter.scala 54:20]
      end
    end else if (2'h1 == io_out_regState) begin // @[Arbiter.scala 48:22]
      if (io_in_2_valid) begin // @[Arbiter.scala 58:24]
        io_out_regState <= 2'h3; // @[Arbiter.scala 60:20]
      end else begin
        io_out_regState <= 2'h0; // @[Arbiter.scala 62:20]
      end
    end else if (2'h2 == io_out_regState) begin // @[Arbiter.scala 48:22]
      io_out_regState <= _GEN_4;
    end else begin
      io_out_regState <= _GEN_6;
    end
    if (reset) begin // @[Arbiter.scala 41:27]
      io_out_regState_3 <= 2'h0; // @[Arbiter.scala 41:27]
    end else if (2'h0 == io_out_regState_3) begin // @[Arbiter.scala 48:22]
      if (io_in_0_valid) begin // @[Arbiter.scala 50:24]
        io_out_regState_3 <= 2'h2; // @[Arbiter.scala 52:20]
      end else begin
        io_out_regState_3 <= 2'h1; // @[Arbiter.scala 54:20]
      end
    end else if (2'h1 == io_out_regState_3) begin // @[Arbiter.scala 48:22]
      if (io_out_out_valid) begin // @[Arbiter.scala 58:24]
        io_out_regState_3 <= 2'h3; // @[Arbiter.scala 60:20]
      end else begin
        io_out_regState_3 <= 2'h0; // @[Arbiter.scala 62:20]
      end
    end else if (2'h2 == io_out_regState_3) begin // @[Arbiter.scala 48:22]
      io_out_regState_3 <= _GEN_40;
    end else begin
      io_out_regState_3 <= _GEN_42;
    end
    if (2'h0 == io_out_regState_1) begin // @[Arbiter.scala 48:22]
      if (io_in_3_valid) begin // @[Arbiter.scala 50:24]
        io_out_regData_1 <= io_in_3_bits; // @[Arbiter.scala 51:19]
      end
    end else if (2'h1 == io_out_regState_1) begin // @[Arbiter.scala 48:22]
      if (io_in_4_valid) begin // @[Arbiter.scala 58:24]
        io_out_regData_1 <= io_in_4_bits; // @[Arbiter.scala 59:19]
      end
    end
    if (reset) begin // @[Arbiter.scala 41:27]
      io_out_regState_1 <= 2'h0; // @[Arbiter.scala 41:27]
    end else if (2'h0 == io_out_regState_1) begin // @[Arbiter.scala 48:22]
      if (io_in_3_valid) begin // @[Arbiter.scala 50:24]
        io_out_regState_1 <= 2'h2; // @[Arbiter.scala 52:20]
      end else begin
        io_out_regState_1 <= 2'h1; // @[Arbiter.scala 54:20]
      end
    end else if (2'h1 == io_out_regState_1) begin // @[Arbiter.scala 48:22]
      if (io_in_4_valid) begin // @[Arbiter.scala 58:24]
        io_out_regState_1 <= 2'h3; // @[Arbiter.scala 60:20]
      end else begin
        io_out_regState_1 <= 2'h0; // @[Arbiter.scala 62:20]
      end
    end else if (2'h2 == io_out_regState_1) begin // @[Arbiter.scala 48:22]
      io_out_regState_1 <= _GEN_16;
    end else begin
      io_out_regState_1 <= _GEN_18;
    end
    if (reset) begin // @[Arbiter.scala 41:27]
      io_out_regState_4 <= 2'h0; // @[Arbiter.scala 41:27]
    end else if (2'h0 == io_out_regState_4) begin // @[Arbiter.scala 48:22]
      if (io_out_out_1_valid) begin // @[Arbiter.scala 50:24]
        io_out_regState_4 <= 2'h2; // @[Arbiter.scala 52:20]
      end else begin
        io_out_regState_4 <= 2'h1; // @[Arbiter.scala 54:20]
      end
    end else if (2'h1 == io_out_regState_4) begin // @[Arbiter.scala 48:22]
      if (io_out_out_2_valid) begin // @[Arbiter.scala 58:24]
        io_out_regState_4 <= 2'h3; // @[Arbiter.scala 60:20]
      end else begin
        io_out_regState_4 <= 2'h0; // @[Arbiter.scala 62:20]
      end
    end else if (2'h2 == io_out_regState_4) begin // @[Arbiter.scala 48:22]
      io_out_regState_4 <= _GEN_52;
    end else begin
      io_out_regState_4 <= _GEN_54;
    end
    if (2'h0 == io_out_regState_2) begin // @[Arbiter.scala 48:22]
      if (io_in_5_valid) begin // @[Arbiter.scala 50:24]
        io_out_regData_2 <= io_in_5_bits; // @[Arbiter.scala 51:19]
      end
    end else if (2'h1 == io_out_regState_2) begin // @[Arbiter.scala 48:22]
      if (io_in_6_valid) begin // @[Arbiter.scala 58:24]
        io_out_regData_2 <= io_in_6_bits; // @[Arbiter.scala 59:19]
      end
    end
    if (reset) begin // @[Arbiter.scala 41:27]
      io_out_regState_2 <= 2'h0; // @[Arbiter.scala 41:27]
    end else if (2'h0 == io_out_regState_2) begin // @[Arbiter.scala 48:22]
      if (io_in_5_valid) begin // @[Arbiter.scala 50:24]
        io_out_regState_2 <= 2'h2; // @[Arbiter.scala 52:20]
      end else begin
        io_out_regState_2 <= 2'h1; // @[Arbiter.scala 54:20]
      end
    end else if (2'h1 == io_out_regState_2) begin // @[Arbiter.scala 48:22]
      if (io_in_6_valid) begin // @[Arbiter.scala 58:24]
        io_out_regState_2 <= 2'h3; // @[Arbiter.scala 60:20]
      end else begin
        io_out_regState_2 <= 2'h0; // @[Arbiter.scala 62:20]
      end
    end else if (2'h2 == io_out_regState_2) begin // @[Arbiter.scala 48:22]
      io_out_regState_2 <= _GEN_28;
    end else begin
      io_out_regState_2 <= _GEN_30;
    end
    if (2'h0 == io_out_regState_3) begin // @[Arbiter.scala 48:22]
      if (io_in_0_valid) begin // @[Arbiter.scala 50:24]
        io_out_regData_3 <= io_in_0_bits; // @[Arbiter.scala 51:19]
      end
    end else if (2'h1 == io_out_regState_3) begin // @[Arbiter.scala 48:22]
      if (io_out_out_valid) begin // @[Arbiter.scala 58:24]
        io_out_regData_3 <= io_out_regData; // @[Arbiter.scala 59:19]
      end
    end
    if (reset) begin // @[Arbiter.scala 41:27]
      io_out_regState_5 <= 2'h0; // @[Arbiter.scala 41:27]
    end else if (2'h0 == io_out_regState_5) begin // @[Arbiter.scala 48:22]
      if (io_out_out_3_valid) begin // @[Arbiter.scala 50:24]
        io_out_regState_5 <= 2'h2; // @[Arbiter.scala 52:20]
      end else begin
        io_out_regState_5 <= 2'h1; // @[Arbiter.scala 54:20]
      end
    end else if (2'h1 == io_out_regState_5) begin // @[Arbiter.scala 48:22]
      if (io_out_out_4_valid) begin // @[Arbiter.scala 58:24]
        io_out_regState_5 <= 2'h3; // @[Arbiter.scala 60:20]
      end else begin
        io_out_regState_5 <= 2'h0; // @[Arbiter.scala 62:20]
      end
    end else if (2'h2 == io_out_regState_5) begin // @[Arbiter.scala 48:22]
      io_out_regState_5 <= _GEN_64;
    end else begin
      io_out_regState_5 <= _GEN_66;
    end
    if (2'h0 == io_out_regState_4) begin // @[Arbiter.scala 48:22]
      if (io_out_out_1_valid) begin // @[Arbiter.scala 50:24]
        io_out_regData_4 <= io_out_regData_1; // @[Arbiter.scala 51:19]
      end
    end else if (2'h1 == io_out_regState_4) begin // @[Arbiter.scala 48:22]
      if (io_out_out_2_valid) begin // @[Arbiter.scala 58:24]
        io_out_regData_4 <= io_out_regData_2; // @[Arbiter.scala 59:19]
      end
    end
    if (2'h0 == io_out_regState_5) begin // @[Arbiter.scala 48:22]
      if (io_out_out_3_valid) begin // @[Arbiter.scala 50:24]
        io_out_regData_5 <= io_out_regData_3; // @[Arbiter.scala 51:19]
      end
    end else if (2'h1 == io_out_regState_5) begin // @[Arbiter.scala 48:22]
      if (io_out_out_4_valid) begin // @[Arbiter.scala 58:24]
        io_out_regData_5 <= io_out_regData_4; // @[Arbiter.scala 59:19]
      end
    end
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  io_out_regData = _RAND_0[7:0];
  _RAND_1 = {1{`RANDOM}};
  io_out_regState = _RAND_1[1:0];
  _RAND_2 = {1{`RANDOM}};
  io_out_regState_3 = _RAND_2[1:0];
  _RAND_3 = {1{`RANDOM}};
  io_out_regData_1 = _RAND_3[7:0];
  _RAND_4 = {1{`RANDOM}};
  io_out_regState_1 = _RAND_4[1:0];
  _RAND_5 = {1{`RANDOM}};
  io_out_regState_4 = _RAND_5[1:0];
  _RAND_6 = {1{`RANDOM}};
  io_out_regData_2 = _RAND_6[7:0];
  _RAND_7 = {1{`RANDOM}};
  io_out_regState_2 = _RAND_7[1:0];
  _RAND_8 = {1{`RANDOM}};
  io_out_regData_3 = _RAND_8[7:0];
  _RAND_9 = {1{`RANDOM}};
  io_out_regState_5 = _RAND_9[1:0];
  _RAND_10 = {1{`RANDOM}};
  io_out_regData_4 = _RAND_10[7:0];
  _RAND_11 = {1{`RANDOM}};
  io_out_regData_5 = _RAND_11[7:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
