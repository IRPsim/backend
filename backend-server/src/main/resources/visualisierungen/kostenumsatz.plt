unset key
set datafile separator ';'
set title 'Kostennutzendiagramm'
set xlabel 'Kosten [€]'
set ylabel 'Umsatz [€]'
plot 'download.csv' using 2:1:3:4 with points pt 7 ps var lc var, 'download.csv' using 2:1:5 with labels notitle, x linecolor 1
MAX=(GPVAL_X_MAX<GPVAL_Y_MAX)?GPVAL_Y_MAX:GPVAL_X_MAX
MIN=(GPVAL_X_MIN>GPVAL_Y_MIN)?GPVAL_Y_MIN:GPVAL_X_MIN
set xrange [MIN-(MAX-MIN)*0.2:MAX+(MAX-MIN)*0.2]
set yrange [MIN-(MAX-MIN)*0.2:MAX+(MAX-MIN)*0.2]
set terminal png
set output 'download.png'
plot 'download.csv' using 2:1:3:4 with points pt 7 ps var lc var, 'download.csv' using 2:1:5 with labels notitle, x linecolor 1
