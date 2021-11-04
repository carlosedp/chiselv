#pragma once

typedef unsigned int uint32_t;
typedef int int32_t;
typedef unsigned char uint8_t;
typedef char int8_t;

int main();

// GPIO Direction
void pinMode(unsigned char port, unsigned char val);
void setDirection(unsigned int val);
uint32_t readDirection();

// GPIO Value
void setGPIO(uint32_t val);
uint32_t readGPIO();
void digitalWrite(uint8_t port, uint8_t val);

// Timer
void setTimer(unsigned int val);
unsigned int getTimer();
void resetTimer();
void sleep(unsigned int ms);