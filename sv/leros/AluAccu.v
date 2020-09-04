module AluAccu(
  input         clock,
  input         reset,
  input  [2:0]  io_op,
  input  [31:0] io_din,
  input         io_ena,
  output [31:0] io_accu
);
  reg [31:0] a; // @[AluAccu.scala 25:24]
  reg [31:0] _RAND_0;
  wire  _T; // @[Conditional.scala 37:30]
  wire  _T_1; // @[Conditional.scala 37:30]
  wire [31:0] _T_3; // @[AluAccu.scala 37:16]
  wire  _T_4; // @[Conditional.scala 37:30]
  wire [31:0] _T_6; // @[AluAccu.scala 40:16]
  wire  _T_7; // @[Conditional.scala 37:30]
  wire [31:0] _T_8; // @[AluAccu.scala 43:16]
  wire  _T_9; // @[Conditional.scala 37:30]
  wire [31:0] _T_10; // @[AluAccu.scala 46:16]
  wire  _T_11; // @[Conditional.scala 37:30]
  wire [31:0] _T_12; // @[AluAccu.scala 49:16]
  wire  _T_13; // @[Conditional.scala 37:30]
  wire  _T_14; // @[Conditional.scala 37:30]
  wire [30:0] _T_15; // @[AluAccu.scala 55:16]
  assign _T = 3'h0 == io_op; // @[Conditional.scala 37:30]
  assign _T_1 = 3'h1 == io_op; // @[Conditional.scala 37:30]
  assign _T_3 = a + io_din; // @[AluAccu.scala 37:16]
  assign _T_4 = 3'h2 == io_op; // @[Conditional.scala 37:30]
  assign _T_6 = a - io_din; // @[AluAccu.scala 40:16]
  assign _T_7 = 3'h3 == io_op; // @[Conditional.scala 37:30]
  assign _T_8 = a & io_din; // @[AluAccu.scala 43:16]
  assign _T_9 = 3'h4 == io_op; // @[Conditional.scala 37:30]
  assign _T_10 = a | io_din; // @[AluAccu.scala 46:16]
  assign _T_11 = 3'h5 == io_op; // @[Conditional.scala 37:30]
  assign _T_12 = a ^ io_din; // @[AluAccu.scala 49:16]
  assign _T_13 = 3'h6 == io_op; // @[Conditional.scala 37:30]
  assign _T_14 = 3'h7 == io_op; // @[Conditional.scala 37:30]
  assign _T_15 = a[31:1]; // @[AluAccu.scala 55:16]
  assign io_accu = a; // @[AluAccu.scala 64:11]
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
  a = _RAND_0[31:0];
  `endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end
  always @(posedge clock) begin
    if (reset) begin
      a <= 32'h0;
    end else begin
      if (io_ena) begin
        if (!(_T)) begin
          if (_T_1) begin
            a <= _T_3;
          end else begin
            if (_T_4) begin
              a <= _T_6;
            end else begin
              if (_T_7) begin
                a <= _T_8;
              end else begin
                if (_T_9) begin
                  a <= _T_10;
                end else begin
                  if (_T_11) begin
                    a <= _T_12;
                  end else begin
                    if (_T_13) begin
                      a <= io_din;
                    end else begin
                      if (_T_14) begin
                        a <= {{1'd0}, _T_15};
                      end
                    end
                  end
                end
              end
            end
          end
        end
      end
    end
  end
endmodule
