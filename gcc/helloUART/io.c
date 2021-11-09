#include "io.h"

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

void setDirection(unsigned int val)
{
  uint32_t addr;
  addr = GPIO0_BASE + GPIO0_DIR;
  *(volatile uint32_t *)addr = val;
}

uint32_t readDirection()
{
  uint32_t addr;
  uint32_t val;
  addr = GPIO0_BASE + GPIO0_DIR;
  val = *(volatile uint32_t *)addr;
  return val;
}

uint32_t digitalRead(uint32_t port)
{
  uint32_t addr;
  uint32_t val;
  addr = GPIO0_BASE + GPIO0_VAL;
  val = *(volatile uint32_t *)addr;
  return val & (1 << port);
}

void setGPIO(uint32_t val)
{
  uint32_t addr;
  addr = GPIO0_BASE + GPIO0_VAL;
  *(volatile uint32_t *)addr = val;
}

uint32_t readGPIO()
{
  uint32_t addr;
  uint32_t val;
  addr = GPIO0_BASE + GPIO0_VAL;
  val = *(volatile uint32_t *)addr;
  return val;
}

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

void setTimer(unsigned int val)
{
  uint32_t addr;
  addr = TIMER0_BASE;
  *(volatile uint32_t *)addr = val;
}

unsigned int getTimer()
{
  uint32_t addr;
  uint32_t val;
  addr = TIMER0_BASE;
  val = *(volatile uint32_t *)addr;
  return val;
}

void resetTimer()
{
  setTimer(0);
}

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
