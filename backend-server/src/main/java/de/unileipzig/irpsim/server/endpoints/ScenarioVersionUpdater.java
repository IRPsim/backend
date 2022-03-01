package de.unileipzig.irpsim.server.endpoints;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.data.simulationparameters.OptimisationScenario;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersSingleModel;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

public class ScenarioVersionUpdater {
   
   private static final Logger LOG = LogManager.getLogger(ScenarioVersionUpdater.class);
   
   public static void update() {
      try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final Session session = (Session) em.getDelegate();

         CriteriaBuilder builder = session.getCriteriaBuilder();
         CriteriaQuery<OptimisationJobPersistent> criteria = builder.createQuery(OptimisationJobPersistent.class);
         Root<OptimisationJobPersistent> queryRoot = criteria.from(OptimisationJobPersistent.class);
         criteria.select(queryRoot);

         final List<OptimisationJobPersistent> jobs  = session.createQuery(criteria).list();
         for (OptimisationJobPersistent job : jobs) {
            updateJobParameters(session, job);
            updateJobResults(session, job);
         }

         CriteriaQuery<OptimisationScenario> criteriaScenario = builder.createQuery(OptimisationScenario.class);
         Root<OptimisationScenario> scenarioRoot = criteriaScenario.from(OptimisationScenario.class);
         criteriaScenario.select(scenarioRoot);
         final List<OptimisationScenario> scenarios =  session.createQuery(criteriaScenario).list();
         for (OptimisationScenario scenario : scenarios) {
            updateScenarioParameters(session, scenario);
         }
      }
   }

   private static void updateScenarioParameters(Session session, OptimisationScenario job) {
      try {
         String parameterString = job.getData();
         JSONParametersMultimodel multiModel = getMultimodelParameters(parameterString);
         String convertedDataString = Constants.MAPPER.writeValueAsString(multiModel);
         Transaction transaction = session.beginTransaction();
         job.setData(convertedDataString);
         session.update(job);
         transaction.commit();
      } catch (Throwable t) {
         LOG.info("No Single Model JSON Scenario: " + job.getId());

         // Dirty hack
         String parameterString = job.getData();
         if (parameterString != null) {
            String updatedParameter = removeDescriptions(parameterString);
            Transaction transaction = session.beginTransaction();
            job.setData(updatedParameter);
            session.update(job);
            transaction.commit();
         }
      }

   }

   private static void updateJobParameters(Session session, OptimisationJobPersistent job) {
      try {
         String parameterString = job.getJsonParameter();
         JSONParametersMultimodel multiModel = getMultimodelParameters(parameterString);
         String convertedDataString = Constants.MAPPER.writeValueAsString(multiModel);
         Transaction transaction = session.beginTransaction();
         job.setJsonParameter(convertedDataString);
         session.update(job);
         transaction.commit();
      } catch (Throwable t) {
         LOG.info("No Single Model JSON Job: " + job.getId());
        
         //Dirty hack
         String parameterString = job.getJsonParameter();
         if (parameterString != null) {
            String updatedParameter = removeDescriptions(parameterString);
            Transaction transaction = session.beginTransaction();
            job.setJsonParameter(updatedParameter);
            session.update(job);
            transaction.commit();
         }
      }
   }

   private static JSONParametersMultimodel getMultimodelParameters(String parameterString) throws JsonProcessingException, JsonMappingException {
      JSONParametersSingleModel singleModelParameters = Constants.MAPPER.readValue(parameterString, JSONParametersSingleModel.class);
      JSONParametersMultimodel multiModel = new JSONParametersMultimodel();
      multiModel.getModels().add(singleModelParameters);
      multiModel.setDescription(singleModelParameters.getDescription());
      singleModelParameters.setDescription(null);
      return multiModel;
   }

   private static void updateJobResults(Session session, OptimisationJobPersistent job) {
      try {
         String resultString = job.getJsonResult();
         JSONParametersMultimodel multiModel = getMultimodelParameters(resultString);
         String convertedDataString = Constants.MAPPER.writeValueAsString(multiModel);
         Transaction transaction = session.beginTransaction();
         job.setJsonResult(convertedDataString);
         session.update(job);
         transaction.commit();
      } catch (Throwable t) {
         LOG.info("No Single Model JSON Job: " + job.getId());

         // Dirty hack - delete result descriptions which have been created by accident
         String resultString = job.getJsonResult();
         if (resultString != null) {
            String updatedResult = removeDescriptions(resultString);
            Transaction transaction = session.beginTransaction();
            job.setJsonResult(updatedResult);
            session.update(job);
            transaction.commit();
         }
      }
   }

   public static String removeDescriptions(String resultString) {
      JSONObject json = new JSONObject(resultString);
      JSONArray models = (JSONArray) json.get("models");
      for (int i = 0; i < models.length(); i++) {
         LOG.debug("Removing description: " + i);
         JSONObject model = (JSONObject) models.get(i);
         model.remove("description");
      }
      String updatedResult = json.toString();
      return updatedResult;
   }
   
   public static void main(String[] args) throws JsonMappingException, JsonProcessingException {
      String test = "{\"models\": [{\"description\": {\"test\": \"test2\"}, \"years\":  [{\"config\": null, \"sets\": {\"set_X\": null}} ]}]}";
      System.out.println(removeDescriptions(test));
      Constants.MAPPER.readValue(removeDescriptions(test), JSONParametersMultimodel.class);
   }
}
