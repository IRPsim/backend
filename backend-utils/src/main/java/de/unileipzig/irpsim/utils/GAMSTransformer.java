package de.unileipzig.irpsim.utils;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.utils.ParameterBaseDependenciesUtil;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.models.ModelInformation;
import de.unileipzig.irpsim.utils.transformer.ExcelParametersReader;
import de.unileipzig.irpsim.utils.transformer.ExtendedScenarioBuilder;
import de.unileipzig.irpsim.utils.transformer.GAMSModelTransformer;
import de.unileipzig.irpsim.utils.transformer.GAMSParserCaller;

public class GAMSTransformer {
   
   private static final Logger LOG = LogManager.getLogger(GAMSTransformer.class);
   
   private final File modelDir;
   private final ModelInformation metadata;
   
   public GAMSTransformer(File folder, ModelInformation metadata) throws IOException, Exception {
      modelDir = findModelDirectory(folder);
      this.metadata = metadata;
   }
   
   public void transform() throws IOException, Exception {
      if (transformModel(modelDir.getAbsolutePath(), metadata.getId())) {
         final File parameterFileJSON = generateJSONParameterFiles(modelDir.getAbsolutePath(), metadata);
         LOG.debug("Standard Parameter Datei erzeugt: {}", parameterFileJSON);
         if (metadata.getId() == 1) {
            generateJSONTestFiles(metadata, parameterFileJSON);
         }
      } else {
         LOG.error("Error in model transformation");
      }
   }
   
   /**
    * Erzeugt aus Basismodell-JSON die Test-JSON-Dateien.
    *
    * @param modelInformation Modellname
    * @param parameterFileJSON Parameterdatei, auf deren Grundlage alle Testdateien erzeugt werden.
    * @throws Exception
    */
   public static void generateJSONTestFiles(final ModelInformation modelInformation, final File parameterFileJSON) throws Exception {
      try {
         File scenarioBaseFile = new File(Constants.SCENARIO_FOLDER_JSON, modelInformation.getId() + ".json");

         LOG.debug("Erstelle TestFiles in {}", TestFiles.TEST_PARAMS.getAbsolutePath());
         final File baseFile = TestFiles.TEST.make();
         final File oneYearFile = TestFiles.FULL_YEAR.make();
         FileUtils.copyFile(parameterFileJSON, baseFile);
         FileUtils.copyFile(scenarioBaseFile, oneYearFile);

         new ExtendedScenarioBuilder(scenarioBaseFile).changeNumberOfDays(21).save(TestFiles.DAYS_21.make());

         new ExtendedScenarioBuilder(scenarioBaseFile).changeNumberOfDays(3).save(TestFiles.DAYS_3.make());

         new ExtendedScenarioBuilder(scenarioBaseFile).changeNumberOfDays(3).addMirroredYear(0).addNullYears(0, 2).save(TestFiles.MULTI_THREE_DAYS.make());
         new ExtendedScenarioBuilder(scenarioBaseFile).deleteSet("set_side_cust").save(TestFiles.ERROR.make());

         final ExtendedScenarioBuilder builder2 = new ExtendedScenarioBuilder(scenarioBaseFile);
         builder2.addMirroredYear(0).addNullYears(0, 2).save(TestFiles.MULTIPLE_YEAR.make());

         builder2.addNullYears(0, 2).changeYearDate(5, 2020).doubleSingleTimeseries(5, new String[] { "timeseries", "par_C_MS_E" });
         builder2.save(TestFiles.INTERPOLATION.make());

      } catch (final Exception e) {
         e.printStackTrace();
         throw e;
      }
   }
   
   /**
    * Erzeugt die Parameterdatei.
    *
    * @param modelName Modellname
    * @param source Quellordner der GAMS Spezifikationen
    * @return Die erzeugte Datei
    * @throws Exception
    */
   public static File generateJSONParameterFiles(final String source, ModelInformation information)
         throws Exception {
      final File excelFile = new File(source + "/input/modelinput.xlsx");
      final File inputSpecificationFile = new File(source + "/input/input_specification.txt");
      if (excelFile.exists() && inputSpecificationFile.exists()) {
         final String weekDescription = TransformConstants.DEFAULTSCENARIO_WEEK_TITLE + information.getName();

         File scenarioFile = new File(Constants.SERVER_MODULE_PATH + "src/main/resources/scenarios/" + information.getId() + ".json");
         File scenarioFileFullYear = new File(Constants.SERVER_MODULE_PATH + "src/main/resources/scenarios/" + information.getId() + "_full.json");

         final ExcelParametersReader readXLSXModel = new ExcelParametersReader(excelFile, inputSpecificationFile, scenarioFile, weekDescription, information.getId());
         readXLSXModel.readXLSXFileToJSON();
        
         final String yearDescription = TransformConstants.DEFAULTSCENARIO_YEAR_TITLE + information.getName();
         final ExtendedScenarioBuilder extendedSimulationBuilder = new ExtendedScenarioBuilder(scenarioFile);
         extendedSimulationBuilder.changeNumberOfDays(365).describe(yearDescription).save(scenarioFileFullYear);

         return scenarioFile;
      } else {
         LOG.warn("Datei {} {} oder {} {} existiert nicht ", excelFile, excelFile.exists(), inputSpecificationFile, inputSpecificationFile.exists());
         return null;
      }

   }
   
   private static File findModelDirectory(File folder) {
      File modelDir = null;
      for (File potentialModelFolder : folder.listFiles()) {
         if (potentialModelFolder.isDirectory() && !potentialModelFolder.getName().equals(".git")) {
            modelDir = potentialModelFolder;
         }
      }
      if (modelDir == null) {
         System.out.println("Model directory not found - given folder needs to contain model directory!");
         System.exit(1);
      }
      return modelDir;
   }
   
   /**
    * TODO.
    *
    * @param source Der Name des QUellordners
    * @param model TODO
    * @param modelRealName Der reale Modellname
    * @return true falls der GAMD-check poditiv vrtläuft, false sonst
    * @throws IOException Tritt auf falls Fehler beim Lesen und Schreiben auftreten
    */
   public static boolean transformModel(final String source, final int modelId) throws IOException {
      if (GAMSParserCaller.checkGAMS(source)) {
         final String outputDependenciesName = "../backend-core/src/main/resources/gams-dependencies-" + modelId + ".json";
         GAMSParserCaller.createBackendDependencies(source, outputDependenciesName);
         GAMSParserCaller.createFrontendData(source, Constants.SERVER_MODULE_PATH + "src/main/resources/modeldefinitions/" + modelId + ".json");
         ParameterBaseDependenciesUtil.getInstance().loadDependencies(new File(outputDependenciesName), modelId);
         LOG.info("Verarbeite: {}", modelId);

         File destFolder = new File(Constants.GAMS_MODULE_PATH + "src/main/resources/gams/" + modelId);
         GAMSModelTransformer.transformModel(new File(source), destFolder);
         return true;
      } else {
         return false;
      }
   }
}
