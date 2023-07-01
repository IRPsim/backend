parameter par_DSM_DS_E_Cup(set_load_DS_E) maximal upward shift of load in percent of load;
parameter par_DSM_DS_E_Cdown(set_load_DS_E) maximal downward shift of load in percent of load;

POSITIVE VARIABLE var_DSM_down(set_ii_0,set_ii_0, set_pss) demand delayed in timestep ii;
POSITIVE VARIABLE var_DSM_down_sum(set_ii_0,set_pss) demand delayed in timestep ii;
POSITIVE VARIABLE var_DSM_up(set_ii_0, set_pss) demand served in timestep ii;
POSITIVE VARIABLE var_DSM_up_sum(set_ii_0, set_pss) demand served in timestep ii;
POSITIVE VARIABLE var_DSM_shift(set_pss) demand served in timestep ii;

***-----------------------------------------------------------------------------
***Gleichungen
***-----------------------------------------------------------------------------

EQUATIONS EqLoad_E1(set_ii,set_pss) Lastdeckungsgleichung elektrische Last;
EqLoad_E1(set_t,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1 and (par_DSM_DS_E_t(set_load_DS_E)=0 or par_DSM_DS_E_Cup(set_load_DS_E)=0))..
         - sum(set_fromPss,
         var_energyFlow(set_t,'E',set_fromPss,set_load_DS_E)$set_energyLink_opt('E',set_fromPss,set_load_DS_E))
         + sum(set_toPss,
         var_energyFlow(set_t,'E',set_load_DS_E,set_toPss)$set_energyLink_opt('E',set_load_DS_E,set_toPss))
         =e= par_L_DS_E(set_t,set_load_DS_E)*(-1);

***Umsetzung neue DSM a la DS_CL

EQUATIONS EqLoad_E1_1(set_ii_0,set_pss) Lastdeckungsgleichung mit Lastverschiebungsmoeglichkeit;
EqLoad_E1_1(set_t,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1
                                         AND par_DSM_DS_E_t(set_load_DS_E)>0 AND par_DSM_DS_E_Cup(set_load_DS_E)>0)..
         sum(set_fromPss, var_energyFlow(set_t,'E',set_fromPss,set_load_DS_E)$set_energyLink_opt('E',set_fromPss,set_load_DS_E))
         + var_DSM_down_sum(set_t,set_load_DS_E)
         =e=
         par_L_DS_E(set_t,set_load_DS_E)
         + var_DSM_up_sum(set_t,set_load_DS_E);

EQUATIONS EqLoad_E2_I(set_ii_0,set_pss) Lastdeckungsgleichung UP=DOWN Lastverschiebungsmoeglichkeit (erste Optimierungshaelfte);
EqLoad_E2_I(set_ii,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1
                                         AND par_DSM_DS_E_t(set_load_DS_E)>0 AND par_DSM_DS_E_Cup(set_load_DS_E)>0
                                         AND (ORD(set_ii) ge sca_numberStoring*sca_iterator+1)
                                         AND (ORD(set_ii) le sca_numberStoring*(sca_iterator+1)))..
         var_DSM_up(set_ii,set_load_DS_E)
         =e=
         sum(set_ii_duplicate$(ORD(set_ii_duplicate)>=sca_numberStoring*sca_iterator+1 AND ORD(set_ii_duplicate)>=ORD(set_ii)-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)
                                         AND ORD(set_ii_duplicate)<=ORD(set_ii)+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E) AND ORD(set_ii_duplicate)<=sca_numberStoring*(sca_iterator+1)),var_DSM_down(set_ii,set_ii_duplicate,set_load_DS_E));

