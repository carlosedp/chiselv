#include <stdlib.h>
#include "VToplevel.h"
#include "verilated.h"
#include "verilated_vcd_c.h"

/*
 * Current simulation time
 * This is a 64-bit integer to reduce wrap over issues and
 * allow modulus.  You can also use a double, if you wish.
 */
vluint64_t main_time = 0;

/*
 * Called by $time in Verilog
 * converts to double, to match
 * what SystemC does
 */
double sc_time_stamp(void)
{
	return main_time;
}

#if VM_TRACE
VerilatedVcdC *tfp;
#endif

void tick(VToplevel *top)
{
	top->clock = 1;
	top->eval();
#if VM_TRACE
	if (tfp)
		tfp->dump((double)main_time);
#endif
	main_time++;

	top->clock = 0;
	top->eval();
#if VM_TRACE
	if (tfp)
		tfp->dump((double)main_time);
#endif
	main_time++;
}

void uart_tx(unsigned char tx);
unsigned char uart_rx(void);

int main(int argc, char **argv)
{
	Verilated::commandArgs(argc, argv);

	// init top verilog instance
	VToplevel *top = new VToplevel;

#if VM_TRACE
	// init trace dump
	Verilated::traceEverOn(true);
	tfp = new VerilatedVcdC;
	top->trace(tfp, 99);
	tfp->open("ChiselV.vcd");
#endif

	// Reset
	top->reset = 1;
	for (unsigned long i = 0; i < 5; i++)
		tick(top);
	top->reset = 0;

	while (!Verilated::gotFinish())
	{
		tick(top);
		// VL_PRINTF("GPIO  %" VL_PRI64 "x\r\n", top->Toplevel__DOT__CPU__DOT__GPIO0__DOT__GPIO);

		uart_tx(top->io_UART0_tx);
		top->io_UART0_rx = uart_rx();
	}

#if VM_TRACE
	tfp->close();
	delete tfp;
#endif

	delete top;
}
