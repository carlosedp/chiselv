`timescale 1ps/1ps

module PLL0

 (
  output reg    clko,
  output reg    lock,
  input         clki
 );

   always @* begin
    lock = 1;
    clko = clki;
  end

endmodule
