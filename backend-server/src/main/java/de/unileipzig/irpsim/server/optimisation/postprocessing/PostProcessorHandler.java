package de.unileipzig.irpsim.server.optimisation.postprocessing;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.Calculation;
import de.unileipzig.irpsim.core.simulation.data.Set;
import de.unileipzig.irpsim.core.simulation.data.SetElement;
import de.unileipzig.irpsim.core.utils.DataUtils;
import de.unileipzig.irpsim.core.utils.ParameterOutputDependenciesUtil;
import de.unileipzig.irpsim.server.optimisation.OptimisationJobUtils;
import de.unileipzig.irpsim.server.optimisation.ResultFileData;
import de.unileipzig.irpsim.server.optimisation.ResultFileInfo;

/**
 * Verwaltet den Aufruf aller PostProzessor-Objekte für einen GAMSJob.
 *
 * @author reichelt
 */
public class PostProcessorHandler extends AbstractPostProcessorHandler implements Runnable {

   static final Logger LOG = LogManager.getLogger(PostProcessorHandler.class);

   private final List<ResultFileInfo> handledFiles = new LinkedList<>();

   private final File workspace;

   private final int savelength;

   private final int modelLength;

   /**
    * Initialisiert den PostProcessorHandler mit dem übergebenen Workspace und das übergebene Modell.
    *
    * @param workspace Der Arbeitsordner
    * @param savelength Die GAMS Speicherlänge
    * @param simulationLength Die Simulationslänge, wichtig um festzustellen, ob alle Prozessoren fertig sind.
    */
   public PostProcessorHandler(final File workspace, final int savelength, final int simulationLength, int modeldefinition) {
	  super(modeldefinition);
      this.savelength = savelength;
      this.workspace = workspace;
      this.modelLength = simulationLength;
      LOG.debug("Savelength: {}", savelength);
   }

   /**
    * Führt die Nachverarbeitung aus, indem aktuelle Dateien abgerufen werden und für jede noch nicht verarbeitete Datei die Nachverarbeitungswerte aktuallisiert werden.
    *
    * @param fileInfos Die Liste der zu bearbeitenden Dateien als {@link ResultFileInfo}
    */
   public final void executePostprocessing(final List<ResultFileInfo> fileInfos) {
      for (final ResultFileInfo fileinfo : fileInfos) {
         try {
            final ResultFileData data = OptimisationJobUtils.getInstance().readWholeFile(fileinfo.getFile(), savelength);
            if (data.getLines().size() < savelength) {
               LOG.trace("Datei {} wurde nicht zuende geschrieben!", fileinfo.getFile());
            } else {
               LOG.trace("Bearbeite {}", fileinfo.getFile());
               handledFiles.add(fileinfo);
               processLines(fileinfo.getFile(), data);
            }
         } catch (Throwable t) {
            LOG.debug("Problem reading {}", fileinfo.getFile());
            throw t;
         }
      }
   }

   /**
    * Liest die Dateizeilen aus und fügt sie der Nachberechnung zu.
    *
    * @param file die Datei zur Lokalisierung von Fehlern
    * @param lines die Zeilen
    */
   private void processLines(final File file, final ResultFileData data) {
      final String headerline = data.getHeader();
      final String[] parameterNames = Constants.SEMICOLONPATTERN.split(headerline);

      final Map<Integer, String> processorNames = calculateNames(parameterNames);

      if (!processorNames.isEmpty()) { // ist empty, d.h. wurde nichts
         // reingeschrieben
         for (final Map.Entry<Integer, String> processorIdentity : processorNames.entrySet()) {
            final String processorName = processorIdentity.getValue();
            final int processorIndex = processorIdentity.getKey();
            final String[] dependentNames = getDependentNames(parameterNames, processorName, processorIndex);
            LOG.trace("Name: {} Dependencies: {} {}", processorName, dependentNames.length, dependentNames);
            for (int lineIndex = 0; lineIndex < data.getLines().size(); lineIndex++) {
               processLine(file, data.getLines(), processorName, processorIndex, dependentNames, lineIndex);
            }
         }
      }
   }

