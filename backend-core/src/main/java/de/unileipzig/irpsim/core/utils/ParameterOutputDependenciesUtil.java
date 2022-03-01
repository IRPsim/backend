package de.unileipzig.irpsim.core.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import de.unileipzig.irpsim.core.simulation.data.Calculation;
/**
 * Schnittstelle für Parameter und Namen bzgl. abhängigen Sets für Ausgebe-Parameter.
 *
 * @author kluge
 */
public class ParameterOutputDependenciesUtil {

   private static final Logger LOG = LogManager.getLogger(ParameterOutputDependenciesUtil.class);

   private static ParameterOutputDependenciesUtil instance;

   private static final ParameterBaseDependenciesUtil pdu = ParameterBaseDependenciesUtil.getInstance();
   /**
    * Singletonkonstruktor.
    */
   private ParameterOutputDependenciesUtil() {
   }

   public static ParameterOutputDependenciesUtil getInstance() {
      if (null == instance) {
         instance = new ParameterOutputDependenciesUtil();
      }
      return instance;
   }

   //nur vom Test aufgerufen
   /**
    * Gibt eine Liste mit allen Ausgabeparametern zurück.
    *
    * @return Die Ausgabeparameter als Liste.
    */
   public List<String> getAllOutputParameters(int modelDefinition) {
      JSONObject outputDependencies = pdu.getModelData(modelDefinition).getOutputDependencies();
      return new LinkedList<>(outputDependencies.keySet());
   }

   /**
    * Gibt den Namen des (ersten nicht-Zeitreihen-)Sets aus, von dem der Parameter abhängt.
    *
    * @param parameterName Name des Parameters.
    * @return Der Name des letzten sets im JSONArray als String, TODO.
    */
   public String getOutputSetName(final String parameterName, int modelDefinition) {
      final JSONObject parameterObject = pdu.getModelData(modelDefinition).getOutputDependencies().getJSONObject(parameterName);
      final JSONArray jsa = parameterObject.getJSONArray("dependencies");
      LOG.trace("JSA: " + jsa);
      return jsa.getString(jsa.length() - 1);
   }

   private JSONObject getOutputDependencies(int modelDefinition) {
      return pdu.getModelData(modelDefinition).getOutputDependencies();
   }

   public Map<String, List<Calculation>> getPostprocessings(int modelDefinition) {
      JSONObject currentDependencies = getOutputDependencies(modelDefinition);
      final Map<String, List<Calculation>> results = new HashMap<>();
      for (final Object parameterName : currentDependencies.keySet()) {
         final JSONObject jso = currentDependencies.getJSONObject((String) parameterName);

         final LinkedList<Calculation> values = new LinkedList<>();
         results.put((String) parameterName, values);
         if (!((String) parameterName).contains("Modelstat") && !((String) parameterName).contains("Solvestat")) {
            values.add(Calculation.OUTLINE);
         }
         if (jso.has("processing")) {
            final JSONArray processings = jso.getJSONArray("processing");
            for (int i = 0; i < processings.length(); i++) {
               values.add(Calculation.fetchCalculation(processings.getString(i)));
            }
         }
      }

      return results;
   }

   /**
    * Gibt eine Liste der Ausgabeparameter zurück, die *nicht* zu Jahres-Endergebnissen abgezinst zusammengefasst werden sollen. Das sind genau die Ausgabeparameter, die in den
    * Metadaten der Parameter mit overview gekennzeichnet sind.
    *
    * @return Eingeschränkte Liste der Ausgabeparameter
    */
   public Set<String> getNoOverviewData(int modelDefinition) {
      JSONObject currentDependencies = getOutputDependencies(modelDefinition);
      final Set<String> results = new HashSet<>();
      for (final Object parameterName : currentDependencies.keySet()) {
         final JSONObject jso = getOutputDependencies(modelDefinition).getJSONObject((String) parameterName);
         if (jso.has("overview")) {
            if (!jso.getBoolean("overview")) {
               results.add((String) parameterName);
            }
         }
      }

      return results;
   }

   public Map<String, List<String>> getAllOutputDependencies(int modelDefinition) {
      final Map<String, List<String>> allOutputDependencies = new HashMap<>();
      final List<String> allParameters = getAllOutputParameters(modelDefinition);
      allParameters.forEach(parameter -> {
         final List<String> dependencies = getOutputParameterDependencies(parameter, modelDefinition);
         allOutputDependencies.put(parameter, dependencies);
      });
      return allOutputDependencies;
   }

   /**
    * Gibt eine Liste der Setnamen zurück, von dem der übergebene Parameter abhängt.
    *
    * @param parameter Name des Parameters
    * @return Liste der Setnamen
    */
   public List<String> getOutputParameterDependencies(final String parameter, int modelDefinition) {
      final List<String> sets = new LinkedList<>();
      final JSONArray array = getOutputDependencies(modelDefinition).getJSONObject(parameter).getJSONArray("dependencies");
      for (int i = 0; i < array.length(); i++) {
         sets.add(array.getString(i));
      }

      return sets;
   }
}
