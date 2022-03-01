package de.unileipzig.irpsim.server.optimisation.comparators.modelconnectors;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles Parameter which are syncable between Models.
 *
 * @author kluge
 *
 */
public class SyncableParameterHandler {

   private static SyncableParameterHandler instance;
   private static final Logger LOG = LogManager.getLogger(SyncableParameterHandler.class);

   private final Map<Pair<Integer, Integer>, Map<ConnectionType, SyncableParameter>> jointModelParameter;

   /**
    * GetInstance-Methode der Singleton-Klasse.
    *
    * @return Die erzeugte SyncableParameterHandler-Instanz
    */
   public static SyncableParameterHandler getInstance() {
      if (null == instance) {
         instance = new SyncableParameterHandler();
      }
      return instance;
   }

   public SyncableParameterHandler() {
      this.jointModelParameter = new HashMap<>();
   }

   public SyncableParameter fetchSyncableParameter(Pair<Integer, Integer> modelDefinitionCombination, ConnectionType type) {
      Map<ConnectionType, SyncableParameter> map = jointModelParameter.get(modelDefinitionCombination);
      SyncableParameter syncableParameter = null;
      if (map == null) {
         jointModelParameter.putIfAbsent(modelDefinitionCombination, new HashMap<>());
         syncableParameter = fetchAndCache(modelDefinitionCombination, type);
      } else {
         if (map.containsKey(type)) {
            syncableParameter = map.get(type);
         } else {
            syncableParameter = fetchAndCache(modelDefinitionCombination, type);
         }
      }
      return syncableParameter;
   }

   /**
    * Returns a map of ConnectionType to SyncableParameter.
    * If that's the first call for the specific modelDefinitionCombination,the parameter connections will be discovered,
    * and cached.
    *
    * @param modelDefinitionCombination Pair containing the model connection direction (key = oldModel, value = newModel)
    * @return Map<ConnectionType, SyncableParameter>
    */
   public Map<ConnectionType, SyncableParameter> fetchSyncableParameter(final Pair<Integer, Integer> modelDefinitionCombination) {
      Map<ConnectionType, SyncableParameter> map = jointModelParameter.get(modelDefinitionCombination);
      if (map == null) {
         ModelMatcher matcher = ModelMatcher.getInstance();
         SyncableParameter input = matcher.matchSyncableParameters(modelDefinitionCombination.getFirst(), modelDefinitionCombination.getSecond(), ConnectionType.INPUT);
         SyncableParameter output = matcher.matchSyncableParameters(modelDefinitionCombination.getFirst(), modelDefinitionCombination.getSecond(), ConnectionType.OUTPUT);
         SyncableParameter input_to_output = matcher.matchOutputToInputParameter(modelDefinitionCombination.getFirst(), modelDefinitionCombination.getSecond());
         map = new HashMap<>();
         map.put(ConnectionType.INPUT, input);
         map.put(ConnectionType.OUTPUT, output);
         map.put(ConnectionType.OUTPUT_TO_INPUT, input_to_output);
         jointModelParameter.put(modelDefinitionCombination, map);
      } else {
         if (!map.containsKey(ConnectionType.INPUT)){
            map.putIfAbsent(ConnectionType.INPUT, fetchSyncableParameter(modelDefinitionCombination, ConnectionType.INPUT));
         }
         if (!map.containsKey(ConnectionType.OUTPUT)) {
            map.putIfAbsent(ConnectionType.OUTPUT, fetchSyncableParameter(modelDefinitionCombination, ConnectionType.OUTPUT));
         }
         if (!map.containsKey(ConnectionType.OUTPUT_TO_INPUT)) {
            map.putIfAbsent(ConnectionType.OUTPUT_TO_INPUT, fetchSyncableParameter(modelDefinitionCombination, ConnectionType.OUTPUT_TO_INPUT));
         }
      }

      return map;
   }

   /**
    * Discovers parameter connections between models and caches the result.
    *
    * @param modelDefinitionCombination Pair containing the model connection direction (key = oldModel, value = newModel)
    * @param type modus of connection  see {@link ConnectionType}
    * @return SyncableParameter for the given {@link ConnectionType}
    */
   private SyncableParameter fetchAndCache(Pair<Integer, Integer> modelDefinitionCombination, ConnectionType type) {
      SyncableParameter syncableParameter = null;
      switch (type) {
         case OUTPUT_TO_INPUT: {
            syncableParameter = ModelMatcher.getInstance()
                  .matchOutputToInputParameter(modelDefinitionCombination.getFirst(), modelDefinitionCombination.getSecond());
            break;
         }
         case INPUT:
         case OUTPUT:{
             syncableParameter = ModelMatcher.getInstance()
                   .matchSyncableParameters(modelDefinitionCombination.getFirst(), modelDefinitionCombination.getSecond(), type);
            break;
         }
         default: {
            LOG.error("invalid Type for SyncableParameter");
         }
      }

      if (syncableParameter != null) {
         jointModelParameter.putIfAbsent(modelDefinitionCombination, new HashMap<>());
         Map<ConnectionType, SyncableParameter> joint = jointModelParameter.get(modelDefinitionCombination);
         joint.putIfAbsent(type, syncableParameter);
      }

      return syncableParameter;
   }

   /**
    * Connect only approved OptimisationYears, this will avoid connection 2 Years of IRPOpt
    *
    * @param previousModelDefinition ModelDefinition of the previous Year
    * @param currentModelDefinition ModelDefinition of the current Year
    * @return true if connection is approved, else false
    */
   public boolean isApprovedConnection(int previousModelDefinition, int currentModelDefinition) {
      boolean model5With1To3 = previousModelDefinition == 1 && currentModelDefinition == 3;
      boolean model3 = previousModelDefinition == 3 && currentModelDefinition == 3;
      return model5With1To3 || model3;
   }
}
