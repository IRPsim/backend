package de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RequestedParametersYear {

   private int simulationid;

   private int year;
   
   private int modelindex;

   private Map<String, Combination> parameters;

   public int getSimulationid() {
      return simulationid;
   }

   public void setSimulationid(final int simulationid) {
      this.simulationid = simulationid;
   }

   public int getYear() {
      return year;
   }

   public void setYear(final int year) {
      this.year = year;
   }
   
   public int getModelindex() {
      return modelindex;
   }

   public void setModelindex(int modelindex) {
      this.modelindex = modelindex;
   }

   public Map<String, Combination> getParameters() {
      return parameters;
   }

   public void setParameters(final Map<String, Combination> parameters) {
      this.parameters = parameters;
   }

   public static void main(String[] args) throws JsonProcessingException {
      RequestedParametersYear requestedParametersYear = new RequestedParametersYear();
      requestedParametersYear.setYear(0);
      requestedParametersYear.setSimulationid(9);
      Map<String, Combination> parameters = new HashMap<>();
      Combination combination = new Combination();
      combination.setSet_ii("Viertelstundenwerte");
      parameters.put("var_energyFlow", combination);
      requestedParametersYear.setParameters(parameters);
      combination.setCombinations(new LinkedList<>());
      combination.getCombinations().add(Arrays.asList(new String[] { "EGrid", "E" }));
      System.out.println(new ObjectMapper().writeValueAsString(requestedParametersYear));
   }
}
