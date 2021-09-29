# ChiselV - A RISC-V Processor in Chisel

```sh
   `.`
 `.:+/:            MM'""""'YMM dP       oo                   dP M""MMMMM""M
 ./:-:/:`          M' .mmm. `M 88                            88 M  MMMMM  M
 `:---:::.`        M  MMMMMooM 88d888b. dP .d8888b. .d8888b. 88 M  MMMMP  M
   `.---:::-`      M  MMMMMMMM 88'  `88 88 Y8ooooo. 88ooood8 88 M  MMMM' .M
     `-----/+.     M. `MMM' .M 88    88 88       88 88.  ... 88 M  MMP' .MM
       .-``.:+-    MM.     .dM dP    dP dP `88888P' `88888P' dP M     .dMMM
        .--.-+s-   MMMMMMMMMMM                                  MMMMMMMMMMM
          `-o::/`
              `.::
                .+o+//+:`
                `+oooooss:
               `-.:ooooooss-
                `-.-/ooooosyo-
                  .-.-+oossssyo.
                   `.--:osssssso:`
                     `--:/ssoo+/-`
                       ../s+/-`
                        `--`
```

This project is a learning exercise for both writing a RISC-V core and also
better understand [Chisel](https://www.chisel-lang.org/), an HDL language based on Scala.

Currently the target is to have a RV32I core.

## Planned features

* Add a standard bus like Wishbone
* Integrate peripherals thru this bus (UART, etc)
* Add memory controller for SDRAM or DDR
