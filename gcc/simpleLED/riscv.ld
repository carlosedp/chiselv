/* Thanks https://github.com/darklife/darkriscv */
/* and https://github.com/racerxdl/riskow */

__heap_size     = 0x2000;    /* amount of heap  */
__stack_size    = 0x8000;    /* amount of stack */

MEMORY
{
    ROM         (rwx) : ORIGIN = 0x00000000, LENGTH = 0x10000
    RAM         (rwx) : ORIGIN = 0x80000000, LENGTH = 0x10000
    IO          (rw)  : ORIGIN = 0x30000000, LENGTH = 0x00010
}
SECTIONS
{
    .text :
    {
        *(.boot)
        *(.text)
        *(.text)
    } > ROM
    .data :
    {
        *(.rodata*)
        *(.sbss)
        *(.data)
        *(.bss)
        *(.rela*)
        *(COMMON)
    } > RAM

    .heap :
    {
        . = ALIGN(4);
        PROVIDE ( end = . );
        _sheap = .;
        . = . + __heap_size;
        . = ALIGN(4);
        _eheap = .;
    } >RAM


    .stack :
    {
        . = ALIGN(4);
        _estack = .;
        . = . + __stack_size;
        . = ALIGN(4);
        _sstack = .;
    } >RAM

}