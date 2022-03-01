package de.unileipzig.irpsim.server.optimisation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersMultiModel;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.json.YearData;

/**
 * Stellt Funktionalitäten für die Ausführung eines Simulationsjobs bereit, derzeitig handelt es sich hierbei nur um die Bereistellung von Informationen zu den Ausgabeparametern
 * eines bestimmten Modells.
 *
 * @author reichelt
 */
public final class OptimisationJobUtils {

   private static final Logger LOG = LogManager.getLogger(OptimisationJobUtils.class);

   private static OptimisationJobUtils instance;

   public static final Pattern COMMAPATTERN = Pattern.compile(",");
   private static final Pattern SPACEPATTERN = Pattern.compile("\\s");

   /**
    * Privater Konstruktor für Singletonarchitektur.
    */
   private OptimisationJobUtils() {
   }

   /**
    * Erstellt eine Instanz der Klasse, nach Singleton-Architektur, falls diese null ist.
    *
    * @return Die erzeugete Instanz.
    */
   public static OptimisationJobUtils getInstance() {
      if (null == instance) {
         instance = new OptimisationJobUtils();
      }
      return instance;
   }

   /**
    * @param results {@link JSONParametersMultiModel}, in die Daten gemerged werden
    * @param postProcessingResults {@link BackendParametersMultiModel}, die zu den Ergebnissen hinzugefügt werden
    */
   public static void mergeCurrentResult(final JSONParametersMultimodel results, final BackendParametersMultiModel postProcessingResults) {
      //TODO This needs to be done for all models in a correct manner - currently only usable for SingleModel 
      //(but this is ok, since postprocessing was only wanted by IWB and is currently not realy wanted)
      final ListIterator<YearData> resultIterator = results.getModels().get(0).getYears().listIterator();
      final int lastProcessedYear = getLastYear(postProcessingResults);
      for (int index = 0; index <= lastProcessedYear; index++) {
         final BackendParametersYearData processedYear = postProcessingResults.getModels()[0].getYeardata()[index];
         final boolean test = resultIterator.next() == null;
         if (!resultIterator.hasNext() || test) {
            if (processedYear != null) {
               resultIterator.add(processedYear.createJSONParameters());
            } else {
               resultIterator.add(null);
            }
         }
      }
   }

   /**
    * @param postProcessingResults Datensatz, für den das letzte nicht leere Jahr gefunden wird
    * @return Das letzte Jahr, das nicht leer ist
    */
   private static int getLastYear(final BackendParametersMultiModel postProcessingResults) {
      for (int lastYear = postProcessingResults.getModels()[0].getYeardata().length - 1; lastYear >= 0; lastYear--) {
         if (postProcessingResults.getModels()[0].getYeardata()[lastYear] != null) {
            return lastYear;
         }
      }
      return 0;
   }

   /**
    * Gibt eine nach Namen der Dateien sortierte Liste der CSV_Ergebnisdateien zurück, die im aktuellen GAMS-Ordner liegen.
    *
    * @param currentWorkspace Aktueller Workspace, aus dem Ergebnisdateien ausgegeben werden sollen
    * @return Nach Namen sortierte Liste der CSV_Ergebnisdateien.
    */
   public List<ResultFileInfo> getResultFiles(final File currentWorkspace) {
      final File output = new File(currentWorkspace, "output" + File.separator + "results");
      LOG.trace("Suche nach Ausgabe in {} Ordner: {}", output.getAbsolutePath(), currentWorkspace);

      if (!output.exists()) {
         LOG.debug("Ausgabeordner {} existiert nicht", output.getAbsoluteFile());
         return new LinkedList<>();
      }
      final List<ResultFileInfo> infos = new LinkedList<>();
      for (final File outputFile : output.listFiles((FilenameFilter) new WildcardFileFilter("a*.csv"))) {
         LOG.trace("AusgabeDatei: {}", outputFile.getName());
         final ResultFileInfo fi = new ResultFileInfo(outputFile);
         infos.add(fi);
      }
      LOG.trace("Ergebnisse: {}", infos.size());
      Collections.sort(infos);
      return infos;
   }

