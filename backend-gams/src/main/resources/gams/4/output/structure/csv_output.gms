*-------------------------------------------------------------------------------
* ##Deklarieren##
*-------------------------------------------------------------------------------
*LOOP((set_a, set_side_opt(set_side)),

    PUT Output;

    PUT_UTILITY 'ren' / 'output/results/a0.ii1.output.csv';



    LOOP(set_t,

         PUT "# ";

         PUT "set_t" "; ";

*-------------------------------------------------------------------------------
* ##Header NPV##
*-------------------------------------------------------------------------------
         PUT "sca_total_cost" "; ";
         PUT "par_EY_power""("set_t.tl"); "; 

         PUT /;

         PUT set_t.tl"; ";

*-------------------------------------------------------------------------------
* ##Werte NPV##
*-------------------------------------------------------------------------------

         PUT sca_total_cost"; ";
         PUT par_EY_power(set_t)"; ";


         PUT /;
         PUT /;

    );
*);



*
*
*
*
*
*
*
*LOOP((set_a),
*
*    PUT Output_accounting_customermdl;
*
*    PUT_UTILITY 'ren' / 'output/results/' set_a.tl:0 '.' set_optsteps.tl:0  '.output_accounting_customermdl.csv';
*
*    LOOP(set_t$set_t_store(set_t),
*
*         PUT "# ";
*
*         PUT "set_a" "; ";
*
*         PUT "set_t" "; ";
*
**-------------------------------------------------------------------------------
** ##Header NPV##
**-------------------------------------------------------------------------------
*
*         PUT "par_out_IuO_Sector_Cust""("set_t.tl"); ";
*
*
*         PUT /;
*
*         PUT set_a.tl"; "
*
*         PUT set_t.tl"; ";
*
**-------------------------------------------------------------------------------
** ##Werte NPV##
**-------------------------------------------------------------------------------
*
*         PUT par_out_IuO_Sector_Cust(set_t)"; ";
*
*
*         PUT /;
*         PUT /;
*
*    );
*);
*
