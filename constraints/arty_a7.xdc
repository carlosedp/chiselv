# Clock pin
set_property -dict { PACKAGE_PIN E3 IOSTANDARD LVCMOS33 } [get_ports {clock}]

# Clock constraints
create_clock -period 10.0 [get_ports {clock}]

# Reset
set_property -dict { PACKAGE_PIN C2 IOSTANDARD LVCMOS33 } [get_ports { reset }]

# LEDs
set_property -dict { PACKAGE_PIN H5    IOSTANDARD LVCMOS33 } [get_ports {io_led0}]
set_property -dict { PACKAGE_PIN J5    IOSTANDARD LVCMOS33 } [get_ports {io_GPIO0[0]}]
set_property -dict { PACKAGE_PIN T9    IOSTANDARD LVCMOS33 } [get_ports {io_GPIO0[1]}]
set_property -dict { PACKAGE_PIN T10   IOSTANDARD LVCMOS33 } [get_ports {io_GPIO0[2]}]

set_property -dict { PACKAGE_PIN H6    IOSTANDARD LVCMOS33 } [get_ports { io_GPIO0[3] }]; #IO_L24P_T3_35 Sch=led3_g
set_property -dict { PACKAGE_PIN J2    IOSTANDARD LVCMOS33 } [get_ports { io_GPIO0[4] }]; #IO_L22N_T3_35 Sch=led2_g
set_property -dict { PACKAGE_PIN J4    IOSTANDARD LVCMOS33 } [get_ports { io_GPIO0[5] }]; #IO_L21P_T3_DQS_35 Sch=led1_g
set_property -dict { PACKAGE_PIN F6    IOSTANDARD LVCMOS33 } [get_ports { io_GPIO0[6] }]; #IO_L19N_T3_VREF_35 Sch=led0_g

##Buttons
set_property -dict { PACKAGE_PIN D9    IOSTANDARD LVCMOS33 } [get_ports { io_GPIO0[7] }]; #IO_L6N_T0_VREF_16 Sch=btn[0]
# set_property -dict { PACKAGE_PIN C9    IOSTANDARD LVCMOS33 } [get_ports { io_GPIO0[8] }]; #IO_L11P_T1_SRCC_16 Sch=btn[1]
# set_property -dict { PACKAGE_PIN B9    IOSTANDARD LVCMOS33 } [get_ports { btn[2] }]; #IO_L11N_T1_SRCC_16 Sch=btn[2]
# set_property -dict { PACKAGE_PIN B8    IOSTANDARD LVCMOS33 } [get_ports { btn[3] }]; #IO_L12P_T1_MRCC_16 Sch=btn[3]

##USB-UART Interface
set_property -dict { PACKAGE_PIN D10   IOSTANDARD LVCMOS33 } [get_ports { io_UART0tx }];
set_property -dict { PACKAGE_PIN A9    IOSTANDARD LVCMOS33 } [get_ports { io_UART0rx }];


