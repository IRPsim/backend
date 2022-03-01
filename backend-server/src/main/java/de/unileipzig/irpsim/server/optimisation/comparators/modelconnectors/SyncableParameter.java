package de.unileipzig.irpsim.server.optimisation.comparators.modelconnectors;

import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;

/**
 * Verwaltet alle Parameter die in der #modelDefinitionCombination enthalten sind.
 *
 */
public class SyncableParameter {

   private final Map<DependencyType, List<String>> parameterMap;
   private final ConnectionType type;
   private final Pair<Integer, Integer> modelDefinitionCombination;

   public SyncableParameter(Map<DependencyType, List<String>> parameterMap, ConnectionType type, Pair<Integer, Integer> modelDefinitionCombination) {
      this.parameterMap = parameterMap;
      this.type = type;
      this.modelDefinitionCombination = modelDefinitionCombination;
   }

   public Map<DependencyType, List<String>> getParameterMap() {
      return parameterMap;
   }

   public ConnectionType getType() {
      return type;
   }

   public Pair<Integer, Integer> getModelDefinitionCombination() {
      return modelDefinitionCombination;
   }
}