EQUATIONS EqLoad_E2_II(set_ii_0,set_pss) Lastdeckungsgleichung UP=DOWN Lastverschiebungsmoeglichkeit Periodenanfang (zweite Optimierungshaelfte);
EqLoad_E2_II(set_ii,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1
                                         AND par_DSM_DS_E_t(set_load_DS_E)>0 AND par_DSM_DS_E_Cup(set_load_DS_E)>0
                                         AND (ORD(set_ii) ge sca_numberStoring*(sca_iterator+1)+1)
                                         AND (ORD(set_ii) le sca_numberStoring*(sca_iterator+2)))..
         var_DSM_up(set_ii,set_load_DS_E)
         =e=
         sum(set_ii_duplicate$(ORD(set_ii_duplicate)>=sca_numberStoring*(sca_iterator+1)+1 AND ORD(set_ii_duplicate)>=ORD(set_ii)-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)
                                         AND ORD(set_ii_duplicate)<=ORD(set_ii)+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E) AND ORD(set_ii_duplicate)<=sca_numberStoring*(sca_iterator+2)),var_DSM_down(set_ii,set_ii_duplicate,set_load_DS_E));

EQUATIONS EqLoad_E3(set_ii_0,set_pss) Summierung Lastverschiebung inkl. Regelenergievorhaltung;
EqLoad_E3(set_t,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1
                                         AND par_DSM_DS_E_t(set_load_DS_E)>0 AND par_DSM_DS_E_Cup(set_load_DS_E)>0)..
         var_DSM_up(set_t,set_load_DS_E)
         =e=
         var_DSM_up_sum(set_t,set_load_DS_E);
*         +SUM(set_toPss,var_energyFlow(set_t,'PR',set_load_DS_E,set_toPss)$set_energyLink_opt('PR',set_load_DS_E,set_toPss));

EQUATIONS EqLoad_E4_I(set_ii_0,set_pss) Summierung Lastreduktionen inklusive Regelenergievorhaltung Periodenanfang (erste Optimierungshaelfte);
EqLoad_E4_I(set_ii,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1
                                         AND par_DSM_DS_E_t(set_load_DS_E)>0 AND par_DSM_DS_E_Cup(set_load_DS_E)>0
                                         AND (ORD(set_ii) ge sca_numberStoring*sca_iterator+1)
                                         AND (ORD(set_ii) le sca_numberStoring*(sca_iterator+1)))..
          sum(set_ii_duplicate$(ORD(set_ii_duplicate)>=sca_numberStoring*sca_iterator+1 AND ORD(set_ii_duplicate)>=ORD(set_ii)-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)
                                         AND ORD(set_ii_duplicate)<=ORD(set_ii)+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E) AND ORD(set_ii_duplicate)<=sca_numberStoring*sca_iterator+sca_numberStoring),var_DSM_down(set_ii_duplicate,set_ii,set_load_DS_E))
          =e=
          var_DSM_down_sum(set_ii,set_load_DS_E);
*          + SUM(set_toPss,var_energyFlow(set_ii,'NR',set_load_DS_E,set_toPss)$set_energyLink_opt('NR',set_load_DS_E,set_toPss));

EQUATIONS EqLoad_E4_II(set_ii_0,set_pss) Summierung Lastreduktionen inklusive Regelenergievorhaltung Periodenanfang (zweite Optimierungshaelfte);
EqLoad_E4_II(set_ii,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1
                                         AND par_DSM_DS_E_t(set_load_DS_E)>0 AND par_DSM_DS_E_Cup(set_load_DS_E)>0
                                         AND (ORD(set_ii) ge sca_numberStoring*(sca_iterator+1)+1)
                                         AND (ORD(set_ii) le sca_numberStoring*(sca_iterator+2)))..
          sum(set_ii_duplicate$(ORD(set_ii_duplicate)>=sca_numberStoring*sca_iterator+1+sca_numberStoring AND ORD(set_ii_duplicate)>=ORD(set_ii)-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)
                                         AND ORD(set_ii_duplicate)<=ORD(set_ii)+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E) AND ORD(set_ii_duplicate)<=sca_numberStoring*sca_iterator+sca_numberStoring+sca_numberStoring),var_DSM_down(set_ii_duplicate,set_ii,set_load_DS_E))
          =e=
          var_DSM_down_sum(set_ii,set_load_DS_E);
