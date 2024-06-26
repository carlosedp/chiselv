# Source and target files/directories
project = chiselv
scala_files = $(wildcard $(project)/src/*.scala) $(wildcard $(project)/resources/*.scala) $(wildcard $(project)/test/src/*.scala)
generated_files = generated/$(wildcard *.sv) $(wildcard *.v)
export PATH := $(PWD):$(PATH)

# Toolchains and tools
MILL = ./mill
DOCKERARGS  = run --rm -v $(PWD):/src -w /src

# Define utility applications for simulation
YOSYS = docker $(DOCKERARGS) hdlc/yosys yosys
VERILATORLOCAL := $(shell command -v verilator 2> /dev/null)
VERILATORARGS = --name verilator --hostname verilator --rm -it --entrypoint= -v $(PWD):/work -w /work
ifndef VERILATORLOCAL
	VERILATOR = docker $(DOCKERARGS) $(VERILATORARGS) gcr.io/hdl-containers/verilator:latest
else
	VERILATOR =
endif

# Set board PLL or bypass if not defined
BOARD ?= bypass
PLLFREQ ?= 50000000
BOARDPARAMS=--board ${BOARD} --cpufreq ${PLLFREQ}
# Check if generating for a different board/pll
$(if $(findstring $(shell cat .genboard 2>/dev/null),$(BOARDPARAMS)),,$(shell echo ${BOARDPARAMS} > .genboard))
CHISELPARAMS = --target-dir generated --split-verilog

# Targets
all: chisel gcc

chisel: $(generated_files) ## Generates Verilog code from Chisel sources (output to ./generated)
$(generated_files): $(scala_files) build.sc Makefile .genboard firtool
	@test "$(BOARD)" != "bypass" || (printf "Generating design with bypass PLL (for simulation). If required, set BOARD and PLLFREQ variables to one of the supported boards: " ; test -f chiselv.core && cat chiselv.core|grep "\-board"|cut -d '-' -f 3 | grep -v bypass | sed s/board\ //g |tr -s ' \n' ','| sed 's/,$$/\n/'; echo "Eg. make chisel BOARD=ulx3s PLLFREQ=15000000"; echo)
	$(MILL) $(project).run $(BOARDPARAMS) $(CHISELPARAMS)
	@echo "Generated files in $(generated_files)"

firtool:
	@./download_firtool.sh

check: test
.PHONY: test
test:## Run Chisel tests
	$(MILL) Alias/run test

.PHONY: lint
lint: ## Formats code using scalafmt and scalafix
	$(MILL) Alias/run lint

.PHONY: deps
deps: ## Check for library version updates
	$(MILL) Alias/run deps

rvfi: $(rvfi_files) ## Generates Verilog code for RISC-V Formal tests
$(rvfi_files):  $(scala_files) build.sc Makefile
	$(MILL) $(project)_rvfi.run $(CHISELPARAMS)

# This section defines the Verilator simulation and demo application to be used
# Below, I use -DENABLE_INITIAL_MEM_ to enable initial memory load on firtool from progload.mem and progload-RAM.mem
binfile = chiselv.bin
verilator: $(binfile) ## Generate Verilator simulation
$(binfile): $(generated_files)
	@rm -rf obj_dir
	$(VERILATOR) verilator -O3 --timescale 1ns/1ps -DENABLE_INITIAL_MEM_ --assert $(foreach f,$(shell find ./generated -name "*.v" -o -name "*.sv"),--cc $(f)) --exe verilator/chiselv.cpp verilator/uart.c --top-module Toplevel -o $(binfile)
	make -C obj_dir -f VToplevel.mk -j`nproc`
	@cp obj_dir/$(binfile) .

# Adjust the rom and ram files below to match the desired demo app
romfile = gcc/helloUART/main-rom.mem
ramfile = gcc/helloUART/main-ram.mem
verirun: $(binfile) ## Run Verilator simulation with ROM and RAM files to be loaded
	@cp $(romfile) progload.mem
	@cp $(ramfile) progload-RAM.mem
	@echo "------------------------------------------------------"
	@bash -c "trap 'reset' EXIT; ./$(binfile)"

MODULE ?= Toplevel
dot: $(generated_files) ## Generate dot files for Core
	@echo "Generating graphviz dot file for module \"$(MODULE)\". For a different module, pass the argument as \"make dot MODULE=mymod\"."
	@touch progload.mem progload-RAM.mem
	@$(YOSYS) -p "read_verilog ./generated/*.v ./generated/*.sv; proc; opt; show -colors 2 -width -format dot -prefix $(MODULE) -signed $(MODULE)"
	@rm progload.mem progload-RAM.mem

.PHONY: gcc
gcc: ## Builds gcc sample code
	@for d in `find gcc -name Makefile`;do echo ----\\nBuilding $$d; pushd `dirname $$d`; make; popd; done

.PHONY: clean
clean:   ## Clean all generated files
	$(MILL) clean
	@rm -rf obj_dir test_run_dir target
	@rm -rf $(generated_files)
	@rm -rf tmphex
	@rm -rf out
	@rm -f *.mem

.PHONY: cleanall
cleanall: clean  ## Clean all downloaded dependencies and cache
	@rm -rf project/.bloop
	@rm -rf project/project
	@rm -rf project/target
	@rm -rf .bloop .bsp .metals .vscode

.PHONY: help
help:
	@echo "Makefile targets:"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = "[:##]"}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$4}'
	@echo ""

.DEFAULT_GOAL := help
