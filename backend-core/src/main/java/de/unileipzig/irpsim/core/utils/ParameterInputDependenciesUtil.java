package de.unileipzig.irpsim.core.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Schnittstelle für Parameter und Namen bzgl. abhängigen Sets für Eingabe-Parameter.
 *
 * @author kluge
 */
public class ParameterInputDependenciesUtil {
   private static final Logger LOG = LogManager.getLogger(ParameterInputDependenciesUtil.class);

   private static ParameterInputDependenciesUtil instance;

   private static final ParameterBaseDependenciesUtil pdu = ParameterBaseDependenciesUtil.getInstance();
   /**
    * Singletonkonstruktor.
    */
   private ParameterInputDependenciesUtil() {
   }

   public static ParameterInputDependenciesUtil getInstance() {
      if (null == instance) {
         instance = new ParameterInputDependenciesUtil();
      }
      return instance;
   }

   // Nur von Test aufgerufen
   public Set<String> getJSONInputs(int modelDefinition) {
      return pdu.getModelData(modelDefinition).getInputNames();
   }

   /**
    * Gibt eine Liste mit allen Ausgabeparametern zurück.
    *
    * @return Die Ausgabeparameter als Liste.
    */
   public List<String> getAllInputParameters(int modelDefinition) {
      JSONObject inputDependencies = pdu.getModelData(modelDefinition).getInputDependencies();
      return new LinkedList<>(inputDependencies.keySet());
   }

   public List<String> getInputSetNames(final String parameterName, int modelDefinition) {
      JSONObject inputDependencies = pdu.getModelData(modelDefinition).getInputDependencies();
      final JSONObject parameterObject = inputDependencies.getJSONObject(parameterName);
      final JSONArray jsa = parameterObject.getJSONArray("dependencies");
//      LOG.debug("JSA: " + jsa);

      final List<String> inputSetNames = new ArrayList<>();

      final String inputSetName = jsa.getString(jsa.length() - 1);

      // add input set name
      inputSetNames.add(inputSetName);

      // find all sets which depends on inputSetName
      final Iterator<?> keys = pdu.getModelData(modelDefinition).getSetDependencies().keys();
      while (keys.hasNext()) {
         final String setNameKey = (String) keys.next();
         JSONObject setDependencies = pdu.getModelData(modelDefinition).getSetDependencies();
         if (setDependencies.get(setNameKey) instanceof JSONArray) {
            final JSONArray setDepArray = setDependencies.getJSONArray(setNameKey);
            for (int si = 0; si < setDepArray.length(); si++) {
               if (setDepArray.getString(si).equals(inputSetName)) {
                  inputSetNames.add(setNameKey);
               }
            }
         }
      }

      return inputSetNames;
   }

   public List<String> getInputTableNames(final String parameterName, int modelDefinition) {
      final JSONObject parameterObject = getInputDependencies(modelDefinition).getJSONObject(parameterName);
      final JSONArray jsa = parameterObject.getJSONArray("dependencies");
      final List<String> inputSetNames = new ArrayList<>(jsa.length());
      for (int i = 0; i < jsa.length(); i++) {
         inputSetNames.add(jsa.getString(i));
      }
      return inputSetNames;
   }

   /**
    * Gibt die Einheit für einen Parameter zurück. Wird zum Erstellen des Excel Deckblattes benötigt.
    *
    * @param parameterName Name des Parameters.
    * @return Einheit des Parameters.
    */
   public String getUnit(final String parameterName, int modelDefinition) {
      final JSONObject parameterObject = getInputDependencies(modelDefinition).getJSONObject(parameterName);
      return parameterObject.getString("unit");
   }

   /**
    * Gibt die Domain Regeln zu einem Parameter zurück. Wird zum Erstellen des Excel Deckblattes benötigt.
    *
    * @param parameterName Name des Parameters.
    * @return Key-Value Paare, z.B. ">=" : 20
    */
   public Map<String, BigDecimal> getDomain(final String parameterName, int modelDefinition) {
      final JSONObject parameterObject = getInputDependencies(modelDefinition).getJSONObject(parameterName);
      final JSONObject domain = parameterObject.getJSONObject("domain");

      final Map<String, BigDecimal> result = new HashMap<>();

      for (final String s : domain.keySet()) {
         result.put(s, domain.getBigDecimal(s));
      }
      return result;
   }

   public JSONObject getInputDependencies(int modelDefinition) {
      return pdu.getModelData(modelDefinition).getInputDependencies();
   }

   public Map<String, List<String>> getAllInputDependencies(int modelDefinition) {
      return pdu.getModelData(modelDefinition).getAllInputDependencies();
   }

   public List<String> getInputParameterDependencies(String parameterName, int modelDefinition) {
      return pdu.getModelData(modelDefinition).getInputParameterDependencies(parameterName);
   }

}
