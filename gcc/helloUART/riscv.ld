/* Thanks https://github.com/darklife/darkriscv */
/* and https://github.com/racerxdl/riskow */

__heap_size     = 0x2000;    /* amount of heap  */
__stack_size    = 0x8000;    /* amount of stack */

MEMORY
{
    ROM         (rwx) : ORIGIN = 0x00000000, LENGTH = 0x10000
    RAM         (rwx) : ORIGIN = 0x80000000, LENGTH = 0x10000
}
SECTIONS
{
    .text :
    {
        *(.boot)
        *(.text)
    } > ROM
    .data :
    {
        *(.rodata*)
        *(.*data*)
        *(.sbss)
        *(.bss)
        *(.rela*)
        *(COMMON)
        _heap = .;
    } > RAM

    PROVIDE ( _sstack = ORIGIN(RAM) + LENGTH(RAM) );
}