
module test;
  logic [31:0] a, b, res;
  logic [1:0] fun;
  logic clock = 0;

  alu dut (.func(fun), .a(a), .b(b), .result(res));

  always #10 clock = ~clock;

  initial begin
    @(negedge clock)
    a = 2;
    b = 3;
    fun = 0;
    @(posedge clock)
    $display("result: %d %d %d", a, b, res);
    @(negedge clock)
    a = 5;
    @(posedge clock)
    $display("result: %d %d %d", a, b, res);
    @(negedge clock)
    fun = 3;
    a = 1;
    @(posedge clock)
    $display("result: %d %d %d", a, b, res);
    $finish;
  end
endmodule