*          + SUM(set_toPss,var_energyFlow(set_ii,'NR',set_load_DS_E,set_toPss)$set_energyLink_opt('NR',set_load_DS_E,set_toPss));


*EQUATIONS EqLoad_E4a_I(set_ii_0,set_pss) Summierung Lastreduktionen inklusive Regelenergievorhaltung Periodenanfang (erste Optimierungshaelfte);
*EqLoad_E4a_I(set_ii,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1
*                                         AND par_DSM_DS_E_t(set_load_DS_E)>0 AND par_DSM_DS_E_Cup(set_load_DS_E)>0
*                                         AND (ORD(set_ii) ge sca_numberStoring*sca_iterator+1)
*                                         AND (ORD(set_ii) le sca_numberStoring*sca_iterator+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)))..
*          sum(set_ii_duplicate$(ORD(set_ii_duplicate)>=sca_numberStoring*sca_iterator+1 AND ORD(set_ii_duplicate)>=ORD(set_ii)-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)
*                                         AND ORD(set_ii_duplicate)<=ORD(set_ii)+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E) AND ORD(set_ii_duplicate)<=sca_numberStoring*sca_iterator+sca_numberStoring),
*                                         var_DSM_down(set_ii_duplicate,set_ii,set_load_DS_E))
*          =e=
*          var_DSM_down_sum(set_ii,set_load_DS_E);
**          + SUM(set_toPss,var_energyFlow(set_ii,'NR',set_load_DS_E,set_toPss)$set_energyLink_opt('NR',set_load_DS_E,set_toPss));
*
*EQUATIONS EqLoad_E4a_II(set_ii_0,set_pss) Summierung Lastreduktionen inklusive Regelenergievorhaltung Periodenanfang (zweite Optimierungshaelfte);
*EqLoad_E4a_II(set_ii,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1
*                                         AND par_DSM_DS_E_t(set_load_DS_E)>0 AND par_DSM_DS_E_Cup(set_load_DS_E)>0
*                                         AND (ORD(set_ii) ge sca_numberStoring*sca_iterator+1+sca_numberStoring)
*                                         AND (ORD(set_ii) le sca_numberStoring*sca_iterator+sca_numberStoring+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)))..
*          sum(set_ii_duplicate$(ORD(set_ii_duplicate)>=sca_numberStoring*sca_iterator+1+sca_numberStoring AND ORD(set_ii_duplicate)>=ORD(set_ii)-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)
*                                         AND ORD(set_ii_duplicate)<=ORD(set_ii)+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E) AND ORD(set_ii_duplicate)<=sca_numberStoring*sca_iterator+sca_numberStoring+sca_numberStoring),var_DSM_down(set_ii_duplicate,set_ii,set_load_DS_E))
*          =e=
*          var_DSM_down_sum(set_ii,set_load_DS_E);
**          + SUM(set_toPss,var_energyFlow(set_ii,'NR',set_load_DS_E,set_toPss)$set_energyLink_opt('NR',set_load_DS_E,set_toPss));
*
*EQUATIONS EqLoad_E4m_I(set_ii_0,set_pss) Summierung Lastreduktionen inklusive Regelenergievorhaltung Periodenmitte (erste Optimierungshaelfte);
*EqLoad_E4m_I(set_ii,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1
*                                         AND par_DSM_DS_E_t(set_load_DS_E)>0 AND par_DSM_DS_E_Cup(set_load_DS_E)>0
*                                         AND (ORD(set_ii) ge sca_numberStoring*sca_iterator+1+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E))
*                                         AND (ORD(set_ii) le sca_numberStoring*sca_iterator+sca_numberStoring-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)))..
*          sum(set_ii_duplicate$(ORD(set_ii_duplicate)>=sca_numberStoring*sca_iterator+1 AND ORD(set_ii_duplicate)>=ORD(set_ii)-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)
*                                         AND ORD(set_ii_duplicate)<=ORD(set_ii)+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E) AND ORD(set_ii_duplicate)<=sca_numberStoring*sca_iterator+sca_numberStoring),var_DSM_down(set_ii_duplicate,set_ii,set_load_DS_E))
*          =e=
*          var_DSM_down_sum(set_ii,set_load_DS_E);
**          + SUM(set_toPss,var_energyFlow(set_ii,'NR',set_load_DS_E,set_toPss)$set_energyLink_opt('NR',set_load_DS_E,set_toPss));
*
*EQUATIONS EqLoad_E4m_II(set_ii_0,set_pss) Summierung Lastreduktionen inklusive Regelenergievorhaltung Periodenmitte (zweite Optimierungshaelfte);
*EqLoad_E4m_II(set_ii,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1
*                                         AND par_DSM_DS_E_t(set_load_DS_E)>0 AND par_DSM_DS_E_Cup(set_load_DS_E)>0
*                                         AND (ORD(set_ii) ge sca_numberStoring*sca_iterator+1+sca_numberStoring+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E))
*                                         AND (ORD(set_ii) le sca_numberStoring*sca_iterator+sca_numberStoring+sca_numberStoring-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)))..
*          sum(set_ii_duplicate$(ORD(set_ii_duplicate)>=sca_numberStoring*sca_iterator+1+sca_numberStoring AND ORD(set_ii_duplicate)>=ORD(set_ii)-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)
*                                         AND ORD(set_ii_duplicate)<=ORD(set_ii)+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E) AND ORD(set_ii_duplicate)<=sca_numberStoring*sca_iterator+sca_numberStoring+sca_numberStoring),var_DSM_down(set_ii_duplicate,set_ii,set_load_DS_E))
*          =e=
*          var_DSM_down_sum(set_ii,set_load_DS_E);
**          + SUM(set_toPss,var_energyFlow(set_ii,'NR',set_load_DS_E,set_toPss)$set_energyLink_opt('NR',set_load_DS_E,set_toPss));
*
*EQUATIONS EqLoad_E4e_I(set_ii_0,set_pss) Summierung Lastreduktionen inklusive Regelenergievorhaltung Periodenende (erste Optimierungshaelfte);
*EqLoad_E4e_I(set_ii,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1
*                                         AND par_DSM_DS_E_t(set_load_DS_E)>0 AND par_DSM_DS_E_Cup(set_load_DS_E)>0
*                                         AND (ORD(set_ii) ge sca_numberStoring*sca_iterator+1+sca_numberStoring-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E))
*                                         AND (ORD(set_ii) le sca_numberStoring*sca_iterator+sca_numberStoring))..
*          sum(set_ii_duplicate$(ORD(set_ii_duplicate)>=sca_numberStoring*sca_iterator+1 AND ORD(set_ii_duplicate)>=ORD(set_ii)-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)
*                                         AND ORD(set_ii_duplicate)<=ORD(set_ii)+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E) AND ORD(set_ii_duplicate)<=sca_numberStoring*sca_iterator+sca_numberStoring),var_DSM_down(set_ii_duplicate,set_ii,set_load_DS_E))
*          =e=
*          var_DSM_down_sum(set_ii,set_load_DS_E);
**          + SUM(set_toPss,var_energyFlow(set_ii,'NR',set_load_DS_E,set_toPss)$set_energyLink_opt('NR',set_load_DS_E,set_toPss));
*
*EQUATIONS EqLoad_E4e_II(set_ii_0,set_pss) Summierung Lastreduktionen inklusive Regelenergievorhaltung Periodenende (zweite Optimierungshaelfte);
*EqLoad_E4e_II(set_ii,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1
*                                         AND par_DSM_DS_E_t(set_load_DS_E)>0 AND par_DSM_DS_E_Cup(set_load_DS_E)>0
*                                         AND (ORD(set_ii) ge sca_numberStoring*sca_iterator+1+sca_numberStoring+sca_numberStoring-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E))
*                                         AND (ORD(set_ii) le sca_numberStoring*sca_iterator+sca_numberStoring+sca_numberStoring))..
*          sum(set_ii_duplicate$(ORD(set_ii_duplicate)>=sca_numberStoring*sca_iterator+1+sca_numberStoring AND ORD(set_ii_duplicate)>=ORD(set_ii)-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)
*                                         AND ORD(set_ii_duplicate)<=ORD(set_ii)+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E) AND ORD(set_ii_duplicate)<=sca_numberStoring*sca_iterator+sca_numberStoring+sca_numberStoring),var_DSM_down(set_ii_duplicate,set_ii,set_load_DS_E))
*          =e=
*          var_DSM_down_sum(set_ii,set_load_DS_E);
**          + SUM(set_toPss,var_energyFlow(set_ii,'NR',set_load_DS_E,set_toPss)$set_energyLink_opt('NR',set_load_DS_E,set_toPss));

