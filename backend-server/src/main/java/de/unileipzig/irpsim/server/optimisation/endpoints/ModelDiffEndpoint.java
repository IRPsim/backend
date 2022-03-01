package de.unileipzig.irpsim.server.optimisation.endpoints;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.collect.MapDifference;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.server.optimisation.comparators.modelDifferences.ModelDifference;
import de.unileipzig.irpsim.server.optimisation.comparators.modelDifferences.PerformanceModelComparator;
import de.unileipzig.irpsim.server.optimisation.queue.OptimisationJobHandler;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.unileipzig.irpsim.server.data.Responses.badRequestResponse;
import static java.util.Comparator.comparingInt;

/**
 * Bietet Funktionalitäten zum Abruf von Performance Model Analysen.
 *
 * @author kluge
 */
@Path("modeldifference")
@Api(value = "/modeldifference", tags = "Model Difference")
public class ModelDiffEndpoint {

   private static final Logger LOG = LogManager.getLogger(ModelDiffEndpoint.class);
   private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

   @PUT
   @Path("/jobIds")
   @Produces(MediaType.APPLICATION_JSON)
   @ApiOperation(value = "Liefert Unterschiede in Json Job Konfigurationen zurück", notes = "Holt Jobs von der Datenbank und evaluiert Unterschiede.")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Bad Request") })
   public Response getJsonDifferences(final List<Integer> jobIds){
      List<OptimisationJobPersistent> persistentJobs = getOptimisationJobPersistents(jobIds);
      if (persistentJobs.isEmpty() || persistentJobs.size() == 1){
         return badRequestResponse("Simulationsläufe existieren nicht", "Simulationsstatus");
      }

      PerformanceModelComparator pmc = new PerformanceModelComparator(persistentJobs);
      Map<Pair<Long, Long>, MapDifference<String, Object>> diffMap;
      try {
         diffMap = pmc.diff();
      } catch (JsonProcessingException e) {
         e.printStackTrace();
         return badRequestResponse("Vergleichs Map konnte nicht erstellt werden ", "Simulationsstatus");
      }
      final String result;
      List<ModelDifference> modelDifferences = new LinkedList<>();
      try {
          for (Map.Entry<Pair<Long, Long>, MapDifference<String, Object>> entry: diffMap.entrySet()) {
             modelDifferences.add(new ModelDifference(entry.getKey(), entry.getValue()));
         }
         result = MAPPER.writeValueAsString(modelDifferences);
      } catch (JsonProcessingException e) {
         e.printStackTrace();
         return badRequestResponse("Vergleichs-Map konnte nicht serialisiert werden", "Simulationsstatus");
      }
      return Response.ok(result).build();
   }

   private List<OptimisationJobPersistent> getOptimisationJobPersistents(List<Integer> jobIds) {
      List<OptimisationJobPersistent> persistentJobs = new LinkedList<>();
      for (int jobId : jobIds){
         final OptimisationJobPersistent persistentJob = OptimisationJobHandler.getInstance().getJob(jobId);
         if (persistentJob != null) {
            persistentJobs.add(persistentJob);
         }
      }
      return persistentJobs;
   }

   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @ApiOperation(value = "Liefert <Anzahl von> Jobs zurück", notes = "Holt Job number ")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok") })
   @Path("/jobIds/{numberOfJobs}")
   public Response getJobs(@DefaultValue("10") @QueryParam("numberOfJobs") final long numberOfJobs){

      final JSONArray jsa = new JSONArray();
      OptimisationJobHandler.getInstance().getPersistedJobs().forEach(job -> jsa.put(job.getId()));
      LOG.info("Gespeicherte Optimierungen, IDs: {}", jsa);
      final JSONArray jsaLimited = new JSONArray();
      for (int i = jsa.length()-1; i > (jsa.length() -1 ) - numberOfJobs && i > 0; i--) {
         Object jso = jsa.get(i);
         if (jso != null){
            jsaLimited.put(jso);
         }
      }
      LOG.info("Gespeicherte limitierte Optimierungen, IDs: {}", jsaLimited);
      return Response.status(Response.Status.OK).entity(jsaLimited.toString()).build();
   }

   @PUT
   @Produces(MediaType.APPLICATION_JSON)
   @ApiOperation(value = "Liefert <Anzahl von> Jobs zurück", notes = "Holt Job number ")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok") })
   @Path("/jobIds/highdiffering")
   public Response findHighDifferingPairs(final List<Integer> jobIds){
      List<OptimisationJobPersistent> persistentJobs = getOptimisationJobPersistents(jobIds);
      if (persistentJobs.isEmpty() || persistentJobs.size() == 1){
         return badRequestResponse("Simulationsläufe existieren nicht", "Simulationsstatus");
      }

      PerformanceModelComparator pmc = new PerformanceModelComparator(persistentJobs);
      Map<Pair<Long, Long>, MapDifference<String, Object>> diffMap;
      try {
         diffMap = pmc.diff();
      } catch (JsonProcessingException e) {
         e.printStackTrace();
         return badRequestResponse("Vergleichs-Map konnte nicht erstellt werden ", "Simulationsstatus");
      }
      final String result;
      List<ModelDifference> modelDifferences = new LinkedList<>();

      for (Map.Entry<Pair<Long, Long>, MapDifference<String, Object>> entry: diffMap.entrySet()) {
         modelDifferences.add(new ModelDifference(entry.getKey(), entry.getValue()));
      }

      List<ModelDifference> sorted = modelDifferences.stream()
            .sorted(comparingInt(ModelDifference::getDifferingMapSize).reversed())
            .collect(Collectors.toList());
      for (ModelDifference modelDifference: sorted) {
         System.out.println(modelDifference.jobIdPair.toString()+ " : " + modelDifference.differingMap.size());
      }

      try {
         result = MAPPER.writeValueAsString(modelDifferences);
      } catch (JsonProcessingException e) {
         e.printStackTrace();
         return badRequestResponse("Vergleichs-Map konnte nicht serialisiert werden", "Simulationsstatus");
      }

      return Response.ok(result).build();
   }
}
