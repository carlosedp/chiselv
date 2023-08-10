#include "io.h"

//-- User facing functions --//

// Sets the pin mode (INPUT or OUTPUT)
void pinMode(unsigned char port, unsigned char val)
{
  uint32_t dir = readDirection();
  uint32_t b = 1 << port;

  if (val)
  {
    dir |= b;
  }
  else
  {
    dir &= ~b;
  }

  setDirection(dir);
}

// Reads the value of a pin set as INPUT (LOW or HIGH)
uint32_t digitalRead(uint32_t port)
{
  uint32_t val;
  val = readGPIO();
  if (val & (1 << port))
  {
    return 1;
  }
  else
  {
    return 0;
  }
}

// Writes a HIGH or a LOW value to a pin set as OUTPUT
void digitalWrite(uint8_t port, uint8_t val)
{
  uint32_t vals = readGPIO();
  uint32_t b = 1 << port;

  if (val)
  {
    vals |= b;
  }
  else
  {
    vals &= ~b;
  }

  setGPIO(vals);
}

// Reads the value of the timer (in microseconds)
unsigned int getTimer()
{
  uint32_t addr;
  uint32_t val;
  addr = TIMER0_BASE;
  val = *(volatile uint32_t *)addr;
  return val;
}

// Set the timer to 0
void resetTimer()
{
  setTimer(0);
}

// Sleeps for the specified number of milliseconds (blocking)
void sleep(unsigned int ms)
{
  resetTimer();
  unsigned int startTime = getTimer();
  unsigned int countedMs = 0;
  while (getTimer() <= startTime + ms)
  {
    countedMs++;
  }
  resetTimer();
}

//-- Internal functions --//

// Set the GPIO direction (for all ports)
void setDirection(unsigned int val)
{
  uint32_t addr;
  addr = GPIO0_BASE + GPIO0_DIR;
  *(volatile uint32_t *)addr = val;
}

// Reads the GPIO direction (for all ports)
uint32_t readDirection()
{
  uint32_t addr;
  uint32_t val;
  addr = GPIO0_BASE + GPIO0_DIR;
  val = *(volatile uint32_t *)addr;
  return val;
}

// Set the GPIO value (for all ports)
void setGPIO(uint32_t val)
{
  uint32_t addr;
  addr = GPIO0_BASE + GPIO0_VAL;
  *(volatile uint32_t *)addr = val;
}

// Reads the GPIO value (for all ports)
uint32_t readGPIO()
{
  uint32_t addr;
  uint32_t val;
  addr = GPIO0_BASE + GPIO0_VAL;
  val = *(volatile uint32_t *)addr;
  return val;
}

// Sets the timer value
void setTimer(unsigned int val)
{
  uint32_t addr;
  addr = TIMER0_BASE;
  *(volatile uint32_t *)addr = val;
}