   /**
    * Liest eine Datei Zeile für Zeile, fügt Alle Zeilen der Datei,welche länger als 3 Zeichen sind, einer Liste hinzu und liefert diese Liste zurück.
    *
    * @param file Die zu lesende Datei.
    * @param savelength Savelength der GAMS-Simulation
    * @return Liste aller Zeilen der Datei.
    */
   public ResultFileData readWholeFile(final File file, final int savelength) {
      try (BufferedReader br = new BufferedReader(new FileReader(file))) {
         String line = br.readLine();
         ResultFileData data;
         if (line != null) {
            final String shortLine = SPACEPATTERN.matcher(line).replaceAll("");
            data = new ResultFileData(shortLine, savelength);
            while ((line = br.readLine()) != null) {
               if (line.length() < 3) {
                  LOG.error("Achtung: Zeile kleiner als 3 Zeichen!");
               } else {
                  final String shortenedLine = SPACEPATTERN.matcher(line).replaceAll("");
                  data.getLines().add(shortenedLine);
               }
               br.readLine(); // Leerzeile
               br.readLine(); // Header-Zeile
            }

         } else {
            data = null;
         }
         return data;
      } catch (final IOException e) {
         e.printStackTrace();
         throw new RuntimeException(e);
      }
   }

   /**
    * @param fileinfos Die zu sortierenden {@link ResultFileInfo}
    * @return Gibt die ResultFileInfos sortiert in der Reihenfolge der Ergebnisse zurück.
    */
   public static Map<Integer, List<ResultFileInfo>> createFileInfoMap(final List<ResultFileInfo> fileinfos) {
      final Map<Integer, List<ResultFileInfo>> fileInfoMap = new LinkedHashMap<>();
      for (final ResultFileInfo fileinfo : fileinfos) {
         List<ResultFileInfo> infolist = fileInfoMap.get(fileinfo.getTimestep());
         if (infolist == null) {
            infolist = new LinkedList<>();
            fileInfoMap.put(fileinfo.getTimestep(), infolist);
         }
         infolist.add(fileinfo);
      }
      return fileInfoMap;
   }

   /**
    * Fügt die Zeilen, die als Liste von Strings übergeben werden, zu einem ImportHandler hinzu, der später den Import verwaltet.
    *
    * @param importhandler Der Importhandler für dieses Jahr.
    * @param lines Die zu bearbeitenden und importierenden Zeilen.
    * @return True wenn kein Fehler festgestellt wurde, false wenn mindestens eine der Zeilen mehr/weniger Daten als als Angaben in der Kopfzeile hat.
    */
   public static boolean importReadLines(final ImportResultHandler importhandler, final ResultFileData data) {
      // Die Header-Zeile enthält zwar wachsende Zeitschritte, diese sind aber
      // aufgrund der Zeilennummer nicht relevant. Deshalb ist es ok, nur die
      // 1. Zeile als header-Zeile zu nutzen (andere Set-Abhängigkeiten, bspw.
      // die zu Kundengruppen, bleiben in allen Headerzeilen konstant).
      final String headerline = data.getHeader();
      final String[] names = Constants.SEMICOLONPATTERN.split(headerline);
      boolean success = true;
      for (int lineIndex = 0; lineIndex < data.getLines().size(); lineIndex++) {
         LOG.trace("Lese {}", lineIndex);
         final String contentline = data.getLines().get(lineIndex);
         final String[] contents = Constants.SEMICOLONPATTERN.split(contentline);
         if (names.length != contents.length) {
            LOG.error("Achtung: Header-Länge ({}) ungleich Wertlänge ({}) bei {}!", names.length, contents.length, lineIndex);
            success = false;
         } else {
            for (int j = 0; j < names.length; j++) {
               final String name = names[j].replace("'", "");
               final String content = contents[j].replace("'", "");
               if (NumberUtils.isCreatable(content)) {
                  final double value = Double.parseDouble(content);
                  importhandler.manageResult(name, value);
               } else {
                  if (!content.matches("j[0-9]+") && !content.matches("a[0-9]+") && !content.matches("t[0-9]+")) {
                     LOG.trace("Keine Zahl: {}", content);
                  }
               }
            }
         }
      }
      return success;
   }
}
