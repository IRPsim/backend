package de.unileipzig.irpsim.server.modelstart;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.Globalconfig;
import de.unileipzig.irpsim.core.utils.PersistenceFolderUtil;
import de.unileipzig.irpsim.gams.OptimisationFolderUtil;
import de.unileipzig.irpsim.server.optimisation.ImportResultHandler;
import de.unileipzig.irpsim.server.optimisation.OptimisationJobUtils;
import de.unileipzig.irpsim.server.optimisation.ResultFileData;
import de.unileipzig.irpsim.server.optimisation.ResultFileInfo;
import de.unileipzig.irpsim.server.optimisation.postprocessing.PostProcessorHandler;

public class GAMSModelStarter extends ModelStarter {
   
   private static final Logger LOG = LogManager.getLogger(GAMSModelStarter.class);

   public GAMSModelStarter(final long jobid, int yearIndex, int modelindex, int modeldefinition) {
      super(jobid, modelindex, yearIndex);
      OptimisationFolderUtil.prepareGAMSDirectory(yearFolder, modeldefinition);
   }

   @Override
   public void parameterize() throws IOException, InterruptedException {
      runMain("de.unileipzig.irpsim.gams.GAMSParameterizer", parameterizeLog);
      if (Thread.currentThread().isInterrupted()) {
         return;
      }
      LOG.info("Parametrisierung beendet, Exit-Code: {} ", lastExitCode);
   }
   
   @Override
   public void startOptimisation() throws IOException, InterruptedException {
      runMain("de.unileipzig.irpsim.gams.GAMSStarter", optimizeLog);
      if (Thread.currentThread().isInterrupted()) {
         return;
      }
      LOG.info("Berechnung beendet, Exit-Code: {} ", lastExitCode);
   }

   private void runMain(String mainClass, File outputFile) throws IOException, InterruptedException {
      final String javaLibPath = System.getProperty("java.library.path");
      final String[] command = new String[] { "java",
            "-Djava.library.path=" + javaLibPath,
            "-cp",
            "backend-gams-0.2-SNAPSHOT.jar:target/backend-gams-0.2-SNAPSHOT.jar",
            mainClass,
            "" + jobid,
            "" + modelindex,
            "" + yearIndex };
      runProcess(outputFile, command);
   }

   protected ProcessBuilder buildProzess(final String[] command) throws IOException {
      final ProcessBuilder pb = new ProcessBuilder(command);

      createEnvironmentVariables(pb);
      addDatabase(pb);
      pb.environment().put(Constants.IRPSIM_PERSISTENCEFOLDER, PersistenceFolderUtil.getPersistenceFolder().getAbsolutePath());

      LOG.info(pb.command());
      return pb;
   }

   private void addDatabase(final ProcessBuilder pb) throws IOException {
      if (!pb.environment().containsKey(Constants.IRPSIM_DATABASE_URL)) {
         final File file = new File("url.txt");
         LOG.debug("Suche nach: {}", file.getAbsolutePath());
         final String url = FileUtils.readFileToString(file, Charset.defaultCharset()).replaceAll("\n", "");
         pb.environment().put(Constants.IRPSIM_DATABASE_URL, url);
      }
   }

   private void createEnvironmentVariables(final ProcessBuilder pb) {
      for (final Map.Entry<String, String> entry : System.getenv().entrySet()) {
         final String key = entry.getKey().contains("_TEST") ? entry.getKey().replace("_TEST", "") : entry.getKey();
         if (key.startsWith("IRPSIM")) {
            LOG.debug(key + " " + entry.getValue());
         }
         pb.environment().put(key, entry.getValue());
      }
   }

   @Override
   public File getListingFile() {
      return new File(yearFolder, "_gams_java_gjo1.lst");
   }

   @Override
   public File getGDXParameterFile() {
      return new File(yearFolder, "gamsirpsim.gdx");
   }

   @Override
   public File getGDXResultFile() {
      return new File(yearFolder, "irpsimresult.gdx");
   }
   
   /**
    * Berechnet die Endergebnisse aus den CSV-Dateien, wobei Datenreihen direkt in die Datenbank importiert werden.
    *
    * @param yearIndex Das Jahr dessen Jahresdaten importiert werden sollen
    */
   @Override
   public BackendParametersYearData getResult(final PostProcessorHandler postProcessor, Globalconfig config) {
      final BackendParametersYearData yeardata = new BackendParametersYearData();
      yeardata.setConfig(config);
      final ImportResultHandler importhandler = new ImportResultHandler(yeardata);

      final List<ResultFileInfo> fileinfos = OptimisationJobUtils.getInstance().getResultFiles(getYearWorkspace());

      final int size = fileinfos.size();
      LOG.info("Lese {} Dateiinfos", size);
      for (final ResultFileInfo fileinfo : fileinfos) {
         readResultFile(importhandler, fileinfo, config);
      }
      if (Thread.currentThread().isInterrupted()) {
         return null;
      }
      importhandler.executeImports();
      yeardata.setPostprocessing(postProcessor.fetchPostprocessingResults());
      LOG.info("Lesen beendet");
      
      return yeardata;
   }
   
   private void readResultFile(final ImportResultHandler importhandler, final ResultFileInfo fileinfo, Globalconfig config) {
      LOG.debug("Ergebnisse: {} {}", fileinfo.getIndex(), fileinfo.getFile());
      final ResultFileData data = OptimisationJobUtils.getInstance().readWholeFile(fileinfo.getFile(), config.getSavelength());
      LOG.trace("Lines: {}", data.getLines().size());

      if (!OptimisationJobUtils.importReadLines(importhandler, data)) {
         LOG.error("Fehler in Datei: {}", fileinfo.getFile().getAbsolutePath());
      }
   }
}
