package de.unileipzig.irpsim.server.endpoints;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.data.simulationparameters.OptimisationScenario;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersMultiModel;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.ConnectedModelParameter;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.core.standingdata.data.StaticData;

public class Cleaner implements Runnable {

   private static final Logger LOG = LogManager.getLogger(Cleaner.class);

   private static final CleanState state = new CleanState();

   private boolean cleaning = true;

   /**
    * Löscht die Datensätze aus der Liste von IDs, die in dem übergebenen Parameter-String vorkommen
    * 
    * @param datensatzIds
    * @param parameterString
    * @throws IOException
    * @throws JsonParseException
    * @throws JsonMappingException
    */
   private void removeAllFromParameter(final Set<Integer> datensatzIds, final String parameterString, final String title, final boolean input)
         throws IOException, JsonParseException,
         JsonMappingException {
      try {
         final JSONParametersMultimodel parameters = Constants.MAPPER.readValue(parameterString, JSONParametersMultimodel.class);
         final BackendParametersMultiModel backendParameters = new BackendParametersMultiModel(parameters);
         for (ConnectedModelParameter modelParameters : backendParameters.getModels()) {
            LOG.debug(title); // Temporary debugging
            LOG.debug("Bearbeite {} Jahre: {}", title, modelParameters.getYeardata().length);
            for (int year = 0; year < modelParameters.getYeardata().length; year++) {
               final BackendParametersYearData yeardata = modelParameters.getYeardata()[year];
               removeYeardata(datensatzIds, title, input, year, yeardata);
            }
         }
      } catch (UnrecognizedPropertyException e) {
         LOG.info("Warning! Using deprecated single model data in {}", title);
         final JSONParametersMultimodel parameters = Constants.MAPPER.readValue(parameterString, JSONParametersMultimodel.class);
         final BackendParametersMultiModel backendParameters = new BackendParametersMultiModel(parameters);
         for (ConnectedModelParameter model : backendParameters.getModels()) {
            LOG.debug("Bearbeite {} Jahre: {}", title, model.getYeardata().length);
            for (int year = 0; year < model.getYeardata().length; year++) {
               final BackendParametersYearData yeardata = model.getYeardata()[year];
               removeYeardata(datensatzIds, title, input, year, yeardata);
            }
         }
      }
   }

   private void removeYeardata(final Set<Integer> datensatzIds, final String title, final boolean input, int year, final BackendParametersYearData yeardata) {
      if (yeardata != null) {
         final Set<Integer> references = yeardata.collectTimeseriesReferences(input).keySet();
         LOG.debug("Entferne Referenzen:  {} {}", references.size(), references);
         datensatzIds.removeAll(references);
         final Set<Integer> scalarReferences = yeardata.collectScalarReferences().keySet();
         datensatzIds.removeAll(scalarReferences);
      } else {
         LOG.trace("Achtung - {} hat null-Jahresdaten in Jahr {}", title, year);
      }
   }

   public void deleteNonReferencedData() {
      final Set<Integer> datensatzIds = getDatensatzIds();
      removeScenarioReferences(datensatzIds);
      removeJobReferences(datensatzIds);
      executeDeletion(datensatzIds);
   }

   private void removeJobReferences(final Set<Integer> datensatzIds) {
      try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final CriteriaBuilder cBuilder = em.getCriteriaBuilder();
         final CriteriaQuery<OptimisationJobPersistent> ojpQuery = cBuilder.createQuery(OptimisationJobPersistent.class);
         ojpQuery.from(OptimisationJobPersistent.class);
         final List<OptimisationJobPersistent> optJobs = em.createQuery(ojpQuery).getResultList();
         state.toAnalyze = optJobs.size();
         LOG.debug("optJobs: {}", optJobs);

         removeAllJobReferences(datensatzIds, optJobs);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   private void executeDeletion(final Set<Integer> datensatzIds) {
      try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         state.toDelete = datensatzIds.size();
         state.deleted = 0;

         deleteNonUsed(em, datensatzIds);
      }
   }

