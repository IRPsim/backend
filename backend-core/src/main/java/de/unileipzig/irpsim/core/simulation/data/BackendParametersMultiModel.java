package de.unileipzig.irpsim.core.simulation.data;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import de.unileipzig.irpsim.core.simulation.data.json.JSONParameters;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.json.UserDefinedDescription;
import de.unileipzig.irpsim.core.simulation.data.json.YearData;

/**
 * GAMS-Parameter wrapper für gekoppelte Szenarien in für Backend gut lesbarer Form.
 *
 * @author kluge
 */
public class BackendParametersMultiModel implements GenericParameters {

   private UserDefinedDescription description = new UserDefinedDescription();
   private ConnectedModelParameter[] models;
   
   public BackendParametersMultiModel(int modelCount, int yearCount) {
      models = new ConnectedModelParameter[modelCount];
      for (int i = 0; i < modelCount; i++) {
         models[i] = new ConnectedModelParameter(yearCount);
      }
   }

   public BackendParametersMultiModel(JSONParametersMultimodel mmpJSON) {
      this.description = mmpJSON.getDescription();
      this.models = new ConnectedModelParameter[mmpJSON.getModels().size()];

      for (int i = 0; i < models.length; i++) {
         JSONParameters model = mmpJSON.getModels().get(i);
         if (model != null) {
            for (int j = 0; j < model.getYears().size(); j++) {
               YearData year = model.getYears().get(j);
               if (year != null) {
                  if (year.getConfig().getOptimizationlength() < year.getConfig().getSavelength()) {
                     throw new RuntimeException("Optimizationlenth (" + year.getConfig().getOptimizationlength() +
                           ") was smaller than savelength (" + year.getConfig().getSavelength() + ")");
                  }
               }
               models[i] = new ConnectedModelParameter(model);
            }
         }
      }
   }

   public UserDefinedDescription getDescription() {
      return description;
   }

   public void setDescription(UserDefinedDescription description) {
      this.description = description;
   }

   public ConnectedModelParameter[] getModels() {
      return models;
   }

   public void setModels(ConnectedModelParameter[] models) {
      this.models = models;
   }

   /**
    * Erzeugt GAMS-Daten.
    *
    * @return Die erzeugten JSON-Parameter als ConnectedModelParameterJSON
    */
   public final JSONParametersMultimodel createJSONParameters() {
      JSONParametersMultimodel mmpJson = new JSONParametersMultimodel();

      List<JSONParameters> cmpJson = new ArrayList<>(models.length);

      for (ConnectedModelParameter model : models) {
         if (model != null) {
            cmpJson.add(model.createJSONParameters());
         } else {
            cmpJson.add(null);
         }
      }

      mmpJson.setModels(cmpJson);
      mmpJson.setDescription(description);

      return mmpJson;
   }

   @Override
   public List<BackendParametersYearData> getAllYears() {
      List<BackendParametersYearData> result = new LinkedList<>();
      for (ConnectedModelParameter model : models) {
         for (BackendParametersYearData yeardata : model.getYeardata()) {
            result.add(yeardata);
         }
      }
      return result;
   }

   @Override
   public int fetchSimulationLength() {
      int sum = 0;
      for (ConnectedModelParameter model : models) {
         for (BackendParametersYearData yeardata : model.getYeardata()) {
            if (yeardata != null) {
               sum += yeardata.getConfig().getSimulationlength();
            }
         }
      }
      return sum;
   }

}