EQUATIONS EqLoad_E5_I(set_ii_0,set_pss) Wiederherstellung Lastverschiebung Periodenanfang (erste Optimierungshaelfte);
EqLoad_E5_I(set_ii,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1
                                         AND par_DSM_DS_E_t(set_load_DS_E)>0 AND par_DSM_DS_E_Cup(set_load_DS_E)>0
                                         AND (ORD(set_ii) ge sca_numberStoring*sca_iterator+1)
                                         AND (ORD(set_ii) le sca_numberStoring*(sca_iterator+1)))..
          var_DSM_up(set_ii,set_load_DS_E)
          =l=
          par_DSM_DS_E_Cup(set_load_DS_E)*(sum(set_ii_duplicate$(ORD(set_ii_duplicate)>=sca_numberStoring*sca_iterator+1 AND ORD(set_ii_duplicate)>=ORD(set_ii)-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)
                                         AND ORD(set_ii_duplicate)<=ORD(set_ii)+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E) AND ORD(set_ii_duplicate)<=sca_numberStoring*sca_iterator+sca_numberStoring),par_L_DS_E(set_ii_duplicate,set_load_DS_E))-par_L_DS_E(set_ii, set_load_DS_E));

EQUATIONS EqLoad_E5_II(set_ii_0,set_pss) Wiederherstellung Lastverschiebung Periodenanfang (zweite Optimierungshaelfte);
EqLoad_E5_II(set_ii,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1
                                         AND par_DSM_DS_E_t(set_load_DS_E)>0 AND par_DSM_DS_E_Cup(set_load_DS_E)>0
                                         AND (ORD(set_ii) ge sca_numberStoring*(sca_iterator+1)+1)
                                         AND (ORD(set_ii) le sca_numberStoring*(sca_iterator+2)))..
          var_DSM_up(set_ii,set_load_DS_E)
          =l=
          par_DSM_DS_E_Cup(set_load_DS_E)*(sum(set_ii_duplicate$(ORD(set_ii_duplicate)>=sca_numberStoring*sca_iterator+1+sca_numberStoring AND ORD(set_ii_duplicate)>=ORD(set_ii)-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)
                                         AND ORD(set_ii_duplicate)<=ORD(set_ii)+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E) AND ORD(set_ii_duplicate)<=sca_numberStoring*sca_iterator+sca_numberStoring+sca_numberStoring),par_L_DS_E(set_ii_duplicate,set_load_DS_E))-par_L_DS_E(set_ii, set_load_DS_E));

