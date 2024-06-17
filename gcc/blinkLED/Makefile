SOURCES       := $(shell find . ../lib -name '*.c')
ASM_SOURCES   := $(shell find . ../lib -name '*.s')
OBJECTS       := $(SOURCES:%.c=%.o)
ASM_OBJECTS   := $(ASM_SOURCES:%.s=%.s.o)
ASM           := $(SOURCES:%.c=%.s)

DOCKERORPODMAN = $(shell command -v podman 2> /dev/null || echo docker)
USEDOCKER = 1
CURDIR = $(shell pwd)
DOCKERARGS = run --rm -v $(PWD)/..:/src -w /src/$(shell basename $(CURDIR))
DOCKERIMG  = $(DOCKERORPODMAN) $(DOCKERARGS) docker.io/carlosedp/crossbuild-riscv64:latest

CFLAGS=-Wall -mabi=ilp32 -march=rv32i -ffreestanding -fcommon -Os -I../lib
LDFLAGS=-T ../lib/riscv.ld -m elf32lriscv -O binary -Map=main.map

PREFIX=riscv64-linux-gnu

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
	@echo "Building $< -> $@ for http://tice.sea.eseo.fr/riscv/"
	@$(OC) -O ihex $< $@ --only-section .text\*

main-%.mem: main.elf  ## Readmemh 32bit memory files (rom or ram)
	@echo "Building $< -> $@"
	$(OC) -O binary $< $(@:main-%.mem=main-%.bin) --only-section $(if $(filter %rom.mem,$@),.text*,.*data*)
	$(HD) -ve '1/4 "%08x\n"' $(@:main-%.mem=main-%.bin) > $@

%.s: %.c
	@echo "Building $< -> $@"
	@$(CC) -S $(CFLAGS) -o $@ $<

clean:
	@echo "Cleaning build files"
	rm -f $(ASM) $(OBJECTS) $(ASM_OBJECTS) *.elf *.hex *.bin *.mem *.s.o *.map *.dump
