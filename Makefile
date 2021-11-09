SHELL = bash
# Project name
project = toplevel

generated_files = generated

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
VERILATOR=verilator

# Default board PLL
BOARD := bypass

# Targets
chisel: check-board-vars clean ## Generates Verilog code from Chisel sources using SBT
	${SBT} "run --target:fpga -board ${BOARD} -cpufreq 50000000 -td $(generated_files) -invreset false"

chisel_tests:
	${SBT} "test"

check: chisel_tests ## Run Chisel tests
test: chisel_tests

romfile = gcc/helloUART/main-rom.mem
ramfile = gcc/helloUART/main-ram.mem
verilator: chisel
	@rm -rf obj_dir
	$(VERILATOR) -O3 --assert $(foreach f,$(wildcard generated/*.v),--cc $(f)) --exe verilator/chiselv.cpp verilator/uart.c --top-module Toplevel -o chiselv --timescale 1ns/1ps
	@make -C obj_dir -f VToplevel.mk -j`nproc`
	@cp obj_dir/chiselv .
	@cp $(romfile) progload.mem
	@cp $(ramfile) progload-RAM.mem

verirun:
	@cp $(romfile) progload.mem
	@cp $(ramfile) progload-RAM.mem
	./chiselv

dot:
	yosys -p "read_verilog Toplevel.v; proc; opt; show -colors 2 -width -format dot -prefix chiselv -signed Toplevel"

fmt: ## Formats code using scalafmt and scalafix
	${SBT} lint

.PHONY: gcc
gcc: ## Builds gcc sample code
	pushd gcc/test ; make ; popd

check-board-vars:
	@test "$(BOARD)" != "bypass" || (echo "Set BOARD variable to one of the supported boards: " ; cat chiselv.core|grep "\-board" |cut -d '-' -f 2|sed s/\"//g | sed s/board\ //g |tr -s '\n' ','| sed 's/,$$/\n/'; echo "Eg. make chisel BOARD=ulx3s"; echo; echo "Generating design with bypass PLL..."; echo)

clean:   ## Clean all generated files
	@rm -rf obj_dir test_run_dir target
	@rm -rf $(generated_files)
	@rm -rf out
	@rm -f $(project)
	@rm -f chiselv
	@rm -f *.mem

help:
	@echo "Makefile targets:"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = "[:##]"}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$4}'
	@echo ""

.PHONY: chisel clean prog help verilator
.DEFAULT_GOAL := help
