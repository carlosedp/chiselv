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

# Default board PLL
BOARD := bypass

# Targets
chisel: check-board-vars clean ## Generates Verilog code from Chisel sources using SBT
	${SBT} "run -board ${BOARD} -td $(generated_files)"

chisel_tests:
	${SBT} "test"

check: chisel_tests ## Run Chisel tests
test: chisel_tests

fmt: ## Formats code using scalafmt and scalafix
	${SBT} lint

.PHONY: gcc
gcc: ## Builds gcc sample code
	pushd gcc ; make ; popd

check-board-vars:
	@test "$(BOARD)" != "bypass" || (echo "Set BOARD variable to one of the supported boards: " ; cat chiselv.core|grep "\-board" |cut -d '-' -f 2|sed s/\"//g | sed s/board\ //g |tr -s '\n' ','| sed 's/,$$/\n/'; echo "Eg. make chisel BOARD=ulx3s"; echo; echo "Generating design with bypass PLL..."; echo)

clean:   ## Clean all generated files
	@rm -rf obj_dir test_run_dir target
	@rm -rf $(generated_files)
	@rm -rf out
	@rm -f $(project)

help:
	@echo "Makefile targets:"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = "[:##]"}; {printf "\033[36m%-20s\033[0m %s\n", $$1, $$4}'
	@echo ""

.PHONY: chisel clean prog help
.DEFAULT_GOAL := help