*EQUATIONS EqLoad_E5a_I(set_ii_0,set_pss) Wiederherstellung Lastverschiebung Periodenanfang (erste Optimierungshaelfte);
*EqLoad_E5a_I(set_ii,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1
*                                         AND par_DSM_DS_E_t(set_load_DS_E)>0 AND par_DSM_DS_E_Cup(set_load_DS_E)>0
*                                         AND (ORD(set_ii) ge sca_numberStoring*sca_iterator+1)
*                                         AND (ORD(set_ii) le sca_numberStoring*sca_iterator+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)))..
*          var_DSM_up(set_ii,set_load_DS_E)
*          =l=
*          par_DSM_DS_E_Cup(set_load_DS_E)*(sum(set_ii_duplicate$(ORD(set_ii_duplicate)>=sca_numberStoring*sca_iterator+1 AND ORD(set_ii_duplicate)>=ORD(set_ii)-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)
*                                         AND ORD(set_ii_duplicate)<=ORD(set_ii)+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E) AND ORD(set_ii_duplicate)<=sca_numberStoring*sca_iterator+sca_numberStoring),par_L_DS_E(set_ii_duplicate,set_load_DS_E))-par_L_DS_E(set_ii, set_load_DS_E));
*
*EQUATIONS EqLoad_E5a_II(set_ii_0,set_pss) Wiederherstellung Lastverschiebung Periodenanfang (zweite Optimierungshaelfte);
*EqLoad_E5a_II(set_ii,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1
*                                         AND par_DSM_DS_E_t(set_load_DS_E)>0 AND par_DSM_DS_E_Cup(set_load_DS_E)>0
*                                         AND (ORD(set_ii) ge sca_numberStoring*sca_iterator+1+sca_numberStoring)
*                                         AND (ORD(set_ii) le sca_numberStoring*sca_iterator+sca_numberStoring+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)))..
*          var_DSM_up(set_ii,set_load_DS_E)
*          =l=
*          par_DSM_DS_E_Cup(set_load_DS_E)*(sum(set_ii_duplicate$(ORD(set_ii_duplicate)>=sca_numberStoring*sca_iterator+1+sca_numberStoring AND ORD(set_ii_duplicate)>=ORD(set_ii)-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)
*                                         AND ORD(set_ii_duplicate)<=ORD(set_ii)+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E) AND ORD(set_ii_duplicate)<=sca_numberStoring*sca_iterator+sca_numberStoring+sca_numberStoring),par_L_DS_E(set_ii_duplicate,set_load_DS_E))-par_L_DS_E(set_ii, set_load_DS_E));
*
*EQUATIONS EqLoad_E5m_I(set_ii_0,set_pss) Wiederherstellung Lastverschiebung Periodenmitte (erste Optimierungshaelfte);
*EqLoad_E5m_I(set_ii,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1
*                                         AND par_DSM_DS_E_t(set_load_DS_E)>0 AND par_DSM_DS_E_Cup(set_load_DS_E)>0
*                                         AND (ORD(set_ii) ge sca_numberStoring*sca_iterator+1+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E))
*                                         AND (ORD(set_ii) le sca_numberStoring*sca_iterator+sca_numberStoring-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)))..
*          var_DSM_up(set_ii,set_load_DS_E)
*          =l=
*          par_DSM_DS_E_Cup(set_load_DS_E)*(sum(set_ii_duplicate$(ORD(set_ii_duplicate)>=sca_numberStoring*sca_iterator+1 AND ORD(set_ii_duplicate)>=ORD(set_ii)-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)
*                                         AND ORD(set_ii_duplicate)<=ORD(set_ii)+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E) AND ORD(set_ii_duplicate)<=sca_numberStoring*sca_iterator+sca_numberStoring),par_L_DS_E(set_ii_duplicate,set_load_DS_E))-par_L_DS_E(set_ii, set_load_DS_E));
*
*EQUATIONS EqLoad_E5m_II(set_ii_0,set_pss) Wiederherstellung Lastverschiebung Periodenmitte (zweite Optimierungshaelfte);
*EqLoad_E5m_II(set_ii,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1
*                                         AND par_DSM_DS_E_t(set_load_DS_E)>0 AND par_DSM_DS_E_Cup(set_load_DS_E)>0
*                                         AND (ORD(set_ii) ge sca_numberStoring*sca_iterator+1+sca_numberStoring+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E))
*                                         AND (ORD(set_ii) le sca_numberStoring*sca_iterator+sca_numberStoring+sca_numberStoring-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)))..
*          var_DSM_up(set_ii,set_load_DS_E)
*          =l=
*          par_DSM_DS_E_Cup(set_load_DS_E)*(sum(set_ii_duplicate$(ORD(set_ii_duplicate)>=sca_numberStoring*sca_iterator+1+sca_numberStoring AND ORD(set_ii_duplicate)>=ORD(set_ii)-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)
*                                         AND ORD(set_ii_duplicate)<=ORD(set_ii)+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E) AND ORD(set_ii_duplicate)<=sca_numberStoring*sca_iterator+sca_numberStoring+sca_numberStoring),par_L_DS_E(set_ii_duplicate,set_load_DS_E))-par_L_DS_E(set_ii, set_load_DS_E));
*
*EQUATIONS EqLoad_E5e_I(set_ii_0,set_pss) Wiederherstellung Lastverschiebung Periodenmitte (erste Optimierungshaelfte);
*EqLoad_E5e_I(set_ii,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1
*                                         AND par_DSM_DS_E_t(set_load_DS_E)>0 AND par_DSM_DS_E_Cup(set_load_DS_E)>0
*                                         AND (ORD(set_ii) ge sca_numberStoring*sca_iterator+1+sca_numberStoring-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E))
*                                         AND (ORD(set_ii) le sca_numberStoring*sca_iterator+sca_numberStoring))..
*          var_DSM_up(set_ii,set_load_DS_E)
*          =l=
*          par_DSM_DS_E_Cup(set_load_DS_E)*(sum(set_ii_duplicate$(ORD(set_ii_duplicate)>=sca_numberStoring*sca_iterator+1 AND ORD(set_ii_duplicate)>=ORD(set_ii)-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)
*                                         AND ORD(set_ii_duplicate)<=ORD(set_ii)+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E) AND ORD(set_ii_duplicate)<=sca_numberStoring*sca_iterator+sca_numberStoring),par_L_DS_E(set_ii_duplicate,set_load_DS_E))-par_L_DS_E(set_ii, set_load_DS_E));
*
*EQUATIONS EqLoad_E5e_II(set_ii_0,set_pss) Wiederherstellung Lastverschiebung Periodenmitte (zweite Optimierungshaelfte);
*EqLoad_E5e_II(set_ii,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1
*                                         AND par_DSM_DS_E_t(set_load_DS_E)>0 AND par_DSM_DS_E_Cup(set_load_DS_E)>0
*                                         AND (ORD(set_ii) ge sca_numberStoring*sca_iterator+1+sca_numberStoring+sca_numberStoring-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E))
*                                         AND (ORD(set_ii) le sca_numberStoring*sca_iterator+sca_numberStoring+sca_numberStoring))..
*          var_DSM_up(set_ii,set_load_DS_E)
*          =l=
*          par_DSM_DS_E_Cup(set_load_DS_E)*(sum(set_ii_duplicate$(ORD(set_ii_duplicate)>=sca_numberStoring*sca_iterator+1+sca_numberStoring AND ORD(set_ii_duplicate)>=ORD(set_ii)-(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E)
*                                         AND ORD(set_ii_duplicate)<=ORD(set_ii)+(1/sca_delta_ii)*par_DSM_DS_E_t(set_load_DS_E) AND ORD(set_ii_duplicate)<=sca_numberStoring*sca_iterator+sca_numberStoring+sca_numberStoring),par_L_DS_E(set_ii_duplicate,set_load_DS_E))-par_L_DS_E(set_ii, set_load_DS_E));

