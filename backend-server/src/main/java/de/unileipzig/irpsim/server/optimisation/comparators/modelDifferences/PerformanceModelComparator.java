package de.unileipzig.irpsim.server.optimisation.comparators.modelDifferences;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.core.simulation.data.persistence.State;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @source https://stackoverflow.com/questions/50967015/how-to-compare-json-documents-and-return-the-differences-with-jackson-or-gson
 */
final class FlatMapUtil {

   private FlatMapUtil() {
      throw new AssertionError("No instances for you!");
   }

   public static Map<String, Object> flatten(Map<String, Object> map) {
      return map.entrySet().stream()
            .flatMap(FlatMapUtil::flatten)
            .collect(LinkedHashMap::new, (m, e) -> m.put("/" + e.getKey(), e.getValue()), LinkedHashMap::putAll);
   }

   private static Stream<Map.Entry<String, Object>> flatten(Map.Entry<String, Object> entry) {

      if (entry == null) {
         return Stream.empty();
      }

      if (entry.getValue() instanceof Map<?, ?>) {
         return ((Map<?, ?>) entry.getValue()).entrySet().stream()
               .flatMap(e -> flatten(new AbstractMap.SimpleEntry<>(entry.getKey() + "/" + e.getKey(), e.getValue())));
      }

      if (entry.getValue() instanceof List<?>) {
         List<?> list = (List<?>) entry.getValue();
         return IntStream.range(0, list.size())
               .mapToObj(i -> new AbstractMap.SimpleEntry<String, Object>(entry.getKey() + "/" + i, list.get(i)))
               .flatMap(FlatMapUtil::flatten);
      }

      return Stream.of(entry);
   }
}

/**
 * Vergleicht die Input-Parameter Konfigurationen von zwei Jobs mit einander
 *
 * @author kluge
 */
public class PerformanceModelComparator {

   private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

   private List<PerformanceModel> performanceModels = new LinkedList<>();

   public PerformanceModelComparator(List<OptimisationJobPersistent> jobs) {
      initModels(jobs);

   }

   private void initModels(List<OptimisationJobPersistent> jobs) {
      for (OptimisationJobPersistent job : jobs) {
         String inputJson = job.getJsonParameter();
         Date end = job.getEnd();
         Date start = job.getStart();

         long diffInMillieSec = Math.abs(end.getTime() - start.getTime());
         long duration = TimeUnit.SECONDS.convert(diffInMillieSec, TimeUnit.MILLISECONDS);

         State state = job.getState();

         performanceModels.add(new PerformanceModel(job.getId(), inputJson, duration, state));
      }
   }

   private static final TypeReference<HashMap<String, Object>> TYPE = new TypeReference<>() {
   };

   public Map<Pair<Long, Long>, MapDifference<String, Object>> diff() throws JsonProcessingException {
      Map<Pair<Long, Long>, MapDifference<String, Object>> diffMap = new HashMap<>();
      for (PerformanceModel model1 : this.performanceModels) {
         for (PerformanceModel model2 : this.performanceModels) {
            if (!Objects.equals(model1.getJobId(), model2.getJobId())) {
               Pair<Long, Long> pair = Pair.of(model1.getJobId(), model2.getJobId());
               Pair<Long, Long> pair_reverse = Pair.of(model2.getJobId(), model1.getJobId());
               if (!diffMap.containsKey(pair) && !diffMap.containsKey(pair_reverse)) {
                  compareParameterSet(diffMap, model1, model2, pair);
               }
            }
         }
      }

      return diffMap;
   }

   private void compareParameterSet(Map<Pair<Long, Long>, MapDifference<String, Object>> diffMap, PerformanceModel model1,
         PerformanceModel model2, Pair<Long, Long> pair) throws JsonProcessingException, JsonMappingException {
      Map<String, Object> leftMap = MAPPER.readValue(model1.getInputJson(), TYPE);
      leftMap.put("Runtime_in_s", model1.getRuntime());
      leftMap.put("Jobstatus", model1.getState());
      Map<String, Object> rightMap = MAPPER.readValue(model2.getInputJson(), TYPE);
      rightMap.put("Runtime_in_s", model2.getRuntime());
      rightMap.put("Jobstatus", model2.getState());
      Map<String, Object> leftFlatMap = FlatMapUtil.flatten(leftMap);
      Map<String, Object> rightFlatMap = FlatMapUtil.flatten(rightMap);

      MapDifference<String, Object> diff = Maps.difference(leftFlatMap, rightFlatMap);
      diffMap.put(pair, diff);
   }
}
