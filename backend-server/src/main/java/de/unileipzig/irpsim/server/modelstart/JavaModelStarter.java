package de.unileipzig.irpsim.server.modelstart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.SQLException;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.Globalconfig;
import de.unileipzig.irpsim.core.simulation.data.json.YearData;
import de.unileipzig.irpsim.server.optimisation.json2sqlite.TransformJSON;
import de.unileipzig.irpsim.server.optimisation.postprocessing.PostProcessorHandler;

public class JavaModelStarter extends ModelStarter {

   public static String getJarName() {
      return JAR_NAME;
   }

   public static File getModelPath() {
      return MODEL_PATH;
   }

   private static final String JAR_NAME = "IRPact-1.0-SNAPSHOT-uber.jar";
   private static final File MODEL_PATH = new File(System.getenv("IRPACT_PATH") != null ? System.getenv("IRPACT_PATH") : "../../irpact/build/libs/");

   private File inputFile;
   private File outputFile;
   private File logFile;
   private BackendParametersYearData yearParameters;

   public JavaModelStarter(final long jobid, int yearIndex, int modeldefinition, BackendParametersYearData yearParameters) {
      super(jobid, modeldefinition, yearIndex);
      this.yearParameters = yearParameters;
      
      inputFile = new File(yearFolder, "input_" + yearIndex + ".json");
      outputFile = new File(yearFolder, "output_" + yearIndex + ".json");
      logFile = new File(yearFolder, "log_"+ yearIndex+".log");
   }
   
   @Override
   public void parameterize() throws IOException, InterruptedException {
      Constants.MAPPER.writeValue(inputFile, yearParameters.createJSONParameters());
   }

   @Override
   public void startOptimisation() throws IOException, InterruptedException {
      File jar = new File(MODEL_PATH, JAR_NAME);
      String[] command = new String[] {"java", "-Xmx2g", "-jar", 
            jar.getAbsolutePath(), 
            "-i", inputFile.getAbsolutePath(),
            "-o", outputFile.getAbsolutePath(),
            "--logPath", logFile.getAbsolutePath(),
      };
      
      runProcess(optimizeLog, command);
   }
   
   @Override
   public BackendParametersYearData getResult(PostProcessorHandler postProcessorHandler, Globalconfig config) {
      try {
         YearData result = Constants.MAPPER.readValue(outputFile, YearData.class);
         result.getConfig().setModeldefinition(config.getModeldefinition());
         result.getConfig().setYear(config.getYear());

         try {
            new TransformJSON(result).transform(getSQLiteFile());
         } catch (SQLException e) {
            e.printStackTrace();
         }

         return new BackendParametersYearData(result);
      } catch (IOException e) {
         e.printStackTrace();
         return null;
      } 
   }
   
   @Override
   protected ProcessBuilder buildProzess(String[] command) throws IOException {
      return new ProcessBuilder(command);
   }

   @Override
   public File getListingFile() {
      return optimizeLog;
   }

   @Override
   public File getGDXParameterFile() {
      return inputFile;
   }

   @Override
   public File getGDXResultFile() {
      return outputFile;
   }

   @Override
   public File getOptimizeLog() {
      return checkLog();
   }

   @Override
   public File getParameterizeLog() {
      return checkLog();
   }

   /**
    * Checks if the irpAct generated Logfile exists,
    * else it creates a log with the information that the jar was not found.
    *
    * @return
    */
   private File checkLog() {
      if (!logFile.exists()){
         try {
            logFile.createNewFile();
            FileOutputStream oFile = new FileOutputStream(logFile, false);
            String jarNotFound = "The IRPact jar with the name: " +JAR_NAME +"was not found!";
            oFile.write(jarNotFound.getBytes());
            oFile.close();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }

      return logFile;
   }

   @Override
   public File getYearWorkspace() {
      return null;
   }
}
