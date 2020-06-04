module alu
  (input logic [1:0] func,
   input logic [31:0] a, b,
   output logic [31:0] result
  );

// The following has en error
// func is two bits, so it is latching on 2 or 3
// But ModelSim is NOT giving a warning!
// Also VCS is NOT giving a warning or error!
always_comb begin
  case (func)
    2'b0 : result = a + b;
    2'b1 : result = a - b;
  endcase
end

endmodule
