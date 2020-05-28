module alu
  (input logic [1:0] func,
   input logic [31:0] a, b,
   output logic [31:0] result
  );

always_comb begin
  case (func)
    2'b0 : result = a + b;
    2'b1 : result = a - b;
  endcase
end

endmodule