EQUATIONS EqLoad_E6(set_ii_0,set_pss) Begrenzung Lastverschiebung;
EqLoad_E6(set_t,set_load_DS_E)$(set_pss_opt(set_load_DS_E) and par_X_pss_model(set_load_DS_E)=1
                                         AND par_DSM_DS_E_t(set_load_DS_E)>0 AND par_DSM_DS_E_Cup(set_load_DS_E)>0)..
         var_DSM_down_sum(set_t,set_load_DS_E)
         =l=
         par_DSM_DS_E_Cdown(set_load_DS_E)*par_L_DS_E(set_t, set_load_DS_E);


EQUATIONS EqLoad_G1(set_ii,set_pss) Lastdeckungsgleichung gasbezogene Last;
EqLoad_G1(set_t,set_load_DS_G)$(set_pss_opt(set_load_DS_G) and par_X_pss_model(set_load_DS_G)=1)..
         - sum(set_fromPss,
         var_energyFlow(set_t,'G',set_fromPss,set_load_DS_G)$set_energyLink_opt('G',set_fromPss,set_load_DS_G))
         + sum(set_toPss,
         var_energyFlow(set_t,'G',set_load_DS_G,set_toPss)$set_energyLink_opt('G',set_load_DS_G,set_toPss))
         =e= par_L_DS_G(set_t,set_load_DS_G)*(-1);


