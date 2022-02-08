SOURCES       := $(shell find . -name '*.s')
OBJECTS       := $(SOURCES:%.s=%.o)
TESTDATA      := $(SOURCES:%.s=%.elf)
HEXDATA       := $(SOURCES:%.s=%.hex)
MEMDATA       := $(SOURCES:%.s=%.mem)

DOCKERARGS = run --rm -v $(PWD):/src -w /src
DOCKERIMG  = docker $(DOCKERARGS) carlosedp/crossbuild-riscv64

CFLAGS=-mabi=ilp32 -march=rv32i
LDFLAGS=-T riscv.ld -m elf32lriscv -O binary

PREFIX=riscv64-linux
OC=$(DOCKERIMG) $(PREFIX)-objcopy
OD=$(DOCKERIMG) $(PREFIX)-objdump
CC=$(DOCKERIMG) $(PREFIX)-gcc
LD=$(DOCKERIMG) $(PREFIX)-ld
HD=$(DOCKERIMG) hexdump

all: $(TESTDATA) $(MEMDATA) $(HEXDATA)

%.elf: %.s
	@echo "Building $< -> $@"
	@$(CC) -c $(CFLAGS) -o $(@:%.elf=%.o) $<
	@$(LD) $(LDFLAGS) $(@:%.elf=%.o) -o $@

%.hex: %.elf
	@echo "Building $< -> $@"
	@$(OC) -O ihex $(@:%.hex=%.elf) $@ --only-section .text\*

%.mem: %.elf  ## Readmemh 32bit memory file
	@echo "Building $< -> $@"
	@$(OC) -O binary $(@:%.mem=%.elf) $(@:%.mem=%.bin) --only-section .text\*
	@hexdump -ve '1/4 "%08x\n"' $(@:%.mem=%.bin) > $@

simhex: $(HEXDATA)
	@echo "Building prog.hex for http://tice.sea.eseo.fr/riscv/"

testmem: $(MEMDATA)
	@echo "Building memdata for unit tests"

clean:
	@echo "Cleaning build files"
	@rm -f $(OBJECTS) *.elf *.hex *.mem *.bin