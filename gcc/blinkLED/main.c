#include "main.h"

#define GPIO0_BASE 0x30001000
#define GPIO0_DIR 0x00
#define GPIO0_VAL 0x04
#define TIMER0_BASE 0x30003000

#define HIGH 1
#define LOW 0

#define INPUT 0
#define OUTPUT 1

int main()
{
  // Set GPIO0 Direction [IIOOOOOO] right to left
  //           Pin Number[76543210]
  pinMode(0, OUTPUT);
  pinMode(1, OUTPUT);
  pinMode(2, OUTPUT);
  pinMode(3, OUTPUT);
  pinMode(4, OUTPUT);
  pinMode(5, OUTPUT);
  pinMode(6, INPUT);
  pinMode(7, INPUT);

  // Turn on LEDs
  digitalWrite(0, HIGH);
  digitalWrite(1, HIGH);
  digitalWrite(2, HIGH);
  digitalWrite(3, HIGH);
  digitalWrite(4, HIGH);

  // Turn off LED 2 after 1/2 second
  sleep(500);
  digitalWrite(2, LOW);
  sleep(500);

  // Loop blinking LED 5
  while(1) {
    for (int i = 0; i < 10; i++)
    {
      digitalWrite(5, HIGH);
      sleep(200);
      digitalWrite(5, LOW);
      sleep(200);
    }
    sleep(1000);
  }
}

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
  return val && (1 << port);
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
