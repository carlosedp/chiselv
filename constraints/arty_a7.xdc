# Clock pin
set_property PACKAGE_PIN E3 [get_ports {clock}]
set_property IOSTANDARD LVCMOS33 [get_ports {clock}]

# LEDs
set_property PACKAGE_PIN H5  [get_ports {io_led0}]
set_property PACKAGE_PIN J5  [get_ports {io_GPIO0[0]}]
set_property PACKAGE_PIN T9  [get_ports {io_GPIO0[1]}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_led0}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_GPIO0}]
set_property IOSTANDARD LVCMOS33 [get_ports {io_GPIO0[1]}]

# Switches
set_property PACKAGE_PIN D9 [get_ports { reset }]
set_property IOSTANDARD LVCMOS33 [get_ports { reset }]

# Clock constraints
create_clock -period 10.0 [get_ports {clock}]
