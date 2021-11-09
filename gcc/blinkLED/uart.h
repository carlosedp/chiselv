#pragma once

/*
 * Core UART header to implement for a port
 */

typedef unsigned int uint32_t;
typedef int int32_t;
typedef unsigned char uint8_t;
typedef char int8_t;

#define SYSCON_BASE 0x00001000 /* System control regs */
#define SYS_REG_CLKINFO 0x08 /* Clock information */

#define UART0_BASE 0x30000000
#define UART0_BAUD 115200

#define UART_TX                 0x00
#define UART_RX                 0x04
#define UART_STATUS             0x0C
#define UART_STATUS_RX_EMPTY    0x01
#define UART_STATUS_TX_EMPTY    0x02
#define UART_STATUS_RX_FULL     0x04
#define UART_STATUS_TX_FULL     0x08
#define UART_CLOCKDIV           0x10

uint32_t uart_reg_read(int offset);
void uart_reg_write(int offset, uint32_t val);
int uart_rx_empty(void);
int uart_tx_full(void);
char uart_read(void);
void uart_write(char c);
unsigned long uart_divisor(unsigned long proc_freq, unsigned long uart_freq);
void uart_init(void);
int getchar(void);
int putchar(unsigned char c);
// void putstr(const char *str, unsigned long len);
// void puts(const char *str);
// int8_t strlen(const char *s);