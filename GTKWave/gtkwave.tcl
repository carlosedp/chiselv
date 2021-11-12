#
# GTKWave Grouping and colorizing script
#
# Load the trace with `gtkwave -S gtkwave.tcl yourtrace.vcd`
#
# Customize the inserted traces in the TOPlevel section below
# and at the bottom in the add_signals function calls
#

# Customize this section as needed
## Add TOPLevel clock, reset and stall signals
set top [list clock reset stall]

## This adds the signals based on a list of {"signal_name_filter" Color}
set signals_to_add {{"PC.io_pcPort" Green ""} {"instructionMemory.io_memPort" Red ""} {"decoder.io_DecoderPort" Violet ""} {"ALU.io_ALUPort" Orange ""} {"registerBank.io_regPort" Green ""} {"registerBank.regs_" Green "regs_0_"} {"memoryIOManager.io_MemoryIOPort_" Red ""} {"dataMemory.io_dualPort_" Yellow ""}}

## For mapping values, add the translate files and the signals to map file
# Format file as: "signal_value mapped_value" (one per line)
set instructions [ gtkwave::setCurrentTranslateFile ./GTKWave/instruction_map.txt ]
set insts [list "_inst"]
set registers [ gtkwave::setCurrentTranslateFile ./GTKWave/registers.txt ]
set regs [list "_rd" "_rs1" "_rs2"]
array set reg_names [list {0[31:0]} {x0(Zero)} \
                          {1[31:0]} {x1(RA)} \
                          {2[31:0]} {x2(SP)} \
                          {3[31:0]} {x3(GP)} \
                          {4[31:0]} {x4(TP)} \
                          {5[31:0]} {x5(t0)} \
                          {6[31:0]} {x6(t1)} \
                          {7[31:0]} {x7(t2)} \
                          {8[31:0]} {x8-(s0/fp)} \
                          {9[31:0]} {x9(s1)} \
                          {10[31:0]} {x10(a0)} \
                          {11[31:0]} {x11(a1)} \
                          {12[31:0]} {x12(a2)} \
                          {13[31:0]} {x13(a3)} \
                          {14[31:0]} {x14(a4)} \
                          {15[31:0]} {x15(a5)} \
                          {16[31:0]} {x16(a6)} \
                          {17[31:0]} {x17(a7)} \
                          {18[31:0]} {x18(s2)} \
                          {19[31:0]} {x19(s3)} \
                          {20[31:0]} {x20(s4)} \
                          {21[31:0]} {x21(s5)} \
                          {22[31:0]} {x22(s6)} \
                          {23[31:0]} {x23(s7)} \
                          {24[31:0]} {x24(s8)} \
                          {25[31:0]} {x25(s9)} \
                          {26[31:0]} {x26(s10)} \
                          {27[31:0]} {x27(s11)} \
                          {28[31:0]} {x28(t3)} \
                          {29[31:0]} {x29(t4)} \
                          {30[31:0]} {x30(t5)} \
                          {31[31:0]} {x31(t6)} ]

################################################################################
# Don't touch from here down
set tmpdir $::env(PATH)
puts $tmpdir

# Load all signals
set nsigs [ gtkwave::getNumFacs ]
set sigs [list]

# Customize view settings
gtkwave::nop
gtkwave::/Edit/Set_Trace_Max_Hier 1
gtkwave::/View/Show_Filled_High_Values 1
gtkwave::/View/Show_Wave_Highlight 1
gtkwave::/View/Show_Mouseover 1

gtkwave::/Edit/Insert_Comment "Clock & Reset"
gtkwave::addSignalsFromList $top
gtkwave::highlightSignalsFromList $top
gtkwave::/Edit/Color_Format/Indigo
gtkwave::/Edit/UnHighlight_All
gtkwave::/Edit/Insert_Blank

proc translate {element_list element mapping_file} {
    set iselement 0
    foreach e $element_list {
        if {[ string first $e $element ] != -1} {
            set iselement 1
        }
    }
    if {$iselement == 1 } {
        gtkwave::highlightSignalsFromList "$element"
        gtkwave::installFileFilter $mapping_file
        gtkwave::/Edit/UnHighlight_All
    }
    # return $iselement
}

proc add_signals { filter color filterOut} {
    global nsigs
    global instructions
    global registers
    global reg_names
    global regs
    global insts

    set filterKeyword $filter
    set monitorSignals [list]
    for {set i 0} {$i < $nsigs } {incr i} {
        set facname [ gtkwave::getFacName $i ]
        set index [ string first $filterKeyword $facname  ]
        set index2 [ string first $filterOut $facname  ]

        if {$index != -1 && $index2 == -1} {
            lappend monitorSignals "$facname"
        }
    }
    gtkwave::/Edit/Insert_Comment $filter
    gtkwave::addSignalsFromList $monitorSignals
    gtkwave::/Edit/Insert_Blank
    foreach v $monitorSignals {
        set a [split $v .]
        set a [lindex $a end]
        gtkwave::highlightSignalsFromList $v
    }
    gtkwave::/Edit/Color_Format/$color
    gtkwave::/Edit/UnHighlight_All

    foreach v $monitorSignals {
        if {[info exists regs]} {
            translate $regs $v $registers
        }
        if {[info exists insts]} {
            translate $insts $v $instructions
        }
    }
    ## This sets the register signal names to be aliased to the register name
    foreach v $monitorSignals {
        if {[string first regs_ $v] != -1} {
        set name [string range [lsearch -inline [split $v .] {regs_*}] 5 end]
        gtkwave::highlightSignalsFromList "$v"
        gtkwave::/Edit/Alias_Highlighted_Trace $reg_names($name)
        gtkwave::/Edit/UnHighlight_All

        }
    }
}

# Zoom all
gtkwave::/Time/Zoom/Zoom_Full

# Add signals thru filters
foreach s $signals_to_add {
    add_signals [lindex $s 0] [lindex $s 1] [lindex $s 2]
}
