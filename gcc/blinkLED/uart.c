#include "uart.h"
/*
 * Core UART functions to implement for a port
 */

uint32_t uart_reg_read(int offset)
{
	uint32_t addr;
	uint32_t val;
	addr = UART0_BASE + offset;
	val = *(volatile uint32_t *)addr;
	return val;
}

void uart_reg_write(int offset, uint32_t val)
{
	uint32_t addr;
	addr = UART0_BASE + offset;
	*(volatile uint32_t *)addr = val;
}

int uart_rx_empty(void)
{
	uint32_t val;
	val = uart_reg_read(UART_STATUS);
	if (val & UART_STATUS_RX_EMPTY)
		return 1;
	return 0;
}

int uart_tx_full(void)
{
	uint32_t val;
	val = uart_reg_read(UART_STATUS);
	if (val & UART_STATUS_TX_FULL)
		return 1;
	return 0;
}

char uart_read(void)
{
	uint32_t val;
	val = uart_reg_read(UART_RX);
	return (char)(val & 0x000000ff);
}

void uart_write(char c)
{
	uint32_t val;
	val = c;
	uart_reg_write(UART_TX, val);
}

unsigned long uart_divisor(unsigned long proc_freq, unsigned long uart_freq)
{
	return proc_freq / (uart_freq * 16) - 1;
}

void uart_init(void)
{
	uint32_t proc_freq;
	proc_freq = *(volatile unsigned long *)(SYSCON_BASE + SYS_REG_CLKINFO);
	uart_reg_write(UART_CLOCKDIV, uart_divisor(proc_freq, UART0_BAUD));
}

int getchar(void)
{
	while (uart_rx_empty())
		; // Do nothing
	return uart_read();
}

int putchar(unsigned char c)
{
	if (c == '\n')
	{
		while (uart_tx_full())
			; // Do nothing
		uart_write('\r');
	}
	while (uart_tx_full())
		; // Do nothing
	uart_write(c);

	return 0;
}
