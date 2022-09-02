module PLL0

// This is just a bypass for the PLL0.
// Input clock is 50Mhz

 (
  input         clki,
  output reg    clko,
  output reg    lock
 );

   always @* begin
    lock = 1;
    clko = clki;
  end

endmodule
