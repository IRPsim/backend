package de.unileipzig.irpsim.core.simulation.data.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.unileipzig.irpsim.core.simulation.data.Globalconfig;

/**
 * POJO für MultiModel-GAMS-Daten in dem für die UI gut nutzbaren Format.
 *
 * @author kluge
 */
public class JSONParametersMultimodel {

   private UserDefinedDescription description = new UserDefinedDescription();
   private List<JSONParameters> models = new ArrayList<>();

   public JSONParametersMultimodel() {

   }

   public JSONParametersMultimodel(int modelCount) {
      models.add(new JSONParameters());
   }

   public final UserDefinedDescription getDescription() {
      return description;
   }

   public final void setDescription(final UserDefinedDescription description) {
      this.description = description;
   }

   public final List<JSONParameters> getModels() {
      return models;
   }

   public final void setModels(final List<JSONParameters> models) {
      this.models = models;
   }

   @JsonIgnore
   public int getModeldefinition() {
      if (models.size() != 1) {
         return 5;
      } else {
         JSONParameters model = models.get(0);
         YearData year0 = model.getYears().get(0);
         return year0.getConfig().getModeldefinition();
      }
   }

   @JsonIgnore
   public Globalconfig fetchConfig() {
      // TODO Auto-generated method stub
      return models.get(0).getYears().get(0).getConfig();
   }

}
