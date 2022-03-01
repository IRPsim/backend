package de.unileipzig.irpsim.server.modelstart;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import de.unileipzig.irpsim.core.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.Globalconfig;
import de.unileipzig.irpsim.core.utils.PersistenceFolderUtil;
import de.unileipzig.irpsim.server.optimisation.postprocessing.PostProcessorHandler;

public abstract class ModelStarter {
   
   private static final Logger LOG = LogManager.getLogger(ModelStarter.class);
   
   protected final long jobid;
   protected final int modelindex;
   protected final int yearIndex;
   protected final File yearFolder;
   protected final File parameterizeLog;
   protected final File optimizeLog;
   
   protected Process process;
   protected int lastExitCode = 0;
   
   public ModelStarter(final long jobid, int modelIndex, int yearIndex) {
      this.jobid = jobid;
      this.modelindex = modelIndex;
      yearFolder = PersistenceFolderUtil.getWorkspaceFolder(jobid, modelIndex, yearIndex);
      this.yearIndex = yearIndex;
      yearFolder.mkdirs();
      parameterizeLog = new File(yearFolder, "java_parameterize.txt");
      optimizeLog = new File(yearFolder, "java_optimize.txt");
   }
   
   public int getLastExitCode() {
      return lastExitCode;
   }

   public abstract void parameterize() throws IOException, InterruptedException;

   public abstract void startOptimisation() throws IOException, InterruptedException;

   /**
    * Beendet den Parametrisierungsjob.
    */
   public void kill() {
      LOG.debug("Empfange kill: {}", process);
      if (process != null) {
         LOG.info("Beende Prozess");
         process.destroyForcibly();
         LOG.info("Prozess beendet");
      }
   }
   
   protected abstract ProcessBuilder buildProzess(final String[] command) throws IOException;

   protected void runProcess(File outputFile, final String[] command) throws IOException, InterruptedException {
      final ProcessBuilder pb = buildProzess(command);

      pb.redirectErrorStream(true);
      pb.redirectOutput(outputFile);

      if (Thread.currentThread().isInterrupted()) {
         return;
      }
      process = pb.start();
      LOG.info("Process started. JobId: {}, ProcessID: {} Model: {}", jobid, process.pid(), modelindex);
      process.waitFor();
      lastExitCode = process.exitValue();
   }

   public abstract File getListingFile();

   public abstract File getGDXParameterFile();

   public abstract File getGDXResultFile();
   
   public File getSQLiteFile() {
      return new File(yearFolder, "sqlite.sql");
   }

   public File getOptimizeLog() {
      return optimizeLog;
   }

   public File getParameterizeLog() {
      return parameterizeLog;
   }

   public File getYearWorkspace() {
      return yearFolder;
   }

   public abstract BackendParametersYearData getResult(PostProcessorHandler postProcessorHandler, Globalconfig config);

}