EQUATIONS EqLoad_W1(set_ii,set_pss) Lastdeckungsgleichung thermische Last;
EqLoad_W1(set_t,set_load_DS_W)$(set_pss_opt(set_load_DS_W) and par_X_pss_model(set_load_DS_W)=1)..
         - sum(set_fromPss,
         var_EnergyFlow(set_t,'W',set_fromPss,set_load_DS_W)$set_energyLink_opt('W',set_fromPss,set_load_DS_W))
         + sum(set_toPss,
         var_EnergyFlow(set_t,'W',set_load_DS_W,set_toPss)$set_energyLink_opt('W',set_load_DS_W,set_toPss))
         =e= par_L_DS_W(set_t,set_load_DS_W)*(-1);

EQUATIONS EqLoad_C1(set_ii,set_pss) Lastdeckungsgleichung thermische Last;
EqLoad_C1(set_t,set_load_DS_C)$(set_pss_opt(set_load_DS_C) and par_X_pss_model(set_load_DS_C)=1)..
         - sum(set_fromPss,
         var_EnergyFlow(set_t,'C',set_fromPss,set_load_DS_C)$set_energyLink_opt('C',set_fromPss,set_load_DS_C))
         + sum(set_toPss,
         var_EnergyFlow(set_t,'C',set_load_DS_C,set_toPss)$set_energyLink_opt('C',set_load_DS_C,set_toPss))
         =e= par_L_DS_C(set_t,set_load_DS_C)*(-1);


