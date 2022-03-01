package de.unileipzig.irpsim.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParameters;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.json.UserDefinedDescription;
import de.unileipzig.irpsim.core.utils.StreamGobbler;
import de.unileipzig.irpsim.models.ModelInformation;

/**
 * Transformiert ein Modell mit seiner Umgebung (d.h. mit Parametern) in ein vom Backend nutzbares Modell mit Umgebung (d.h. mit UI-Modelldefinition,
 * Backend-Abhängigkeitendefinition, Parametersatz).
 *
 * @author reichelt
 */
public final class ModelEnvironmentTransformer {

   // private static final String MODEL_PATH = "gams/model/";
   private static final Logger LOG = LogManager.getLogger(ModelEnvironmentTransformer.class);

   /**
    * Privater Konstruktor.
    */
   private ModelEnvironmentTransformer() {
   }

   /**
    * Startet die Modelltransformierung.
    *
    * @param args Programmstartargumente
    * @throws Exception
    */
   public static void main(final String[] args) throws Exception {
      String path = args[0];

      File folder = new File(path);

      File metaInformationFile = new File(folder, "infos.json");
      if (!metaInformationFile.exists()) {
         System.out.println("infos.json not existing in repo - please add it!");
         System.exit(1);
      }

      String version = saveBuildInformation(folder);
      MetadataHandler handler = new MetadataHandler(metaInformationFile);
      ModelInformation metadata = handler.handleMetadata(version);

      if (metadata.getType().equals("GDX")) {
         new GAMSTransformer(folder, metadata).transform();
      } else if (metadata.getType().equals("Java")) {
         new JavaTransformer(folder, metadata).transform();
      } else {
         throw new RuntimeException("Type " + metadata.getType() + " not supported");
      }
      
      if (metadata.getId() == 1 || metadata.getId() == 3) {
         createFullScenario();
         createWeekScenario();
      }
   }

   private static void createWeekScenario() throws IOException, JsonParseException, JsonMappingException, JsonGenerationException {
      File optWeek = new File(TransformConstants.SCENARIO_FOLDER, "1.json");
      File actWeek = new File(TransformConstants.SCENARIO_FOLDER, "3.json");
      File destFileWeek = new File(TransformConstants.SCENARIO_FOLDER, "5.json");
      
      String descriptionWeek = TransformConstants.DEFAULTSCENARIO_WEEK_TITLE + "opt-act";
      
      createCombinedScenario(optWeek, actWeek, destFileWeek, descriptionWeek);
   }

   private static void createFullScenario() throws IOException, JsonParseException, JsonMappingException, JsonGenerationException {
      File optFull = new File(TransformConstants.SCENARIO_FOLDER, "1_full.json");
      File actFull = new File(TransformConstants.SCENARIO_FOLDER, "3_full.json");
      File destFile = new File(TransformConstants.SCENARIO_FOLDER, "5_full.json");
      
      String description = TransformConstants.DEFAULTSCENARIO_YEAR_TITLE + "opt-act";
      
      createCombinedScenario(optFull, actFull, destFile, description);
   }

   private static void createCombinedScenario(File optFull, File actFull, File destFile, String description)
         throws IOException, JsonParseException, JsonMappingException, JsonGenerationException {
      LOG.info("IRPOPT: " + optFull.exists() +" IRPACT "+actFull.exists());
      if (optFull.exists() && actFull.exists()) {
         LOG.info("Build combined Scenario");
         JSONParametersMultimodel optFullParameters = Constants.MAPPER.readValue(optFull, JSONParametersMultimodel.class);
         JSONParametersMultimodel actFullParameters = Constants.MAPPER.readValue(actFull, JSONParametersMultimodel.class);
         
         JSONParameters onlyOptFullParameters = new JSONParameters();
         JSONParameters onlyActFullParameters = new JSONParameters();
         onlyOptFullParameters.setYears(optFullParameters.getModels().get(0).getYears());
         onlyActFullParameters.setYears(actFullParameters.getModels().get(0).getYears());
         
         JSONParametersMultimodel optAct = new JSONParametersMultimodel();
         optAct.setDescription(new UserDefinedDescription());
         optAct.getDescription().setBusinessModelDescription(description);
         optAct.getDescription().setCreator(TransformConstants.DEFAULTSCENARIO_CREATOR);
         
         optAct.getModels().add(onlyOptFullParameters);
         optAct.getModels().add(onlyActFullParameters);
         
         Constants.MAPPER.writeValue(destFile, optAct);
      }
   }

   

   /**
    * Erstellt den Git-Hash der aktuellen Modellversion und speichert ihn in eine Datei. Der Pfad des Modellversions-Ordners wird angegeben.
    *
    * @param folder Pfad der aktuellen Modellversion
    */
   public static String saveBuildInformation(final File folder) {
      try {
         if (!folder.exists()) {
            LOG.error("Achtung: Ordner {} existiert nicht", folder.getAbsolutePath());
         }
         final File resultfile = new File(Constants.SERVER_MODULE_PATH + "src/main/resources/gams/version.txt");
         if (!resultfile.getParentFile().exists()) {
            resultfile.getParentFile().mkdirs();
         }
         try (final FileWriter fw = new FileWriter(resultfile)) {
            writeGitHash(folder, fw);
            writeBuildDate(folder, fw);
            fw.close();
         }

         String content = Files.readString(resultfile.toPath(), StandardCharsets.UTF_8);
         resultfile.delete();
         return content;
      } catch (final IOException e) {
         e.printStackTrace();
         return null;
      }
   }

   private static void writeGitHash(final File folder, final FileWriter fw) throws IOException {
      final Process commitProcess = Runtime.getRuntime().exec("git rev-parse HEAD", new String[] {}, folder);
      final String commitId = StreamGobbler.getFullProcess(commitProcess, false);
      fw.write(commitId);
   }

   private static void writeBuildDate(final File folder, final FileWriter fw) throws IOException {
      LOG.info("Starte Datumsabfrage");
      final Process dateProcess = Runtime.getRuntime().exec("git show -s --format=%cI HEAD", new String[] {}, folder);
      final String dateResult = StreamGobbler.getFullProcess(dateProcess, true).replaceAll("\n", "");
      final DateTimeFormatter parser2 = ISODateTimeFormat.dateTimeNoMillis();
      final DateTime dt = parser2.parseDateTime(dateResult);
      final DateTimeFormatter sdfDate = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm");// new SimpleDateFormat("dd.MM.yyyy HH:mm");// dd/MM/yyyy
      // final Date now = new Date();
      fw.write(" Datum: " + sdfDate.print(dt));
   }
   
}
