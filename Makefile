# Source and target files/directories
scala_files = $(wildcard src/main/scala/*.scala)
generated_files = generated
BUILDTOOL ?= sbt 							# Can also be mill

# Define the SBT command (Docker or local)
DOCKERARGS  = run --rm -v $(PWD):/src -w /src
SBTIMAGE   = docker $(DOCKERARGS) adoptopenjdk:8u282-b08-jre-hotspot
SBTCMD   = $(SBTIMAGE) curl -Ls https://git.io/sbt > /tmp/sbt && chmod 0775 /tmp/sbt && /tmp/sbt
SBTLOCAL := $(shell command -v sbt 2> /dev/null)
ifndef SBTLOCAL
    SBT=${SBTCMD}
else
	SBT=sbt
endif

# Define utility applications
YOSYS = docker $(DOCKERARGS) hdlc/yosys yosys
VERILATORARGS = --name verilator --hostname verilator --rm -it --entrypoint= -v $(PWD):/work -w /work
VERILATOR=  # Local Verilator
# VERILATOR = docker $(DOCKERARGS) $(VERILATORARGS) verilator/verilator

# Default board PLL
BOARD := bypass
BOARDPARAMS=-board ${BOARD} -cpufreq 50000000 -invreset false
CHISELPARAMS = --target:fpga -td $(generated_files)
# CHISELPARAMS = --target:fpga -td $(generated_files) --emission-options=disableMemRandomization,disableRegisterRandomization

# Targets
chisel: $(generated_files) ## Generates Verilog code from Chisel sources using SBT

$(generated_files): $(scala_files) build.sbt
	@rm -rf $(generated_files)
	@test "$(BOARD)" != "bypass" || (echo "Set BOARD variable to one of the supported boards: " ; test -f chiselv.core && cat chiselv.core|grep "\-board" |cut -d '-' -f 2|sed s/\"//g | sed s/board\ //g |tr -s '\n' ','| sed 's/,$$/\n/'; echo "Eg. make chisel BOARD=ulx3s"; echo; echo "Generating design with bypass PLL..."; echo)
	@if [ $(BUILDTOOL) = "sbt" ]; then \
		${SBT} "run $(CHISELPARAMS) $(BOARDPARAMS)"; \
    elif [ $(BUILDTOOL) = "mill" ]; then \
		project=grep object build.sc |grep -v extends |sed s/object//g |xargs \
		scripts/mill $(project).run $(CHISELPARAMS) $(BOARDPARAMS); \
	fi

chisel_tests:
	@if [ $(BUILDTOOL) = "sbt" ]; then \
		${SBT} "test"; \
    elif [ $(BUILDTOOL) = "mill" ]; then \
		project=grep object build.sc |grep -v extends |sed s/object//g |xargs \
		scripts/mill $(project).test; \
	fi

rvfi: clean ## Generates Verilog code for RISC-V Formal tests
	${SBT} "runMain chiselv.RVFITop --target:fpga -td $(generated_files) $(BOARDPARAMS)"

check: chisel_tests ## Run Chisel tests
test: chisel_tests

# This section defines the Verilator simulation and demo application to be used
verilator: $(generated_files) ## Generate Verilator simulation
	@rm -rf obj_dir
	$(VERILATOR) verilator -O3 --assert $(foreach f,$(wildcard generated/*.v),--cc $(f)) --exe verilator/chiselv.cpp verilator/uart.c --top-module Toplevel -o chiselv --timescale 1ns/1ps
	make -C obj_dir -f VToplevel.mk -j`nproc`
	@cp obj_dir/chiselv .

# Adjust the rom and ram files below to match your test
romfile = gcc/helloUART/main-rom.mem
ramfile = gcc/helloUART/main-ram.mem
verirun: ## Run Verilator simulation with ROM and RAM files to be loaded
	@cp $(romfile) progload.mem
	@cp $(ramfile) progload-RAM.mem
	@bash -c "trap 'reset' EXIT; ./chiselv"

MODULE ?= Toplevel
dot: $(generated_files) ## Generate dot files for Core
	@echo "Generating graphviz dot file for module \"$(MODULE)\". For a different module, pass the argument as \"make dot MODULE=mymod\"."
	@touch progload.mem progload-RAM.mem
	@$(YOSYS) -p "read_verilog ./generated/*.v; proc; opt; show -colors 2 -width -format dot -prefix $(MODULE) -signed $(MODULE)"
	@rm progload.mem progload-RAM.mem

fmt: ## Formats code using scalafmt and scalafix
	${SBT} lint

.PHONY: gcc
gcc: ## Builds gcc sample code
	pushd gcc/test ; make ; popd

check-board-vars:
	@test "$(BOARD)" != "bypass" || (echo "Set BOARD variable to one of the supported boards: " ; cat chiselv.core|grep "\-board" |cut -d '-' -f 2|sed s/\"//g | sed s/board\ //g |tr -s '\n' ','| sed 's/,$$/\n/'; echo "Eg. make chisel BOARD=ulx3s"; echo; echo "Generating design with bypass PLL..."; echo)

clean:   ## Clean all generated files
	@if [ $(BUILDTOOL) = "sbt" ]; then \
		${SBT} "clean"; \
    elif [ $(BUILDTOOL) = "mill" ]; then \
		scripts/mill clean; \
	fi
	@rm -rf obj_dir test_run_dir target
	@rm -rf $(generated_files)
	@rm -rf out
	@rm -f chiselv
	@rm -f *.mem

cleancache: clean  ## Clean all downloaded dependencies and cache
	@rm -rf project/project
	@rm -rf project/target

help:
	@echo "Makefile targets:"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = "[:##]"}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$4}'
	@echo ""

.PHONY: clean prog help
.DEFAULT_GOAL := help
