package de.unileipzig.irpsim.server.optimisation.comparators.modelDifferences;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.MapDifference;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Klasse für die Unterscheide zwischen den Parameter Einstellungen zweier Jobs.
 *
 * @author kluge
 */
public class ModelDifference {

   @JsonProperty("jobIdPair")
   @JsonDeserialize(using = PairDeserializer.class)
   public Pair<Long, Long> jobIdPair;
   @JsonProperty("leftMap")
   public Map<String, Object> leftMap;
   @JsonProperty("rightMap")
   public Map<String, Object> rightMap;
   @JsonProperty("differingMap")
   @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
   public Map<String, List<Object>> differingMap;

   /**
    * Jackson Konstruktor
    */
   @SuppressWarnings("unused")
   public ModelDifference(){

   }

   public ModelDifference(Pair<Long, Long> jobIdPair, MapDifference<String, Object> diffMap) {
      this.jobIdPair = jobIdPair;
      this.leftMap = diffMap.entriesOnlyOnLeft();
      this.rightMap = diffMap.entriesOnlyOnRight();

      this.differingMap = createDifferingMap(diffMap.entriesDiffering());
   }

   private Map<String, List<Object>> createDifferingMap(Map<String, MapDifference.ValueDifference<Object>> entriesDiffering) {
      Map<String, List<Object>> differingMap = new HashMap<>();
      for (Map.Entry<String, MapDifference.ValueDifference<Object>> entry: entriesDiffering.entrySet()){
         String key = entry.getKey();
         MapDifference.ValueDifference<Object> values = entry.getValue();
         Object leftValue = values.leftValue();
         Object rightValue = values.rightValue();
         LinkedList<Object> list = new LinkedList<>();
         list.add(leftValue);
         list.add(rightValue);
         differingMap.put(key, list);
      }
      return differingMap;
   }

   @JsonIgnore
   public int getDifferingMapSize(){
      return this.differingMap.size();
   }

   static class PairDeserializer extends JsonDeserializer<Pair<Long, Long>> {

      @Override
      public Pair<Long, Long> deserialize(JsonParser jsonParser,
            DeserializationContext deserializationContext) throws IOException {
         final TreeNode root = jsonParser.readValueAsTree();
         String key = root.fieldNames().next();
            return Pair.of(Long.parseLong(key), Long.parseLong(root.get(key).toString()));
      }
   }
}
