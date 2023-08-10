SOURCES       := $(shell find . ../lib -name '*.c')
ASM_SOURCES   := $(shell find . ../lib -name '*.s')
OBJECTS       := $(SOURCES:%.c=%.o)
ASM_OBJECTS   := $(ASM_SOURCES:%.s=%.s.o)
ASM           := $(SOURCES:%.c=%.s)

USEDOCKER = 1
DOCKERARGS = run --rm -v $(PWD)/..:/src -w /src/$(shell basename $(CURDIR))
DOCKERIMG  = docker $(DOCKERARGS) docker.io/carlosedp/crossbuild-riscv64:latest

CFLAGS=-Wall -mabi=ilp32 -march=rv32i -ffreestanding -fcommon -Os
LDFLAGS=-T ../lib/riscv.ld -m elf32lriscv -O binary -Map=main.map

PREFIX=riscv64-linux

ifeq ($(USEDOCKER), 1)
	OC=$(DOCKERIMG) $(PREFIX)-objcopy
	OD=$(DOCKERIMG) $(PREFIX)-objdump
	CC=$(DOCKERIMG) $(PREFIX)-gcc
	LD=$(DOCKERIMG) $(PREFIX)-ld
	HD=$(DOCKERIMG) hexdump
else
	OC=$(PREFIX)-objcopy
	OD=$(PREFIX)-objdump
	CC=$(PREFIX)-gcc
	LD=$(PREFIX)-ld
	HD=hexdump
endif

all: main.elf main-rom.mem main-ram.mem main.hex main.dump
asm: $(ASM)

%.o: %.c
	@echo "Building $< -> $@"
	@$(CC) -c $(CFLAGS) -o $@ $<

%.s.o: %.s
	@echo "Building $< -> $@"
	@$(CC) -c $(CFLAGS) -o $@ $<

main.elf: $(OBJECTS) $(ASM_OBJECTS)
	@echo "Linking $< $(OBJECTS) $(ASM_OBJECTS)"
	@$(LD) $(LDFLAGS) $(OBJECTS) $(ASM_OBJECTS) -o main.elf

main.dump: main.elf
	@echo "Dumping to $@"
	@$(OD) -d -t -r $< > $@

main.hex: main.elf
	@echo "Building $@ for http://tice.sea.eseo.fr/riscv/"
	@echo "Building $< -> $@"
	@$(OC) -O ihex $< $@ --only-section .text\*

main-rom.mem: main.elf  ## Readmemh 32bit memory file
	@echo "Building $< -> $@"
	@$(OC) -O binary $< $(@:%-rom.mem=%.bin) --only-section .text*
	@$(HD) -ve '1/4 "%08x\n"' $(@:%-rom.mem=%.bin) > $@

main-ram.mem: main.elf  ## Readmemh 32bit memory file
	@echo "Building $< -> $@"
	@$(OC) -O binary $< $(@:%-ram.mem=%.bin) --only-section .*data*
	@$(HD) -ve '1/4 "%08x\n"' $(@:%-ram.mem=%.bin) > $@

%.s: %.c
	@echo "Building $< -> $@"
	@$(CC) -S $(CFLAGS) -o $@ $<

clean:
	@echo "Cleaning build files"
	rm -f $(ASM) $(OBJECTS) $(ASM_OBJECTS) *.elf *.hex *.bin *.mem *.s.o *.map *.dump
