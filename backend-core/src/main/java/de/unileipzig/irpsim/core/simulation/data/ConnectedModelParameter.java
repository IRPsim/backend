package de.unileipzig.irpsim.core.simulation.data;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.unileipzig.irpsim.core.simulation.data.json.JSONParameters;
import de.unileipzig.irpsim.core.simulation.data.json.YearData;

/**
 * GAMS-Parameter in für Backend gut lesbarer Form.
 *
 * @author kluge
 */
public class ConnectedModelParameter {
   private int modeldefinition;
   private BackendParametersYearData[] yeardata;
   private PostProcessing postprocessing;

   /**
    * Erzeugt neue ConnectedModelParameterInstance aus Daten im Format {@link ConnectedParametersJSON}. Dabei werden für jedes Jahr die {@link BackendParametersYearData}
    * aus den entsprechenden {@link YearData} gebildet und ebenfalls alle weiteren Daten übernommen.
    *
    * @param cmpJson Die Daten im Format {@link ConnectedParametersJSON}
    */
   public ConnectedModelParameter(final JSONParameters cmpJson) {
      yeardata = new BackendParametersYearData[cmpJson.getYears().size()];
      for (int i = 0; i < yeardata.length; i++) {
         YearData year = cmpJson.getYears().get(i);
         if (year != null) {
            if (year.getConfig().getOptimizationlength() < year.getConfig().getSavelength()) {
               throw new RuntimeException("Optimizationlenth (" + year.getConfig().getOptimizationlength() +
                     ") was smaller than savelength (" + year.getConfig().getSavelength() + ")");
            }
            yeardata[i] = new BackendParametersYearData(year);
         }
      }
      postprocessing = cmpJson.getPostprocessing();
   }

   /**
    * Erstellt BackendParameters mit der angegebenen Jahresanzahl.
    *
    * @param years Jahresanzahl
    */
   public ConnectedModelParameter(final int years) {
      yeardata = new BackendParametersYearData[years];
      // for (int year = 0; year < years; year++) {
      // yeardata[year] = new BackendParametersYearData();
      // }
   }

   /**
    * Erzeugt GAMS-Daten.
    *
    * @return Die erzeugten JSON-Parameter als ConnectedModelParameterJSON
    */
   public final JSONParameters createJSONParameters() {
      JSONParameters cmpJson = new JSONParameters();

      List<YearData> years = new ArrayList<YearData>(yeardata.length);

      for (int i = 0; i < yeardata.length; i++) {
         if (yeardata[i] != null) {
            years.add(yeardata[i].createJSONParameters());
         } else {
            years.add(null);
         }
      }
      cmpJson.setYears(years);

      cmpJson.setPostprocessing(postprocessing);

      return cmpJson;
   }

   public int getModeldefinition() {
      return modeldefinition;
   }

   public void setModeldefinition(int modeldefinition) {
      this.modeldefinition = modeldefinition;
   }

   /**
    * Liefert BackendParameterYearData.
    *
    * @return Die Jahresdaten als BackendParameterYearData[]
    */
   public final BackendParametersYearData[] getYeardata() {
      return yeardata;
   }

   /**
    * Gibt die Gesamtlänge der Simulation über alle Jahre zurück.
    *
    * @return Gesamtlänge der Simulation über alle Jahre
    */
   @JsonIgnore
   public final int fetchSimulationLength() {
      int simulationYears = 0;
      for (BackendParametersYearData year : yeardata) {
         simulationYears += year == null ? 0 : 1;
      }
      return simulationYears * yeardata[0].getConfig().getSimulationlength();
   }

   public final PostProcessing getPostprocessing() {
      return postprocessing;
   }

   public final void setPostprocessing(final PostProcessing postprocessing) {
      this.postprocessing = postprocessing;
   }
}
