`timescale 1ps/1ps

module PLL0

 (
  output        clko,
  output        lock,
  input         clki
 );

   always @* begin
    lock <= 1;
    clko <= clki;
  end

endmodule
