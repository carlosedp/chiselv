#include "../lib/io.h"
#include "../lib/uart.h"
#include "../lib/stdio.h"
#include "banner.h"

#define CLEAR "\033[2K"

volatile int heapcheck = 0xdeadbeef;

void header(void) {
  printf("ChiselV, a RISC-V RV32I Core\n");
  uint32_t proc_freq = *(volatile unsigned long *)(SYSCON_BASE + SYS_REG_CLKINFO) / 1000 / 1000;
  uint32_t num_gpio = *(volatile unsigned long *)(SYSCON_BASE + SYS_REG_NUMGPIO0);
  uint32_t bootaddr = *(volatile unsigned long *)(SYSCON_BASE + SYS_REG_BOOTADDR);
  uint32_t romsize = *(volatile unsigned long *)(SYSCON_BASE + SYS_REG_ROMSIZE);
  uint32_t ramsize = *(volatile unsigned long *)(SYSCON_BASE + SYS_REG_RAMSIZE);
  printf("clock: %dMHz\n", proc_freq);
  printf("bootaddr: %d\n", bootaddr);
  printf("ROM: %d bytes\n", romsize);
  printf("RAM: %d bytes\n", ramsize);
  printf("uart0: %d\n", UART0_BAUD);
  printf("gpio0: %d IO\n", num_gpio);
  printf("-----\n");
}

int main(void)
{
  uart_init();
  // Turn on LED to say we're alive
  pinMode(0, OUTPUT);
  digitalWrite(0, HIGH);

  // Print the header with core info
  header();
  // Print the banner
  printf(logocv);
  printf("This demo prints back to the console all typed characters when hit <enter>.\n");
  printf("> ");
  char data[128];
  while (1)
  {
    unsigned char c = getchar();
    int bufLen = strlen(data);
    data[bufLen] = c;
    if (c == '\r')
    {
      printf("\r\n");
      printf("You typed: %s", data);
      // Clear the buffer
      memset(data, 0, sizeof(data));
      printf("\r\n> ");
    }
    else
    {
      putchar(c);
    }

    if (heapcheck != 0xdeadbeef)
    {
      printf("out of memory detected, a reboot is recommended...\n");
    }
  }
  return 0;
}