   private void processLine(final File file, final List<String> lines, final String processorName, final int processorIndex, final String[] dependentNames, int lineIndex) {
      LOG.trace("Lese {}", lineIndex);
      final String contentline = lines.get(lineIndex);
      final String[] contents = Constants.SEMICOLONPATTERN.split(contentline);
      if (contents.length <= processorIndex) {
         LOG.error("Fehler in {}, Länge: {} Index: {} Zeile: {}", file, contents.length, processorName, lineIndex);
      }
      final String content = contents[processorIndex].replace("'", "");
      final double value = Double.parseDouble(content);
      for (final PostProcessor processor : getProcessors().get(processorName)) {
         final String[] dependents = Arrays.copyOfRange(dependentNames, 1, dependentNames.length);
         processor.addValue(dependents, value);
      }
   }

   private String[] getDependentNames(final String[] parameterNames, final String processorName, final int processorIndex) {
      final String nameString = parameterNames[processorIndex].replace("'", "");
      final int indexOfParanthesis = nameString.indexOf("(");
      LOG.trace(nameString + " " + processorName + " " + indexOfParanthesis);
      final String[] dependentNames = OptimisationJobUtils.COMMAPATTERN.split(nameString.substring(indexOfParanthesis, nameString.length() - 1));
      return dependentNames;
   }

   private Map<Integer, String> calculateNames(final String[] parameterNames) {
      final Map<Integer, String> processorNames = new HashMap<>();
      for (int i = 0; i < parameterNames.length; i++) {
         String name = parameterNames[i].replace("'", "");
         LOG.trace("Nachbearbeitung von Parameter {}", name);
         final int indexOfParanthesis = name.indexOf("(");
         if (indexOfParanthesis != -1) {
            name = name.substring(0, indexOfParanthesis);
            if (getProcessors().containsKey(name)) {
               processorNames.put(i, name);
            }
         }
      }
      return processorNames;
   }

   @Override
   public final void run() {
      try {
         while (!Thread.interrupted()) {
            final List<ResultFileInfo> fileInfos = OptimisationJobUtils.getInstance().getResultFiles(workspace);
            Thread.sleep(1000);
            fileInfos.removeAll(handledFiles);
            executePostprocessing(fileInfos);
         }
      } catch (final InterruptedException e) {
         // Das Sleep wird regulär beendet, wenn die Optimierung zuende
         // gelaufen ist.
      }
      LOG.debug("PostProcessing gets finished");
      final List<ResultFileInfo> fileInfos = OptimisationJobUtils.getInstance().getResultFiles(workspace);
      fileInfos.removeAll(handledFiles);
      executePostprocessing(fileInfos);
      if (!finishedPostprocessing()) {
         LOG.error("Einige PostProzessoren sind nicht fertig!");
      }
   }

   /**
    * Überprüft, ob alle Übersichtszeitreihen die durch die Simulationslänge vorgegebene Größe erreicht haben.
    *
    * @return True, wenn das Postprocessing vollständig ist
    */
   private boolean finishedPostprocessing() {
      final Map<String, Integer> unfinishedProcessors = new HashMap<>();
      getProcessors().values().forEach(processorList -> {
         processorList.stream().filter(postProcessor -> postProcessor.getCalculation().equals(Calculation.OUTLINE)).forEach(outlineProcessor -> {
            if (outlineProcessor.getSize() * 96 < modelLength && outlineProcessor.getSize() != 0) {
               unfinishedProcessors.put(outlineProcessor.getName(), outlineProcessor.getSize() * 96);
            }
         });
      });
      if (unfinishedProcessors.isEmpty()) {
         return true;
      } else {
         LOG.debug("Nicht beendete PostProzessoren: {} Ziellänge: {}", unfinishedProcessors, modelLength);
         return false;
      }
   }

   /**
    * TODO: Übersichtszeitreihen müssn egtl. nur für das letzte und unvollständige Jahr geladen werden.
    *
    * @return Die aktuellen Ergebnisse bestehend aus dem PostProcessing und den Übersichtszeitreihen
    */
   public final synchronized BackendParametersYearData fetchCurrentResults() {
      final BackendParametersYearData currentYear = new BackendParametersYearData();
      currentYear.setPostprocessing(fetchPostprocessingResults());
      fillSimpleTimeseriesOutlines(currentYear.getTimeseries());
      fillSetsTimeseriesOutlines(currentYear.getSets());
      fillTablesTimeseriesOutlines(currentYear.getTableTimeseries());
      return currentYear;
   }

