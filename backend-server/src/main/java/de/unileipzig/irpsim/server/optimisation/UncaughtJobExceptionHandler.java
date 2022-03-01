package de.unileipzig.irpsim.server.optimisation;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Date;

import javax.persistence.EntityTransaction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.core.simulation.data.persistence.State;

final class UncaughtJobExceptionHandler implements UncaughtExceptionHandler {

   private static final Logger LOG = LogManager.getLogger(UncaughtJobExceptionHandler.class);
   
   /**
    * 
    */
   private final Job optimisationJob;

   /**
    * @param optimisationJob
    */
   UncaughtJobExceptionHandler(Job optimisationJob) {
      this.optimisationJob = optimisationJob;
   }

   @Override
   public void uncaughtException(final Thread t, final Throwable e) {
      LOG.info("State: {}, Exception: {}", this.optimisationJob.getState().name(), e.getLocalizedMessage());
      e.printStackTrace();
      if (this.optimisationJob.getState() != State.INTERRUPTED && this.optimisationJob.getState()  != State.ABORTED) {
         this.optimisationJob.setError(true);
         this.optimisationJob.setState(State.ERROR);
      }
      updateDBJob(e.getMessage());
   }

   private void updateDBJob(final String message) {

      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final OptimisationJobPersistent persistentJob = em.find(OptimisationJobPersistent.class, this.optimisationJob.getJobid());
         persistentJob.setEnd(new Date());

         if (this.optimisationJob.hasError && this.optimisationJob.getState()  != State.ABORTED) {
            persistentJob.setError(true);
            persistentJob.setErrorMessage(message);
         }

         final EntityTransaction transaction = em.getTransaction();
         transaction.begin();
         em.unwrap(Session.class).saveOrUpdate(persistentJob);
         transaction.commit();
      } catch (final Throwable e2) {
         e2.printStackTrace();
      }
   }

}