   private void removeScenarioReferences(final Set<Integer> datensatzIds) {
      try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final CriteriaBuilder cBuilder = em.getCriteriaBuilder();
         final CriteriaQuery<OptimisationScenario> osQuery = cBuilder.createQuery(OptimisationScenario.class);
         osQuery.from(OptimisationScenario.class);
         final List<OptimisationScenario> optimisationScenarios = em.createQuery(osQuery).getResultList();
         LOG.debug("optimisationScenarios: {}", optimisationScenarios);

         state.analyzed = 0;
         state.toAnalyze = optimisationScenarios.size();
         removeAllScenarioReferences(datensatzIds, optimisationScenarios);
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   private Set<Integer> getDatensatzIds() {
      final Set<Integer> datensatzIds;
      try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {

         final CriteriaBuilder cBuilder = em.getCriteriaBuilder();

         final CriteriaQuery<Integer> idQuery = cBuilder.createQuery(Integer.class);
         final Root<StaticData> sdRoot = idQuery.from(StaticData.class);
         idQuery.where(cBuilder.isNull(sdRoot.get("stammdatum"))).select(sdRoot.get("id"));
         datensatzIds = new HashSet<>(em.createQuery(idQuery).getResultList());
         LOG.debug("datensatzIds: {}", datensatzIds);
      }
      return datensatzIds;
   }

   private void removeAllScenarioReferences(final Set<Integer> datensatzIds, final List<OptimisationScenario> scenarios)
         throws IOException, JsonParseException, JsonMappingException {
      for (final OptimisationScenario scenario : scenarios) {
         removeAllFromParameter(datensatzIds, scenario.getData(), "Szenario " + scenario.getId(), true);
         state.analyzed++;
      }
   }

   private void removeAllJobReferences(final Set<Integer> datensatzIds, final List<OptimisationJobPersistent> jobs) throws IOException, JsonParseException, JsonMappingException {
      for (final OptimisationJobPersistent job : jobs) {
         removeAllFromParameter(datensatzIds, job.getJsonParameter(), "Job " + job.getId(), true);
         if (job.getJsonResult() != null) {
            removeAllFromParameter(datensatzIds, job.getJsonResult(), "Job " + job.getId() + " Ausgabedaten", false);
         }
         state.analyzed++;
      }
   }

   private void deleteNonUsed(final ClosableEntityManager em, final Set<Integer> datensatzIds) {
      final EntityTransaction et = em.getTransaction();
      et.begin();
      datensatzIds.forEach(id -> {
         state.deleted++;
         LOG.debug("Lösche Datensatz " + id);
         final StaticData datensatz = em.find(StaticData.class, id);
         final String table = datensatz.isInData() ? "series_data_in" : "series_data_out";
         em.remove(datensatz);
         em.createNativeQuery("DELETE FROM " + table + " WHERE seriesid=:id").setParameter("id", id).executeUpdate();
      });
      et.commit();
   }

   @Override
   public void run() {
      deleteNonReferencedData();
      LOG.info("Löschen beendet, Optimieren beginnt");
      optimizeDatabase();
      LOG.info("Aufräumen beendet");
      cleaning = false;
   }

   private void optimizeDatabase() {
      state.optimize = 0;
      try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         EntityTransaction transaction = em.getTransaction();
         transaction.begin();
         em.createNativeQuery("ALTER TABLE series_data_in DROP KEY series_in_index").executeUpdate();
         em.createNativeQuery("ALTER TABLE series_data_in DROP KEY series_in_index_timestamp").executeUpdate();
         transaction.commit();
      } catch (PersistenceException e) {
         e.printStackTrace();
      }
      state.optimize = 2;
      try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         EntityTransaction transaction = em.getTransaction();
         transaction.begin();
         em.createNativeQuery("CREATE INDEX series_in_index ON series_data_in(seriesid)").executeUpdate();
         em.createNativeQuery("CREATE INDEX series_in_index_timestamp ON series_data_in(seriesid, unixtimestamp)").executeUpdate();
         transaction.commit();

      } catch (PersistenceException e) {
         e.printStackTrace();
      }
      state.optimize = 3;
      try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         EntityTransaction transaction = em.getTransaction();
         transaction.begin();
         em.createNativeQuery("ALTER TABLE series_data_out DROP KEY series_data_out_index").executeUpdate();
         em.createNativeQuery("ALTER TABLE series_data_out DROP KEY series_data_out_index_timestamp").executeUpdate();
         transaction.commit();
      } catch (PersistenceException e) {
         e.printStackTrace();
      }
      state.optimize = 4;
      try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         EntityTransaction transaction = em.getTransaction();
         transaction.begin();
         em.createNativeQuery("CREATE INDEX series_data_out_index ON series_data_out(seriesid)").executeUpdate();
         em.createNativeQuery("CREATE INDEX series_data_out_index_timestamp ON series_data_out(seriesid, unixtimestamp)").executeUpdate();
         transaction.commit();
      } catch (PersistenceException e) {
         e.printStackTrace();
      }
      state.optimize = 5;
   }

   public CleanState getState() {
      return state;
   }

   public boolean isCleaning() {
      return cleaning;
   }

}