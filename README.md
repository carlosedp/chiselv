# ChiselV - A RISC-V Processor in Chisel

```sh
(..,,***)
 ( #,,****)             MM''''''YMM dP       oo                   dP M''MMMMM''M
  (,,.,,***)            M' .mmm. 'M 88                            88 M  MMMMM  M
   (,,,,,,*,,)          M  MMMMMooM 88d888b. dP .d8888b. .d8888b. 88 M  MMMMP  M
     (,,,,,,,,,)        M  MMMMMMMM 88'  '88 88 Y8ooooo. 88ooood8 88 M  MMMM' .M
       ( .,,...,*)      M. 'MMM' .M 88    88 88       88 88.  ... 88 M  MMP' .MM
         ( .,,,.., )    MM.     .dM dP    dP dP '88888P' '88888P' dP M     .dMMM
           (,,.,,.,..)  MMMMMMMMMMM                                  MMMMMMMMMMM
             (,*,.   ,)
               (,*#/#.,)            INSTRUCTION SETS WANT TO BE FREE
                  ,###.
                    *#*(,
                      /((((((((##.
                    ...*(((((/(/(/##(
                    .... ((((((((((###
                      *....,(((((((((((###
                        .,....(((((((((((###
                          *....(/((((((((####/
                            *.,,.,((((((((((((((
                              /.,,,,((#((/(/***
                                /...((((***
                                  ,(*/*
```

