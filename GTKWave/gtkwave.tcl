#
# GTKWave Grouping and colorizing script
#
# Load the trace with `gtkwave -S gtkwave.tcl yourtrace.vcd`
#
# Customize the inserted traces in the TOPlevel section below
# and at the bottom in the add_signals function calls
#

# Customize this section as needed
## Add TOPLevel clock and reset signals
set top [list TOP.clock TOP.reset ]

## This adds the signals based on a list of {"signal_name_filter" Color}
set signals_to_add {{"PC.io_pcPort" Green ""} {"instructionMemory.io_dualPort" Red ""} {"decoder.io_DecoderPort" Violet ""} {"ALU.io_ALUPort" Orange ""} {"registerBank.io_regPort" Yellow ""} {"registerBank.regs_" Yellow "regs_0_"} {"dataMemory.io_dualPort" Yellow ""}}

## For mapping values, add the translate files and the signals to map file
# Format file as: "signal_value mapped_value" (one per line)
set instructions [ gtkwave::setCurrentTranslateFile ./GTKWave/instruction_map.txt ]
set insts [list "_inst"]
set registers [ gtkwave::setCurrentTranslateFile ./GTKWave/registers.txt ]
set regs [list "_rd" "_rs1" "_rs2"]

################################################################################
# Don't touch from here down
set tmpdir $::env(PATH)
puts $tmpdir

# Load all signals
set nsigs [ gtkwave::getNumFacs ]
set sigs [list]

# Customize view settings
gtkwave::nop
gtkwave::/Edit/Set_Trace_Max_Hier 0
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
        gtkwave::/Edit/Alias_Highlighted_Trace x$name
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
