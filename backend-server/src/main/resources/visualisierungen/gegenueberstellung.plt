set boxwidth 0.75 absolute
set terminal pngcairo  transparent enhanced font "arial,10" fontscale 1.0 size 600, 400 
set datafile separator ';'
set output 'download.png'
set style fill   solid 1.00 border lt -1
set key outside right top vertical Left reverse noenhanced autotitle columnhead nobox
set key invert samplen 4 spacing 1 width 0 height 0 
set style increment default
set style histogram rowstacked title textcolor lt -1
set datafile missing '-'
set style data histograms
set xtics border in scale 0,0 nomirror rotate by -45  autojustify
set xtics  norangelimit 
set title "Gegen\374berstellung Stromerzeugung"
plot 'download.csv' using 2:xtic(1), for [i=3:5] '' using i
