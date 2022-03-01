package de.unileipzig.irpsim.server.optimisation;

import javax.persistence.EntityTransaction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersMultiModel;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.server.data.simulationparameters.MultiScenarioImportHandler;

public class JobImportHandler {
   
   private static final Logger LOG = LogManager.getLogger(JobImportHandler.class);
   
   private final BackendParametersMultiModel gp;
   private final OptimisationJobPersistent persistentJob;
   
   public JobImportHandler(BackendParametersMultiModel gp, OptimisationJobPersistent persistentJob) {
      this.gp = gp;
      this.persistentJob = persistentJob;
   }

   public void importScenarioTimeseries() throws JsonProcessingException, InterruptedException {
      final MultiScenarioImportHandler handler = new MultiScenarioImportHandler(gp, 0, true);
      final JSONParametersMultimodel changedParameters = handler.handleTimeseries();
      LOG.debug("Importierte Jahre: {}", changedParameters.getModels().get(0).getYears().size());
      setAndPersistGAMSParameters(changedParameters);
   }

   private void setAndPersistGAMSParameters(final JSONParametersMultimodel changedParameters) throws JsonProcessingException {
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
