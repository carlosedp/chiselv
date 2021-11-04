#include "main.h"

#define GPIO0_BASE 0x30001000
#define GPIO0_DIR 0x00
#define GPIO0_VAL 0x04
#define TIMER0_BASE 0x30003000

#define HIGH 1
#define LOW  0

#define INPUT 1
#define OUTPUT 0

int main() {
  dir();

  // Turn on LED1 (GPIO0)
    uint32_t addrval;
    addrval = GPIO0_BASE + GPIO0_VAL;
    *(volatile uint32_t *)addrval = 3;
}

void dir() {
    // Set GPIO0 Direction (IIOOOOOO)
    uint32_t addrdir;
    addrdir = GPIO0_BASE + GPIO0_DIR;
    *(volatile uint32_t *)addrdir = 0x3F;
}
