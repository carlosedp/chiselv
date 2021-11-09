#pragma once

typedef unsigned int uint32_t;
typedef int int32_t;
typedef unsigned char uint8_t;
typedef char int8_t;

#define GPIO0_BASE 0x30001000
#define GPIO0_DIR 0x00
#define GPIO0_VAL 0x04
#define TIMER0_BASE 0x30003000

#define HIGH 1
#define LOW 0

#define INPUT 0
#define OUTPUT 1

// GPIO Direction
void pinMode(unsigned char port, unsigned char val);
void setDirection(unsigned int val);
uint32_t readDirection();

// GPIO Value
void setGPIO(uint32_t val);
uint32_t readGPIO();
void digitalWrite(uint8_t port, uint8_t val);
uint32_t digitalRead(uint32_t port);

// Timer
void setTimer(unsigned int val);
unsigned int getTimer();
void resetTimer();
void sleep(unsigned int ms);