[![Scala CI](https://github.com/carlosedp/chiselv/actions/workflows/scala.yml/badge.svg)](https://github.com/carlosedp/chiselv/actions/workflows/scala.yml)

This project is a learning exercise for both writing a RISC-V core and also
better understand [Chisel](https://www.chisel-lang.org/), an HDL language based on Scala.

Currently the target builds a RV32I core.

## Generating Verilog

Verilog code can be generated from Chisel by using the `chisel` Makefile target. It requires a `-board` parameter so the correct PLL is included in the design. If it's not provided, a bypass PLL will be used.

```sh
make chisel BOARD=artya7-35
```

The `BOARD` argument must match one of the `pll_BOARD.v]` files in `/src/main/resources` directory.

The core can be simulated in Verilator using the commands:

```sh
make verilator   # this will build the core, generate the Verilog files and Verilator project
make verirun     # This will copy the UART demo (RAM/ROM) from gcc/helloUART and run Verilator
```

## Building for FPGAs

The standard build process uses locally installed tools like Java (for Chisel generation), Yosys, NextPNR, Vivado and others. It's recommended to use [Fusesoc](https://github.com/olofk/fusesoc) for building the complete workflow by using containers thru a command launcher.

### Fusesoc build and generation

<details>
  <summary>Installing FuseSOC</summary>

To install Fusesoc (requires Python3 and pip3):

```sh
pip3 install --upgrade --user fusesoc
```

Check if it's working:

```sh
$ fusesoc --version
1.12.0
```

If the terminal reports an error about the command not being found check that the directory `~/.local/bin` is in your command search path (`export PATH=~/.local/bin:$PATH`).

</details>

Fusesoc allows multiple boards from different vendors to be supported by the project. It uses chisel-generator to generate Verilog from Scala sources and calls the correct board EDA backend to create it's project files.

For example, to generate the programming files for the **ULX3s** board based on Lattice ECP5:

```sh
mkdir fusesoc-chiselv && cd fusesoc-chiselv

# Add fusesoc standard library and this core
fusesoc library add fusesoc-cores https://github.com/fusesoc/fusesoc-cores
fusesoc library add chiselv https://github.com/carlosedp/chiselv

# Download the command wrapper
wget https://gist.github.com/carlosedp/c0e29d55e48309a48961f2e3939acfe9/raw/bfeb1cfe2e188c1d5ced0b09aabc9902fdfda6aa/runme.py
chmod +x runme.py

# Run fusesoc with the wrapper as an environment var
EDALIZE_LAUNCHER=$(realpath ./runme.py) fusesoc run --target=ulx3s_85 carlosedp:chiselv:singlecycle

# The output files will be on the local ./build dir:
...
# Output bitstream will be on build/carlosedp_demo_chiselblinky_0/ulx3s_85-trellis
❯ ll build/carlosedp_chiselv_singlecycle_0/ulx3s_85-trellis
total 6.9M
-rw-r--r-- 1 cdepaula staff 1.2K Oct 15 16:25 proginfo.py
-rw-r--r-- 1 cdepaula staff  507 Oct 15 16:25 boardconfig.yaml
-rw-r--r-- 1 cdepaula staff  198 Oct 15 16:25 progload.mem
-rw-r--r-- 1 cdepaula staff 1.6K Oct 15 16:25 carlosedp_chiselv_singlecycle_0.eda.yml
-rw-r--r-- 1 cdepaula staff  483 Oct 15 16:25 edalize_yosys_procs.tcl
-rw-r--r-- 1 cdepaula staff  247 Oct 15 16:25 edalize_yosys_template.tcl
-rw-r--r-- 1 cdepaula staff 1.1K Oct 15 16:25 Makefile
-rw-r--r-- 1 cdepaula staff  67K Oct 15 16:25 carlosedp_chiselv_singlecycle_0.blif
-rw-r--r-- 1 cdepaula staff 708K Oct 15 16:25 carlosedp_chiselv_singlecycle_0.json
-rw-r--r-- 1 cdepaula staff  57K Oct 15 16:25 carlosedp_chiselv_singlecycle_0.edif
-rw-r--r-- 1 cdepaula staff  74K Oct 15 16:25 yosys.log
-rw-r--r-- 1 cdepaula staff  32K Oct 15 16:25 carlosedp_chiselv_singlecycle_0.config
-rw-r--r-- 1 cdepaula staff 9.8K Oct 15 16:25 next.log
-rw-r--r-- 1 cdepaula staff 1.9M Oct 15 16:26 carlosedp_chiselv_singlecycle_0.bit
-rw-r--r-- 1 cdepaula staff 3.9M Oct 15 16:26 carlosedp_chiselv_singlecycle_0.svf

# Programming instructions will be printed-out.
```

Just program it to your FPGA with `OpenOCD` or in ULX3S case, [`ujprog`](https://github.com/f32c/tools/tree/master/ujprog)


## Planned features

* Add a standard bus like Wishbone
* Integrate peripherals thru this bus (UART, etc)
* Add memory controller for SDRAM or DDR

## Adding support to new boards

<details>
  <summary>Click to expand</summary>

Support for new boards can be added in the `chiselv.core` file and programming instructions in the `proginfo/buildconfig.yaml` together with a board template text.

Three sections are required:

### Fileset

Filesets lists the dependency from the chisel-generator, that outputs Verilog from Chisel (Scala) code. It also contains the static files used for each board like constraints and programming config that must be copied to the output project dir and used by EDA. The programming info text template is also added.

```yaml
  ulx3s-85:
    depend: ["fusesoc:utils:generators:0.1.6"]
    files:
      - constraints/ecp5-ulx3s.lpf: { file_type: LPF }
      - openocd/ft231x.cfg: { file_type: user }
      - openocd/LFE5U-85F.cfg: { file_type: user }
      - proginfo/ulx3s-template.txt: { file_type: user }
```

### Generate

The generator section contains the Chisel generator parameters. It has the arguments to be passed to Chisel (the board), the project name and the output files created by the generator to be used by the EDA.

```yaml
  ulx3s:
    generator: chisel
    parameters:
      extraargs: "--target:fpga -board ulx3s"
      buildtool: sbt
      copy_core: true
      output:
        files:
          - generated/Toplevel.v: { file_type: verilogSource }
          - generated/GPIOInOut.v: { file_type: verilogSource }
          - generated/pll_ulx3s.v: { file_type: verilogSource }
```

### Target

Finally the target section has the board information to be passed to the EDA tools. Parameters like the package/die or extra parameters to synthesis or PnR. This is highly dependent of the EDA backend. It's name is the one passed on the `--target=` param on FuseSoc. It also references the fileset and generate configs.

```yaml
  ulx3s_85:
    default_tool: trellis
    description: ULX3S 85k version
    filesets: [ulx3s-85, proginfo, progload]
    generate: [ulx3s]
    hooks:
      post_run: [ulx3s-85f]
    tools:
      diamond:
        part: LFE5U-85F-6BG381C
      trellis:
        nextpnr_options: [--package, CABGA381, --85k]
        yosys_synth_options: [-abc9, -nowidelut]
    toplevel: Toplevel
```

### Post-run script

If you desire to add a programming information text output after generating the bitstream files, add the board to the `scripts` section (and to it's target hooks) calling the proginfo.py with a board identifier that must match the `boardconfig.yaml` file in the `proginfo` dir.

```yaml
  ulx3s-85f:
    cmd : [python3, proginfo.py, ulx3s-85f]
```

The `boardconfig.yaml` file must contain the files names used by each board and a corresponding template `.txt` file that will contain the output text. This will be printed after bitstream generation.

</details>
