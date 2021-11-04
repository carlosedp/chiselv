SOURCES       := $(shell find . -name '*.c')
ASM_SOURCES   := $(shell find . -name '*.s')
OBJECTS       := $(SOURCES:%.c=%.o)
ASM_OBJECTS   := $(ASM_SOURCES:%.s=%.s.o)
PROGDATA      := $(SOURCES:%.c=%.elf)
DUMPDATA      := $(SOURCES:%.c=%.dump)
HEXDATA       := $(SOURCES:%.c=%.hex)
MEMDATA       := $(SOURCES:%.c=%.mem)
ASM           := $(SOURCES:%.c=%.s)

DOCKERARGS = run --rm -v $(PWD):/src -w /src
DOCKERIMG  = docker $(DOCKERARGS) carlosedp/crossbuild-riscv64

CFLAGS=-mabi=ilp32 -march=rv32i -Os
LDFLAGS=-T riscv.ld -m elf32lriscv -O binary -Map=main.map

# OC=riscv64-linux-gnu-objcopy
# CC=riscv64-linux-gnu-gcc-10
# LD=riscv64-linux-gnu-ld
# HD=hexdump
OC=$(DOCKERIMG) riscv64-linux-objcopy
OD=$(DOCKERIMG) riscv64-linux-objdump
CC=$(DOCKERIMG) riscv64-linux-gcc
LD=$(DOCKERIMG) riscv64-linux-ld
HD=$(DOCKERIMG) hexdump

all: clean $(PROGDATA) $(MEMDATA) $(HEXDATA) $(DUMPDATA)
asm: $(ASM)

%.o: %.c
	@echo "Building $< -> $@"
	@$(CC) -c $(CFLAGS) -o $@ $<

%.s.o: %.s
	@echo "Building $< -> $@"
	@$(CC) -c $(CFLAGS) -o $@ $<

%.elf: $(OBJECTS) $(ASM_OBJECTS)
	@echo "Linking $< $(OBJECTS) $(ASM_OBJECTS)"
	@$(LD) $(LDFLAGS) $(OBJECTS) $(ASM_OBJECTS) -o $@

%.dump: %.elf
	@echo "Dumping to $@"
	@$(OD) -d -t -r $< > $@

%.s: %.c
	@echo "Building $< -> $@"
	@$(CC) -S $(CFLAGS) -o $@ $<

%.hex: %.elf
	@echo "Building $@ for http://tice.sea.eseo.fr/riscv/"
	@echo "Building $< -> $@"
	@$(OC) -O ihex $(@:%.hex=%.elf) $@ --only-section .text\*

%.mem: %.elf  ## Readmemh 32bit memory file
	@echo "Building $< -> $@"
	@$(OC) -O binary $(@:%.mem=%.elf) $(@:%.mem=%.bin) --only-section .text\*
	@$(HD) -ve '1/4 "%08x\n"' $(@:%.mem=%.bin) > $@

clean:
	@echo "Cleaning build files"
	rm -f $(ASM) $(OBJECTS) $(ASM_OBJECTS) *.elf *.hex *.bin *.mem *.s.o *.map *.dump