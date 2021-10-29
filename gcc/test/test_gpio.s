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
GPIO_DIRECTION =  0xCF       # Set GPIO Inputs and Outputs (OOIIOOOO)
TIMER_BASE =  0x30003000
MEM_BASE =  0x80000000

main:  lui   x1, %hi(GPIO_BASE)         # Define the GPIO0 base address
       lui   x6, %hi(TIMER_BASE)        # Define the Timer0 base address
       addi  x5, x0, GPIO_DIRECTION     # Set x5 to use as GPIO direction
       lui   x15, %hi(MEM_BASE)         # Store base RAM address
       sw    x5, 40(x15)                # Store diretion to RAM (for testing)
       lw    x16, 40(x15)               # Load direction form RAM (for testing)
       addi  x3, x0, 16                 # Set x3 as the highest value (using 4 bits to match 8 used GPIO outputs)
       addi  x7, x0, 1000               # Set wait timer for 1s (1000ms)
       addi  x10, x0, 0x10              # Set x10 to check if button is pressed
       sw    x16, GPIO_DIR_OFFSET(x1)   # Write the GPIO direction to address 0x30001000
loop:  addi  x2, x2, 1                  # Increment x2 by 1
       lw    x9, GPIO_VALUE_OFFSET(x1)  # Read the GPIO value 0x30001004 to x9
       and   x11, x9, x10               # Check if button is pressed
       bne   x10, x11 ,write            # If button is not pressed, do not switch LED on
       ori   x2, x2, 0x40               # Add x9 to x2
write: sw    x2, GPIO_VALUE_OFFSET(x1)  # Write the GPIO value to address 0x30001004
       beq   x2, x3, reset              # Loop until x2 is equal to x4 (from 1 to 255)
wait:  lw    x8, 0(x6)                  # Check timer value
       bne   x7, x8, wait               # Unless timer value is equal to 1000ms, keep waiting
       sw    x0, 0(x6)                  # Reset timer
       jal x0, loop                     # Go to GPIO Loop
reset: addi  x2, x0, 0                  # Reset x2 to 0 (GPIO Count)
       jal x0, loop                     # Go to GPIO Loop
