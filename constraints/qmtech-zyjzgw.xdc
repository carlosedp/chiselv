##### Core board #####

set_property LOC F22 [get_ports clock]
set_property IOSTANDARD LVCMOS33 [get_ports {clock}]
create_clock -period 20.0 [get_ports {clock}]

# LED2_FPGA R26
set_property LOC R26 [get_ports io_led0]
set_property IOSTANDARD LVCMOS33 [get_ports {io_led0}]

# LED3_FPGA P26
set_property LOC P26 [get_ports io_led2]
set_property IOSTANDARD LVCMOS33 [get_ports {io_led2}]

##Buttons
# Sw 2 - AB26
set_property LOC AB26 [get_ports reset]
set_property IOSTANDARD LVCMOS33 [get_ports {reset}]

# Sw 3 - AC26
# set_property LOC AC26 [get_ports sw3]
# set_property IOSTANDARD LVCMOS33 [get_ports {sw3}]

##### Daughter board #####

# LED3_FPGA BANK14_E25
# set_property LOC E25 [get_ports io_led1]
# set_property IOSTANDARD LVCMOS33 [get_ports {io_led1}]

# LED4_FPGA BANK16_C14
# set_property LOC C14 [get_ports io_GPIO0[2]]
# set_property IOSTANDARD LVCMOS33 [get_ports {io_GPIO0[2]}]

# LED5_FPGA BANK16_B14
# set_property LOC B14 [get_ports io_GPIO0[3]]
# set_property IOSTANDARD LVCMOS33 [get_ports {io_GPIO0[3]}]

##Buttons
# Sw1
# set_property LOC Y22 [get_ports sw1]
# set_property IOSTANDARD LVCMOS33 [get_ports {sw1}]
# Sw2
# set_property LOC AA22 [get_ports sw2]
# set_property IOSTANDARD LVCMOS33 [get_ports {sw2}]
# Sw3
# set_property LOC Y23 [get_ports sw3]
# set_property IOSTANDARD LVCMOS33 [get_ports {sw3}]
# Sw4
# set_property LOC AA24 [get_ports sw4]
# set_property IOSTANDARD LVCMOS33 [get_ports {sw4}]
# Sw5
# set_property LOC AC23 [get_ports sw5]
# set_property IOSTANDARD LVCMOS33 [get_ports {sw5}]
# Sw6
# set_property LOC AC24 [get_ports sw6]
# set_property IOSTANDARD LVCMOS33 [get_ports {sw6}]
# Sw7
# set_property LOC AA25 [get_ports sw7]
# set_property IOSTANDARD LVCMOS33 [get_ports {sw7}]
# Sw8
# set_property LOC AB25 [get_ports sw8]
# set_property IOSTANDARD LVCMOS33 [get_ports {sw8}]


##USB-UART Interface (On jp3 in daughter board) Pin 1 - GND, Pin 3 - TX, Pin 5 - RX
# set_property LOC AE26 [get_ports io_UART0_tx]
# set_property IOSTANDARD LVCMOS33 [get_ports {io_UART0_tx}]
# set_property LOC AD26 [get_ports io_UART0_rx]
# set_property IOSTANDARD LVCMOS33 [get_ports {io_UART0_rx}]