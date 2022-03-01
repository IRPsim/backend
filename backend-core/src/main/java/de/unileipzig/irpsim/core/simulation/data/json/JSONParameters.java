package de.unileipzig.irpsim.core.simulation.data.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.unileipzig.irpsim.core.simulation.data.Globalconfig;
import de.unileipzig.irpsim.core.simulation.data.PostProcessing;

public class JSONParameters {

   private List<YearData> years = new ArrayList<>();
   private PostProcessing postprocessing = new PostProcessing();

   public final List<YearData> getYears() {
      return years;
   }

   public final void setYears(final List<YearData> years) {
      this.years = years;
   }

   public final PostProcessing getPostprocessing() {
      return postprocessing;
   }

   public final void setPostprocessing(final PostProcessing postprocessing) {
      this.postprocessing = postprocessing;
   }
   /**
    * @return Die {@link Globalconfig} des ersten Jahres.
    */
   @JsonIgnore
   public final Globalconfig fetchConfig() {
      return years.get(0).getConfig();
   }
}
