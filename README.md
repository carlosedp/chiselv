# Chisel Blinky

This is a simple Blinky project to demonstrate Chisel functionality with scripts
and tooling to be able to synth on FPGA boards.

## Synthesis using Open Source tools (yosys/nextpnr)

Synthesis on FPGAs is supported with yosys/nextpnr. At the moment the tools support
Lattice ECP5 FPGAs. The build process uses Docker images, so no software other than Docker needs
to be installed. If you prefer podman you can use that too, just adjust it in
`Makefile`, `DOCKER=podman`.

### Building and programming the FPGA

The `Makefile` currently supports the following FPGA boards by defining the `BOARD` parameter on make:

* Lattice [ECP5 Evaluation Board](http://www.latticesemi.com/ecp5-evaluation) - `evn`
* Radiona [ULX3S](https://radiona.org/ulx3s/) - `ulx3s`
* Greg Davill [Orangecrab](https://github.com/gregdavill/OrangeCrab) - `orangecrab`
* Q3k [Colorlight](https://github.com/q3k/chubby75/tree/master/5a-75b) - `colorlight`

For example, to build for the ULX3S Board, run:

```sh
make BOARD=ulx3s synth`
```

and to program the FPGA:

```sh
make BOARD=ulx3s prog

# or if your USB device has a different path, pass it on USBDEVICE, like:

make BOARD=ulx3s USBDEVICE=/dev/tty.usbserial-120001 prog
```

Programming using OpenOCD on Docker does not work on Docker Desktop for Mac/Windows since the container is run in a Linux VM and can not see the physical devices connected to the Host.

For the ULX3S board, the current OpenOCD does not support ft232 protocol so to program it, download [ujprog](https://github.com/emard/ulx3s-bin/tree/master/usb-jtag) for your platform and program using `./ujprog chiselwatt.bit` or to persist in the flash, `./ujprog -j FLASH chiselwatt.bit`.
