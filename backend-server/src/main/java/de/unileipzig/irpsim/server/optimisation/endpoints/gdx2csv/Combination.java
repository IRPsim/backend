package de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv;

import java.util.List;

public class Combination {
   private String set_ii;
   private List<List<String>> combinations;

   public String getSet_ii() {
      return set_ii;
   }

   public void setSet_ii(String set_ii) {
      this.set_ii = set_ii;
   }

   public List<List<String>> getCombinations() {
      return combinations;
   }

   public void setCombinations(List<List<String>> combinations) {
      this.combinations = combinations;
   }
}