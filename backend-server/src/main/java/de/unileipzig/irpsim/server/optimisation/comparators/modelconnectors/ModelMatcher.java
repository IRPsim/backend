package de.unileipzig.irpsim.server.optimisation.comparators.modelconnectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unileipzig.irpsim.core.utils.ParameterInputDependenciesUtil;
import de.unileipzig.irpsim.core.utils.ParameterOutputDependenciesUtil;
import org.apache.commons.math3.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Klasse zur Ermittlung von SyncableParameter zwischen Modellen.
 *
 * @author kluge
 */
public class ModelMatcher {

   private static ModelMatcher instance;
   private static final Logger LOG = LogManager.getLogger(ModelMatcher.class);
   private static final ObjectMapper mapper = new ObjectMapper();

   private ModelMatcher() {

   }

   /**
    * GetInstance-Methode der Singleton-Klasse.
    *
    * @return Die erzeugte LoadDataHandler-Instanz
    */
   public static ModelMatcher getInstance() {
      if (null == instance) {
         instance = new ModelMatcher();
      }
      return instance;
   }

   public SyncableParameter matchSyncableParameters(int modelId1, int modelId2, ConnectionType type) {
      ParameterInputDependenciesUtil util = ParameterInputDependenciesUtil.getInstance();
      List<String> model1parameters = util.getAllInputParameters(modelId1);
      List<String> model2parameters = util.getAllInputParameters(modelId2);

      List<String> combinedList = new LinkedList<>(model1parameters);
      combinedList.retainAll(model2parameters);

      Map<DependencyType, List<String>> dependencyMap = new HashMap<>();
      for (String param : combinedList){
         DependencyType dependencyTypeModel1 = GetDependencyType.getDependencyType(param, true, modelId1);
         DependencyType dependencyTypeModel2 = GetDependencyType.getDependencyType(param, true, modelId2);
         if(dependencyTypeModel1.equals(dependencyTypeModel2)){
            if (dependencyMap.containsKey(dependencyTypeModel1)){
               dependencyMap.get(dependencyTypeModel1).add(param);
            } else {
               List<String> list = new LinkedList<String>();
               list.add(param);
               dependencyMap.put(dependencyTypeModel1, list);
            }
         }
      }

      return new SyncableParameter(dependencyMap, type, new Pair<>(modelId1, modelId2));
   }

   public SyncableParameter matchOutputToInputParameter(int outputModelID, int inputModelID) {
      ParameterInputDependenciesUtil util = ParameterInputDependenciesUtil.getInstance();
      ParameterOutputDependenciesUtil outUtil = ParameterOutputDependenciesUtil.getInstance();
      List<String> model1parameters = outUtil.getAllOutputParameters(outputModelID);
      List<String> model2parameters = util.getAllInputParameters(inputModelID);

      model1parameters.replaceAll(x -> x.replace("_out_", "_"));
      List<String> combinedList = new LinkedList<>(model1parameters);
      combinedList.retainAll(model2parameters);

      Map<DependencyType, List<String>> dependencyMap = new HashMap<>();
      for (String param : combinedList){
         DependencyType dependencyTypeInput = GetDependencyType.getDependencyType(param, true, inputModelID);
         String paramOut = param.replace("par_", "par_out_");
         DependencyType dependencyTypeOutput = GetDependencyType.getDependencyType(paramOut, false, outputModelID);
         if (dependencyTypeOutput.equals(dependencyTypeInput)){
            if (dependencyMap.containsKey(dependencyTypeOutput)){
               dependencyMap.get(dependencyTypeOutput).add(param);
            } else {
               List<String> list = new LinkedList<>();
               list.add(param);
               dependencyMap.put(dependencyTypeOutput, list);
            }
         }
      }

      return new SyncableParameter(dependencyMap, ConnectionType.OUTPUT_TO_INPUT, new Pair<>(outputModelID, inputModelID));
   }

}
