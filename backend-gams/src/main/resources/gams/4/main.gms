***MODEL VERIFLEX===========================================

*DATA IMPORT------------------------------------------
$onecho > indata.txt
dset=t rng=sh1!l10 rdim=1
par=prc_el rng=sh1!l10 rdim=1 cdim=0
par=D_Cl rng=sh1!N10 rdim=1 cdim=0
par=D_Cl_tot rng=sh2!A2 dim=0
par=S_eff rng=sh2!B2 dim=0
par=S_max rng=sh2!C2 dim=0
par=S_min rng=sh2!D2 dim=0
par=RU rng=sh2!E2 dim=0
par=RD rng=sh2!F2 dim=0

$offecho

***Erstellen der GDX auf Basis der Spezifikation
***$CALL GDXXRW ./input/modelinput.xlsx @./input/input_specification.txt

***Einlesen der GDX
$GDXIN %gdxincname%

*$load prc_el


*DATA-------------------------------------------------------

$INCLUDE ./input/input_01_general.gms
$INCLUDE ./input/input_02_tech_EY.gms
*set t time_steps /1*168/;
**set d data-elements /1*3/;
*parameter prc_el(t) day ahead spot price EUR per MWh;
*parameter D_Cl(t) demand for chlorine in t;
*scalar D_Cl_tot demand for chlorine for whole period in tCl /1380.82/;
*parameter S_in(t) input in Supply process;
*scalar S_eff conversion efficiency in power per ECU /2.5963/;
*scalar S_max of electrolyzer in MW /28/;
*scalar S_min of electrolyzer in MW     /16.8/;
*scalar RU of electrolyzer in MW per h /1/;
*scalar RD of electrolyzer in MW per h /1/;


*MODULES---------------------------------------------------
$INCLUDE ./module/set_tech_EY_module.gms
$INCLUDE ./module/obj_func_module.gms
*SOLVE---------------------------------------------------------
model CAE /mod_tech_EY,
           mod_obj_func/;
solve CAE using LP min c_tot;
*display
*display c_tot.l, x.l;
*OUTPUT-----------------------------------------------------
$INCLUDE ./output/output.gms
sca_total_cost=var_c_tot.l;
par_EY_power(set_t)=var_x.l(set_t) * sca_EY_S_eff;


*DATA EXPORT--------------------------------------------------
***execute_unload 'result.gdx', x;
*execute 'gdxxrw.exe result.gdx var=x rng=assessment!a2';
**var=c_tot rng=assessment!c2';
*
*$call "gdx2xls test2.gdx"


*Deklarieren der Ausgabedateien
FILE Output / './output/results/output.csv' /;
*Formatieren der deklarierten Ausgabedateien
**.nd-Funktion (number of decimals) legt die max. Anzahl an Nachkommastellen fest
Output.nd = 6;
**.lw-Funktion (number of decimals) legt die Set-Elemt-Bezeichner Beschränkung fest
Output.lw = 0;
**.pw-Funktion (pagewidth) legt die max. Anzahl Spalten fest
Output.pw = 32767;

*Deklarieren der Ausgabeparameter
$INCLUDE ./output/output.gms

*Ausgeben der Optimierungsergebnisse
$INCLUDE ./output/structure/csv_output.gms
