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

		uart_tx(top->io_UART0tx);
		top->io_UART0rx = uart_rx();

		if (top->io_terminate)
		{
			VL_PRINTF("Simulation terminated!\n");
			
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 0, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_0);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 1, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_1);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 2, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_2);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 3, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_3);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 4, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_4);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 5, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_5);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 6, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_6);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 7, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_7);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 8, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_8);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 9, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_9);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 10, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_10);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 11, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_11);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 12, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_12);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 13, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_13);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 14, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_14);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 15, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_15);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 16, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_16);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 17, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_17);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 18, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_18);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 19, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_19);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 20, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_20);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 21, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_21);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 22, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_22);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 23, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_23);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 24, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_24);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 25, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_25);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 26, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_26);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 27, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_27);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 28, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_28);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 29, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_29);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 30, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_30);
			VL_PRINTF("REG%d %x" VL_PRI64 "X\r\n", 31, top->Toplevel__DOT__CPU__DOT__registerBank__DOT__regs_31);

			VL_PRINTF("PC %x" VL_PRI64 "X\r\n",
					  top->Toplevel__DOT__CPU__DOT__PC__DOT__pc);
			// VL_PRINTF("CTR %016" VL_PRI64 "X\r\n",
			// 	  top->Core__DOT__countRegister);

			// VL_PRINTF("CR 00000000%01X%01X%01X%01X%01X%01X%01X%01X\r\n",
			// 	top->Core__DOT__conditionRegister_0,
			// 	top->Core__DOT__conditionRegister_1,
			// 	top->Core__DOT__conditionRegister_2,
			// 	top->Core__DOT__conditionRegister_3,
			// 	top->Core__DOT__conditionRegister_4,
			// 	top->Core__DOT__conditionRegister_5,
			// 	top->Core__DOT__conditionRegister_6,
			// 	top->Core__DOT__conditionRegister_7);

			/*
			 * We run for one more tick to allow any debug
			 * prints in this cycle to make it out.
			 */
			tick(top);
#if VM_TRACE
			tfp->close();
#endif
			exit(1);
		}
	}

#if VM_TRACE
	tfp->close();
	delete tfp;
#endif

	delete top;
}
