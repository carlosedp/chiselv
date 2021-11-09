#include "io.h"
#include "uart.h"
#include "stdio.h"
#include "banner.h"

#define CLEAR "\033[2K"

volatile int heapcheck = 0xdeadbeef;
int main(void)
{
  uart_init();
  pinMode(0, OUTPUT);
  // Turn on LED to say we're alive
  digitalWrite(0, HIGH);

  printf(logocv);
  printf("> ");
  while (1)
  {
    unsigned char c = getchar();
    if (c == '\r')
    {
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
