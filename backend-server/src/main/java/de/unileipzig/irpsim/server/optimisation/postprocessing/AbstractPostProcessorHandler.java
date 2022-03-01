package de.unileipzig.irpsim.server.optimisation.postprocessing;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.simulation.data.AggregatedResult;
import de.unileipzig.irpsim.core.simulation.data.Calculation;
import de.unileipzig.irpsim.core.simulation.data.PostProcessing;
import de.unileipzig.irpsim.core.utils.DataUtils;
import de.unileipzig.irpsim.core.utils.ParameterOutputDependenciesUtil;

/**
 * Basisklasse für alle Klassen, die die Nachverarbeitung anhand von mehreren PostPrcoessor-Objekten organisieren und diese aus einer GAMS-Nachverarbeitungsdefinition erstellen.
 *
 * @author reichelt
 */
public abstract class AbstractPostProcessorHandler {

	private static final Logger LOG = LogManager.getLogger(AbstractPostProcessorHandler.class);

	private final Map<String, List<PostProcessor>> processors = new HashMap<>();

	protected final int modeldefinition;
		
   /**
    * Erstellt die {@link PostProcessor} für das übergebene Modell.
    */
   public AbstractPostProcessorHandler(int modeldefinition) {
      this.modeldefinition = modeldefinition;
      final Map<String, List<Calculation>> postprocessing = ParameterOutputDependenciesUtil.getInstance().getPostprocessings(modeldefinition);

      for (final Map.Entry<String, List<Calculation>> processor : postprocessing.entrySet()) {
         final String parameterName = processor.getKey();
         List<PostProcessor> processorList = new LinkedList<>();
         processors.put(parameterName, processorList);

         final List<String> outputDepencies = ParameterOutputDependenciesUtil.getInstance().getOutputParameterDependencies(parameterName, modeldefinition);
         outputDepencies.remove("set_ii");
         addProcessorsForCalculations(processor, parameterName, processorList, outputDepencies);
      }
   }

   private void addProcessorsForCalculations(final Map.Entry<String, List<Calculation>> processor, final String parameterName, List<PostProcessor> processorList,
         final List<String> outputDepencies) {
      final List<Calculation> calculations = processor.getValue();
      for (final Calculation calculation : calculations) {

         if (outputDepencies.size() == 0) {
            processorList.add(new TimeseriesPostProcessor(parameterName, calculation));
         }
         if (outputDepencies.size() == 1) {
            processorList.add(new SetPostProcessor(parameterName, calculation));
         }
         if (outputDepencies.size() == 2) {
            processorList.add(new TablePostProcessor(parameterName, calculation));
         }
      }
   }

   /**
    * Liest ein Set aus einem Nachverarbeitungs-Objekt aus, falls ein Set mit dem übergebenen Namen existiert. Falls kein Set mit dem Namen existiert, wird ein neues Set erzeugt
    * und hinzugefügt.
    *
    * @param setName Name des Sets, das gefunden oder hinzugefügt werden soll
    * @param gpj Nachverabeitungsobjekt
    * @return Set-Objekt
    */
   private LinkedHashMap<String, Map<String, AggregatedResult>> getOrAddSet(final String setName, final PostProcessing gpj) {
      LOG.trace("Lade Set mit Name: {}", setName);
      LinkedHashMap<String, Map<String, AggregatedResult>> set = gpj.getSets().get(setName);
      if (set == null) {
         set = new LinkedHashMap<String, Map<String, AggregatedResult>>();
         gpj.getSets().put(setName, set);
      }
      return set;
   }

   /**
    * Gibt die aktuellen Nachverarbeitungsergebnisse aus.
    *
    * @return Aktuelle Nachverarbeitungsergebnisse
    */
   public final synchronized PostProcessing fetchPostprocessingResults() {
      final PostProcessing gpj = new PostProcessing();
      LOG.info("Postprocessoren: {}", processors.size());
      for (final Map.Entry<String, List<PostProcessor>> processorList : processors.entrySet()) {
         for (final PostProcessor processor : processorList.getValue()) {
            if (processor.getCalculation() == Calculation.OUTLINE) {
               continue;
            }
            if (processor instanceof TimeseriesPostProcessor) {
               handleScalar(gpj, (TimeseriesPostProcessor) processor);
               continue;
            }
            if (processor instanceof SetPostProcessor) {
               handleSet(gpj, processorList.getKey(), (SetPostProcessor) processor);
               continue;
            }
            if (processor instanceof TablePostProcessor) {
               handleTable(gpj, (TablePostProcessor) processor);
            }
         }
      }
      return gpj;
   }

   /**
    * Fügt das Ergebnis des {@link TimeseriesPostProcessor} zum {@link PostProcessing} hinzu.
    *
    * @param gpj Das {@link PostProcessing}
    * @param processor Der {@link TimeseriesPostProcessor}
    */
   public final void handleScalar(final PostProcessing gpj, final TimeseriesPostProcessor processor) {
      AggregatedResult result = gpj.getScalars().get(processor.getName());
      if (result == null) {
         result = new AggregatedResult();
         gpj.getScalars().put(processor.getName(), result);
      }

      result.placeValue(processor.getCalculation(), processor.getValue().get(0));
   }

   /**
    * Fügt das Ergebnis des {@link SetPostProcessor} zum {@link PostProcessing} hinzu.
    *
    * @param gpj Das {@link PostProcessing}
    * @param name Der Parametername
    * @param processor Der {@link SetPostProcessor}
    */
   public final void handleSet(final PostProcessing gpj, final String name, final SetPostProcessor processor) {
      final String setName = ParameterOutputDependenciesUtil.getInstance().getOutputSetName(name, modeldefinition);
      final LinkedHashMap<String, Map<String, AggregatedResult>> set = getOrAddSet(setName, gpj);
      processor.getValues().forEach((elementname, value) -> {
         Map<String, AggregatedResult> element = set.computeIfAbsent(elementname, k -> new LinkedHashMap<>());
         AggregatedResult result = element.get(processor.getName());
         if (result == null) {
            result = new AggregatedResult();
            element.put(processor.getName(), result);
         }
         result.placeValue(processor.getCalculation(), value.get(0));
      });
   }

   /**
    * Fügt das Ergebnis des {@link TablePostProcessor} zum {@link PostProcessing} hinzu.
    *
    * @param gpj Das {@link PostProcessing}
    * @param processor Der {@link TablePostProcessor}
    */
   public final void handleTable(final PostProcessing gpj, final TablePostProcessor processor) {
      final LinkedHashMap<String, Map<String, AggregatedResult>> results = DataUtils.getOrNewLinkedMap(gpj.getTables(), processor.getName());
      final Map<String, Map<String, List<Double>>> values = processor.getValues();
      values.forEach((parameterName, pair) -> {
         final Map<String, AggregatedResult> key1Map = DataUtils.getOrNewMap(results, parameterName);
         pair.forEach((key2, value) -> {
            AggregatedResult result = key1Map.get(key2);
            if (result == null) {
               result = new AggregatedResult();
               key1Map.put(key2, result);
            }
            result.placeValue(processor.getCalculation(), value.get(0));
         });
      });

      gpj.getTables().put(processor.getName(), results);
   }

   protected final Map<String, List<PostProcessor>> getProcessors() {
      return processors;
   }
}
