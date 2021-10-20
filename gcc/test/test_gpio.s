# Test GPIO
# This writes to GPIO 8 pins as a binary add
#
# Config for the http://tice.sea.eseo.fr/riscv simulator
# GPIO_BASE = 0xd0000000
# GPIO_VALUE_OFFSET = 0x10
# GPIO_DIR_OFFSET =  0x00
# ENABLE_OUTPUT = 0
#
# Config for ChiselV
GPIO_BASE =  0x30001000
GPIO_DIR_OFFSET =  0x00
GPIO_VALUE_OFFSET =  0x04
ENABLE_OUTPUT =  -1
TIMER_BASE =  0x30003000

main:  lui   x1, %hi(GPIO_BASE)         # Define the GPIO0 base address
       lui   x6, %hi(TIMER_BASE)        # Define the Timer0 base address
       addi  x5, x0, ENABLE_OUTPUT      # Set x5 to 0xFFFFFFFF to use as GPIO direction
       addi  x3, x0, 255                # Set x3 as the highest value (using 8 bits to match 8 GPIO outputs)
       addi  x7, x0, 1000               # Set wait timer for 1s (1000ms)
       sw    x5, GPIO_DIR_OFFSET(x1)    # Write the GPIO direction to 0x30001000
loop:  addi  x2, x2, 1                  # Increment x2 by 1 (from x3)
       sw    x2, GPIO_VALUE_OFFSET(x1)  # Write the GPIO value to 0x30001000
       beq   x2, x3, reset              # Loop until x2 is equal to x4 (from 1 to 255)
wait:  lw    x8, 0(x6)                  # Check timer value
       bne   x7, x8, wait               # Unless timer value is equal to 1000ms, keep waiting
       sw    x0, 0(x6)                  # Reset timer
       jal x0, loop                     # Go to GPIO Loop
reset: addi  x2, x0, 0                  # Reset x2 to 0 (GPIO Count)
       jal x0, loop                     # Go to GPIO Loop
