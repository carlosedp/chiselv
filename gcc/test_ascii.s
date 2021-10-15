.global _boot
.text
# Loop from 33 to 126 (visible chars in ascii table) outputting the value to the memory region 0x3000_0000
_boot:                           /* x0  = 0    0x000         */
        addi x1, x0, 40                /* x1 = 40                  */
        addi x2, x0, 33                /* x2 = 33                  */
        addi x3, x0, 126               /* x3 = 126                 */
        lui  x4,     %hi(0x30000000)   /* x4 = 0x3000_0000         */
  loop: sb   x2,     0(x4)             /* [0x3000_0000] = x2 = 33  */
        addi x2, x2, 1                 /* x2 = 1                   */
        bge  x3, x2, loop              /* if x3 > x2 -> pc = pc-8 (back 2 instructions)*/
        jal  x0,     0                 /* loop forever             */
