package de.unileipzig.irpsim.server.optimisation.queue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.unileipzig.irpsim.server.endpoints.CleanupEndpoint;
import de.unileipzig.irpsim.server.standingdata.endpoints.DataUploader;

public class TableOptimizer implements Runnable {

   public static final TableOptimizer OPTIMIZER = new TableOptimizer();

   // TODO TableOptimizer in eigene Klasse auslagern
   static {
      final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
      final long timeTillmidnight = LocalDateTime.now().until(LocalDate.now().plusDays(1).atStartOfDay(), ChronoUnit.MINUTES);
      scheduler.scheduleAtFixedRate(TableOptimizer.OPTIMIZER, timeTillmidnight, 24 * 60, TimeUnit.MINUTES);
   }

   private TableOptimizer() {

   }

   @Override
   public void run() {
      try {
         if (OptimisationJobHandler.getInstance().runningJobs.size() == 0 && !DataUploader.isActive()) {
            new CleanupEndpoint().startCleanup();
            OptimisationJobHandler.getInstance().startNextJob();
         }
      } catch (Throwable t) {
         t.printStackTrace();
      }
   }
}