   /**
    * @param tableEntries Die zu füllenden Tabellenzeitreihen
    */
   private void fillTablesTimeseriesOutlines(final Map<String, Map<String, Map<String, Timeseries>>> tableEntries) {
      getProcessors().forEach((name, processorList) -> {
         processorList.stream().filter(processor -> processor instanceof TablePostProcessor && processor.getCalculation().equals(Calculation.OUTLINE)).forEach(processor -> {
            final Map<String, Map<String, Timeseries>> firstFilledDependents = DataUtils.getOrNewMap(tableEntries, name);
            ((TablePostProcessor) processor).getValues().forEach((firstDependent, secondDependents) -> {
               final Map<String, Timeseries> secondFilledDependents = DataUtils.getOrNewMap(firstFilledDependents, firstDependent);
               secondDependents.forEach((secondDependent, timeseries) -> {
                  secondFilledDependents.put(secondDependent, Timeseries.build(new ArrayList<>()));
                  timeseries.forEach(doub -> secondFilledDependents.get(secondDependent).add(doub));
               });
            });
         });
      });
   }

   /**
    * @param sets Die zu füllenden Setzeitreihen
    */
   private void fillSetsTimeseriesOutlines(final Map<String, Set> sets) {
      getProcessors().forEach((name, processorList) -> {
         processorList.stream().filter(processor -> processor instanceof SetPostProcessor && processor.getCalculation().equals(Calculation.OUTLINE)).forEach(processor -> {
            final String setName = ParameterOutputDependenciesUtil.getInstance().getOutputSetName(name, modeldefinition);
            final Set set = sets.values().stream().filter(s -> s.getName().equals(setName)).findAny().orElse(addNewSet(sets, setName));
            fillSetOutline(name, processor, set);
         });
      });
   }

   private void fillSetOutline(String name, PostProcessor processor, final Set set) {
      ((SetPostProcessor) processor).getValues().forEach((elementName, timeseries) -> {
         final SetElement element = set.getElements().stream()
               .filter(e -> e.getName().equals(elementName))
               .findAny().orElse(addNewElement(set, elementName));
         element.getTimeseries().put(name, Timeseries.build(new ArrayList<>()));
         Timeseries currentTimeseries = element.getTimeseries().get(name);
         timeseries.forEach(value -> currentTimeseries.add(value));
      });
   }

   /**
    * @param set Das zu ergänzende Set
    * @param elementName Der Name des neu hinzuzufügenden Setelements
    * @return Das neue Setelement
    */
   private SetElement addNewElement(final Set set, final String elementName) {
      final SetElement element = new SetElement(elementName);
      set.getElements().add(element);
      return element;
   }

   /**
    * @param sets Die zu ergänzende Liste
    * @param setName Der Name des neu hinzuzufügenden Sets
    * @return das neue Set
    */
   private Set addNewSet(final Map<String, Set> sets, final String setName) {
      final Set set = new Set(setName);
      sets.put(set.getName(), set);
      return set;
   }

   /**
    * @param timeseries Die zu füllenden Zeitreihen
    */
   private void fillSimpleTimeseriesOutlines(final Map<String, Timeseries> timeseries) {
      getProcessors().forEach((name, processorList) -> {
         processorList.stream().filter(p -> p instanceof TimeseriesPostProcessor && p.getCalculation().equals(Calculation.OUTLINE)).forEach(processor -> {
            timeseries.put(name, Timeseries.build(new ArrayList<>()));
            ((TimeseriesPostProcessor) processor).getValue().forEach(doub -> timeseries.get(name).add(doub));
         });
      });
   }

   /**
    * @return Die Anzahl der bereits berechneten Zeitschritte
    */
   public final int fetchFinishedSteps() {
      int maxSteps = 0;
      for (final List<PostProcessor> processorList : getProcessors().values()) {
         final Optional<PostProcessor> outlineProcessorOptional = processorList.stream().filter(p -> p.getCalculation().equals(Calculation.OUTLINE)).findAny();
         if (outlineProcessorOptional.isPresent()) {
            final PostProcessor outlineProcessor = outlineProcessorOptional.get();
            if (outlineProcessor.fetchOutlineSize() * 96 > maxSteps) {
               LOG.debug("Prozessor: {} {} {}", outlineProcessor.getClass().getSimpleName(), outlineProcessor.getName(), outlineProcessor.fetchOutlineSize() * 96);
            }
            maxSteps = Math.max(maxSteps, outlineProcessor.fetchOutlineSize() * 96);
         }

      }
      LOG.info("Max: {}", maxSteps);
      return maxSteps;
   }
}
