#include "io.h"

int main()
{
  // Set GPIO0 Direction [IOOOOOOO] right to left
  //           Pin Number[76543210]
  pinMode(0, OUTPUT);
  pinMode(1, OUTPUT);
  pinMode(2, OUTPUT);
  pinMode(3, OUTPUT);
  pinMode(4, OUTPUT);
  pinMode(5, OUTPUT);
  pinMode(6, OUTPUT);
  pinMode(7, INPUT);

  // Turn on LEDs 0 and 1
  digitalWrite(0, HIGH);
  digitalWrite(1, HIGH);

  // Turn off LED 1 after 1/2 second
  sleep(500);
  digitalWrite(1, LOW);
  sleep(500);

  while (1)
  {
    // Light LED 3 if button is pressed before LED2 starts blinking
    digitalWrite(3, digitalRead(7));

    // Loop blinking LED 2
    for (int i = 0; i < 10; i++)
    {

      digitalWrite(2, HIGH);
      sleep(200);
      digitalWrite(2, LOW);
      sleep(200);
    }
    sleep(1000);
  }
}