*EqLoad_E2_I, EqLoad_E2_II,
*EqLoad_E2a_I, EqLoad_E2a_II, EqLoad_E2m_I, EqLoad_E2m_II, EqLoad_E2e_I, EqLoad_E2e_II, EqLoad_E4a_I, EqLoad_E4a_II, EqLoad_E4m_I, EqLoad_E4m_II, EqLoad_E4e_I, EqLoad_E4e_II, EqLoad_E5a_I, EqLoad_E5a_II, EqLoad_E5m_I, EqLoad_E5m_II, EqLoad_E5e_I, EqLoad_E5e_II,
model mod_load_DS_orga /EqLoad_E1, EqLoad_E1_1, EqLoad_E2_I, EqLoad_E2_II, EqLoad_E3, EqLoad_E4_I, EqLoad_E4_II, EqLoad_E5_I, EqLoad_E5_II, EqLoad_E6,EqLoad_G1,EqLoad_W1,EqLoad_C1/;
model mod_load_DS_cust /EqLoad_E1, EqLoad_E1_1, EqLoad_E2_I, EqLoad_E2_II, EqLoad_E3, EqLoad_E4_I, EqLoad_E4_II, EqLoad_E5_I, EqLoad_E5_II, EqLoad_E6,EqLoad_G1,EqLoad_W1,EqLoad_C1/;



