***-----------------------------------------------------------------------------
***Parameter
***-----------------------------------------------------------------------------

***-----------------------------------------------------------------------------
***Variablen
***----------------------------------------------------------------------------

VARIABLE x(set_t) amount of produced Cl over t;

***-----------------------------------------------------------------------------
***Gleichungen
***-----------------------------------------------------------------------------

EQUATION demand1 periodical supply equals periodical demand;
demand1..       D_Cl_tot =e= sum(set_t, x(set_t));

EQUATION supply1 electrolyzer upper_partial_load;
supply1(set_t)..    x(set_t) * S_eff =l= S_max;

EQUATION supply2 electrolyzer lower_partial_load;
supply2(set_t)..    x(set_t) * S_eff =g= S_min;

EQUATION ramping1 ramping up speed limit;
ramping1(set_t)..   (x(set_t+1) - x(set_t)) * S_eff =l= RU;

EQUATION ramping2 ramping down speed limit;
ramping2(set_t)..   (x(set_t-1) - x(set_t)) * S_eff =l= RD;


MODEL mod_tech_EY/ demand1, supply1, supply2, ramping1, ramping2 /;




