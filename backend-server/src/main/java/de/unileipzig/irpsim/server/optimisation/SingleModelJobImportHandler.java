package de.unileipzig.irpsim.server.optimisation;

import javax.persistence.EntityTransaction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.simulation.data.BackendParameters;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersSingleModel;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.server.data.simulationparameters.ScenarioImportHandler;

public class SingleModelJobImportHandler {
   
   private static final Logger LOG = LogManager.getLogger(SingleModelJobImportHandler.class);
   
   private final BackendParameters gp;
   private final OptimisationJobPersistent persistentJob;
   
   public SingleModelJobImportHandler(BackendParameters gp, OptimisationJobPersistent persistentJob) {
      this.gp = gp;
      this.persistentJob = persistentJob;
   }

   public void importScenarioTimeseries() throws JsonProcessingException, InterruptedException {
      final ScenarioImportHandler handler = new ScenarioImportHandler(gp, 0, true);
      final JSONParametersSingleModel changedParameters = handler.handleTimeseries();
      LOG.debug("Importierte Jahre: {}", changedParameters.getYears().size());
      setAndPersistGAMSParameters(changedParameters);
   }

   private void setAndPersistGAMSParameters(final JSONParametersSingleModel changedParameters) throws JsonProcessingException {
      persistentJob.setJsonParameter(Constants.MAPPER.writeValueAsString(changedParameters));

      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         OptimisationJobPersistent jobWithChangedParameters = em.merge(persistentJob);
         final EntityTransaction transaction = em.getTransaction();
         transaction.begin();
         em.persist(jobWithChangedParameters);
         transaction.commit();
      }
   }
}
