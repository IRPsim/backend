package de.unileipzig.irpsim.server.optimisation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.server.endpoints.CleanupEndpoint;
import de.unileipzig.irpsim.server.optimisation.queue.OptimisationJobHandler;

public class OptimisationYearHandler {
   private static final Logger LOG = LogManager.getLogger(OptimisationYearHandler.class);

   private static final List<OptimisationYear> waitingYears = new LinkedList<>();
   private static final List<OptimisationYear> runningYears = new LinkedList<>();
   private static final Map<OptimisationYear, Thread> yearThreads = new HashMap<>();

   public static void addYear(final OptimisationYear year) {
      LOG.debug("Füge Jahr hinzu: {}", year);
      waitingYears.add(year);

      final Thread runYearThread = new Thread(year);
      yearThreads.put(year, runYearThread);
      tryStartNewYear();
   }

   public static void yearFinished(final OptimisationYear year) {
      LOG.info("Year {}-{} finished, waiting: {} running: {}", year.getJobid(), year.getYearIndex(), waitingYears.size(), runningYears.size());
      yearThreads.remove(year);
      runningYears.remove(year);
      tryStartNewYear();
   }

   public static void tryStartNewYear() {
      synchronized (runningYears) {
         if (waitingYears.size() > 0 && canTakeMoreJobs() && !CleanupEndpoint.isCleaning()) {
            int index = 0;
            boolean started = false;
            while (!started && index < waitingYears.size()) {
               final OptimisationYear year = waitingYears.get(index);
               started = startYear(year);
               index++;
            }
         }
      }
   }

   private static boolean startYear(final OptimisationYear year) {
      if (year.isStartable()) {
         LOG.info("Starte Jahr: {} Wartend: {}", year, waitingYears.size());
         final Thread runYearThread = yearThreads.get(year);
         runYearThread.start();
         runningYears.add(year);
         waitingYears.remove(year);
         LOG.debug("Wartend: {}", waitingYears.size());
         return true;
      } else {
         return false;
      }
   }

   public static boolean canTakeMoreJobs() {
      LOG.info("Running Years: {}", runningYears);
      return runningYears.size() < OptimisationJobHandler.maxParallelJobs;
   }

   public static void kill(OptimisationYear year) {
      boolean removed = waitingYears.remove(year);
      if (!removed) {
         final Thread thread = yearThreads.get(year);
         if (thread != null)
            thread.interrupt();
         else
            LOG.info("Tried to kill a thread that didn't exist");
         removed = runningYears.remove(year);
         if (!removed) {
            LOG.info("Tried to remove year {} from waiting and running years, but was not findable", year);
         }
      }
   }

   public static void join(final OptimisationYear year) throws InterruptedException {
      LOG.info("Warte auf: " + year);
      final Thread thread = yearThreads.get(year);
      while (thread != null && !thread.isAlive()) {
         LOG.info("Warte auf {}", thread);
         Thread runningThread = yearThreads.get(runningYears.get(0));
         runningThread.join(60000);
      }
      if (thread != null) {
         thread.join();
      }
      LOG.debug("Jahr beendet");
   }
}
