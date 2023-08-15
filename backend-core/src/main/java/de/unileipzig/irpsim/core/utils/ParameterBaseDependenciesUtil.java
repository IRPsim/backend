package de.unileipzig.irpsim.core.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Verwaltet die  Parameter und die Namen der abhängigen Sets für Ein- und Ausgebe-Parameter.
 * Darüber hinaus werden Nachberechnungsinformationen (postprocessing, overview) angegeben.
 *
 * @author reichelt
 */
public final class ParameterBaseDependenciesUtil {

   private static final Logger LOG = LogManager.getLogger(ParameterBaseDependenciesUtil.class);

   private static ParameterBaseDependenciesUtil instance;

   private final Map<Integer, ModelData> dependencies = new HashMap<Integer, ModelData>();

   /**
    * Singletonkonstruktor.
    */
   private ParameterBaseDependenciesUtil() {
   }

   public static ParameterBaseDependenciesUtil getInstance() {
      if (null == instance) {
         instance = new ParameterBaseDependenciesUtil();
      }
      return instance;
   }

   public void loadDependencies(final File alternativeDependencyFile, int modelId) {
      LOG.debug("Lades alternative Abhängigkeiten-Datei: {} Existiert: {}",
            alternativeDependencyFile.getAbsolutePath(), alternativeDependencyFile.exists());
      try {
         final InputStream is = new FileInputStream(alternativeDependencyFile);
         loadDependencies(is, modelId);
      } catch (final FileNotFoundException e) {
         LOG.error(e.getLocalizedMessage());
      }
   }

   /**
    * Liest die Abhängigen Sets bzw. Elemente aller Parameter aus und speichert sie intern in den outputDependencies.
    */
   public ModelData loadDependencies(int modeldefinition) {
      final InputStream is = getModelStream(modeldefinition);
      return loadDependencies(is, modeldefinition);
   }

   public InputStream getModelStream(int modeldefinition) {
      final String fileName = "/gams-dependencies-" + modeldefinition + ".json";
      LOG.debug("Lese: {}", fileName);
      final InputStream is = ParameterBaseDependenciesUtil.class.getResourceAsStream(fileName);
      return is;
   }

   private ModelData loadDependencies(final InputStream is, int modeldefinition) {
      try (final Scanner scanner = new Scanner(is)) {
         final String content = scanner.useDelimiter("\\Z").next();
         final JSONObject jsonObject = new JSONObject(content);
         final JSONObject outputDependencies = jsonObject.getJSONObject("output");
         final JSONObject inputDependencies = jsonObject.getJSONObject("input");
         final JSONObject setDependencies = jsonObject.getJSONObject("sets");
         final Set<String> inputNames = jsonObject.getJSONObject("input").keySet();

         ModelData modelData = new ModelData(outputDependencies, inputDependencies, setDependencies, inputNames);
         dependencies.put(modeldefinition, modelData);
         return modelData;
      }
   }

   /**
    * Liefert für die entsprechende modelDefinition geladene Abhängigkeiten zurück.
    * Nebeneffekt sind für diese modelDefinition noch keine Abhängigkeiten geladen werden diese zusätzlich geladen.
    *
    * @param modelDefinition Referenz für die Abhängigkeiten
    * @return geladene Abhängigkeiten
    */
   public ModelData getModelData(int modelDefinition) {
      ModelData modelData = dependencies.get(modelDefinition);
      if (modelData == null) {
         modelData = loadDependencies(modelDefinition);
      }
      return modelData;
   }

   /**
    * TODO
    *
    * @param name Name des Parameters.
    * @param modelDefinition Referenz für die Abhängigkeiten
    * @return
    */
   public List<String> getSubsets(final String name, int modelDefinition) {
      final List<String> values = new LinkedList<>();
      if (dependencies.get(modelDefinition).getSetDependencies() == null) {
         loadDependencies(1);
      }
      if (dependencies.get(modelDefinition).getSetDependencies().has(name)) {
         final JSONArray supersets = dependencies.get(modelDefinition).getSetDependencies().getJSONArray(name);
         supersets.forEach(value -> values.add(value.toString()));
      }
      return values;
   }

   /**
    * TODO
    *
    * @param parameter Name des Parameters.
    * @param modelDefinition Referenz für die Abhängigkeiten
    * @return
    */
   public List<String> getDependencies(String parameter, int modelDefinition) {
      List<String> parameterDependencies = null;
      if (!parameter.startsWith("par_out")) {
         try {
            ModelData currentModelData = getModelData(modelDefinition);
            parameterDependencies = currentModelData.getInputParameterDependencies(parameter);
         } catch (JSONException e) {
            LOG.debug("Problem beim Laden von " + modelDefinition);
            e.printStackTrace();
         }
      } else {
         try {
            parameterDependencies = ParameterOutputDependenciesUtil.getInstance()
                  .getOutputParameterDependencies(parameter, modelDefinition);
         } catch (JSONException e) {
            LOG.debug("Problem beim Laden von " + modelDefinition);
         }
      }
      return parameterDependencies;
   }
}
