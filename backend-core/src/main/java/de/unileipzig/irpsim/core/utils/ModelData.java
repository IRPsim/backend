package de.unileipzig.irpsim.core.utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

class ModelData {
   private final JSONObject outputDependencies;
   private final JSONObject inputDependencies;
   private final JSONObject setDependencies;
   private final Set<String> inputNames;

   public ModelData(JSONObject outputDependencies, JSONObject inputDependencies, JSONObject setDependencies, Set<String> inputNames) {
      this.outputDependencies = outputDependencies;
      this.inputDependencies = inputDependencies;
      this.setDependencies = setDependencies;
      this.inputNames = inputNames;
   }

   public JSONObject getOutputDependencies() {
      return outputDependencies;
   }

   public JSONObject getInputDependencies() {
      return inputDependencies;
   }

   public JSONObject getSetDependencies() {
      return setDependencies;
   }

   public Set<String> getInputNames() {
      return inputNames;
   }

   public List<String> getInputParameterDependencies(final String parameterName) {
      final List<String> sets = new LinkedList<>();
      final JSONObject parameter = inputDependencies.getJSONObject(parameterName);
      final JSONArray array = parameter.getJSONArray("dependencies");
      for (int i = 0; i < array.length(); i++) {
         sets.add(array.getString(i));
      }
      return sets;
   }
   
   public Map<String, List<String>> getAllInputDependencies() {
      Map<String, List<String>> allInputDependencies = new HashMap<>();
      for (String parameter : inputDependencies.keySet()) {
         final JSONObject parameterObject = inputDependencies.getJSONObject(parameter);
         final JSONArray array = parameterObject.getJSONArray("dependencies");
         
         List<String> sets = buildDependencyList(array);
         allInputDependencies.put(parameter, sets);
      }
      return allInputDependencies;
   }

   private List<String> buildDependencyList(final JSONArray array) {
      List<String> sets = new LinkedList<>();
      for (int i = 0; i < array.length(); i++) {
         sets.add(array.getString(i));
      }
      return sets;
   }

}