package de.unileipzig.irpsim.server.optimisation;

import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.core.simulation.data.*;
import de.unileipzig.irpsim.core.simulation.data.json.YearState;
import de.unileipzig.irpsim.core.simulation.data.persistence.*;
import de.unileipzig.irpsim.core.utils.ParameterInputDependenciesUtil;
import de.unileipzig.irpsim.core.utils.ParameterOutputDependenciesUtil;
import de.unileipzig.irpsim.server.modelstart.ModelStarter;
import de.unileipzig.irpsim.server.optimisation.comparators.modelconnectors.ConnectionType;
import de.unileipzig.irpsim.server.optimisation.comparators.modelconnectors.DependencyType;
import de.unileipzig.irpsim.server.optimisation.comparators.modelconnectors.SyncableParameter;
import de.unileipzig.irpsim.server.optimisation.comparators.modelconnectors.SyncableParameterHandler;
import de.unileipzig.irpsim.server.optimisation.persistence.OptimisationJobPersistenceManager;
import de.unileipzig.irpsim.server.optimisation.postprocessing.PostProcessorHandler;
import org.apache.commons.math3.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.persistence.EntityTransaction;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static de.unileipzig.irpsim.core.Constants.IRPACT_MODELDEFINITION;

/**
 * Repräsentiert den Prozess eines Laufenden Optimierungsjahrs.
 * 
 * @author reichelt
 *
 */
public class OptimisationYear implements Runnable {

   private static final Logger LOG = LogManager.getLogger(OptimisationYear.class);

   private final ModelStarter caller;
   private final int modelIndex, yearIndex; // Index im Array
   private final int simulatedYearIndex; // Index in simulierten Jahren
   private final OptimisationJobPersistenceManager persistencemanager;
   private final PostProcessorHandler[] postProcessors;
   private final BackendParametersYearData yearParameters;
   private final BackendParametersMultiModel result;
   private final Job job;
   private final long jobid;
   private boolean finished = false;

   private Date start = null;

   public OptimisationYear(final int modelIndex, final int yearIndex, final long jobid, final OptimisationJobPersistenceManager persistencemanager,
         final PostProcessorHandler[] postProcessors, final BackendParametersYearData yearParameters, final BackendParametersMultiModel result, final Job job,
         final int simulatedYears,
         ModelStarter caller) {
      this.modelIndex = modelIndex;
      this.yearIndex = yearIndex;
      this.persistencemanager = persistencemanager;
      this.postProcessors = postProcessors;
      this.yearParameters = yearParameters;
      this.result = result;
      this.job = job;
      this.simulatedYearIndex = simulatedYears;
      this.jobid = jobid;

      this.caller = caller;
   }

   @Override
   public void run() {
      try {
         persistStart();

         parameterize();

         if (Thread.currentThread().isInterrupted()) {
            return;
         }
         caller.parameterize();

         if (caller.getLastExitCode() != 0) {
            if (caller.getLastExitCode() == 137) { // 128 + 9 -> mit kill -9 beendet
               LOG.info("Job unterbrochen");
            }
            job.setError(true);
            return;
         }
         if (Thread.currentThread().isInterrupted()) {
            return;
         }
         LOG.debug("Starte Modell, Jahr: {}", yearIndex);
         job.setState(State.RUNNING);

         final Thread postProcessorThread = new Thread(postProcessors[yearIndex]);
         try {
            postProcessorThread.start();
            caller.startOptimisation();
            if (caller.getLastExitCode() != 0) {
               LOG.info("Jahr endet mit (vermutlich GAMS-)Exception");
               persistencemanager.persistErrorYear(modelIndex, yearIndex, caller, caller.getGDXParameterFile());
               if (caller.getLastExitCode() == 2) {
                  job.addError("Unbekannter GAMS-Fehler aufgetreten");
                  throw new RuntimeException("Unbekannter GAMS-Fehler aufgetreten");
               }
               return;
            }
         } finally {
            postProcessorThread.interrupt();
         }
         postProcessorThread.join();
         job.setState(State.PERSISTING);
         if (Thread.currentThread().isInterrupted()) {
            return;
         }

         persistResults();
         if (Thread.currentThread().isInterrupted()) {
            return;
         }
         LOG.info("Persistiere Ergebnisdaten");

         persistencemanager.persistYear(modelIndex, simulatedYearIndex, caller, result.createJSONParameters(), job.getCSVFile(0, yearIndex));
      } catch (IOException e) {
         e.printStackTrace();
         job.addError(e.getLocalizedMessage());
      } catch (InterruptedException e) {
         e.printStackTrace();
         return;
      } finally {
         finished = true;
         LOG.debug("Job finished, calling yearFinished");
         OptimisationYearHandler.yearFinished(this);
      }
   }

   /**
    * Fügt dem aktuellen Simulationsjahr den Postprocessor hinzu. Zusätzlich werden im Falle eines Multi Modell Laufes,
    * die aus dem Vorjahr verwendbaren Parameter in das aktuelle Jahr Übernommen.
    * Im Falle eines mehr jährigen IRPact Laufs werden die Ergebnisse des vorjahres in das aktuelle Jahr übernommen.
    */
   private synchronized void parameterize() {
      OptimisationYear predecessorYear = getPredecessorYear();
      if (predecessorYear != null) {
         // 1) match Parameters
         SyncableParameterHandler handler = SyncableParameterHandler.getInstance();
         int preModelDefinition = predecessorYear.yearParameters.getConfig().getModeldefinition();
         int currentModelDefinition = yearParameters.getConfig().getModeldefinition();

         if( handler.isApprovedConnection(preModelDefinition, currentModelDefinition)) {
            if (preModelDefinition != currentModelDefinition) {
               Pair<Integer, Integer> modelPair = new Pair<>(preModelDefinition, currentModelDefinition);

               // 2) override current Run with matches from prev run
               SyncableParameter syncableParameterInputToInput = handler
                     .fetchSyncableParameter(modelPair, ConnectionType.INPUT);
               connectParameter(syncableParameterInputToInput, predecessorYear, false);

               SyncableParameter syncableParameterOutputTOInput = handler
                     .fetchSyncableParameter(modelPair, ConnectionType.OUTPUT_TO_INPUT);
               connectParameter(syncableParameterOutputTOInput, predecessorYear, true);

            } else {
               // 3) if IRPact was running before copy output of last act run to current
               if (currentModelDefinition == IRPACT_MODELDEFINITION) {
                  OptimisationYear previousActYear = getPreviousActYear(this);
                  if (previousActYear != null) {

                     // find pref outputFile.json oder get Result?
                     BackendParametersYearData resultBackendParameter = result.getModels()[previousActYear.modelIndex]
                           .getYeardata()[previousActYear.yearIndex];
                     // copy set_Binary and others
                     SyncableParameter syncableParameterOutputTOInputAct = handler
                           .fetchSyncableParameter(new Pair<>(IRPACT_MODELDEFINITION, IRPACT_MODELDEFINITION), ConnectionType.OUTPUT_TO_INPUT);
                     connectParameter(syncableParameterOutputTOInputAct, previousActYear, true);
                  }
               }
            }
         }
      }
      final File currentWorkspace = caller.getYearWorkspace();
      final Globalconfig config = yearParameters.getConfig();
      postProcessors[yearIndex] = new PostProcessorHandler(currentWorkspace,
            config.getSavelength(), config.getSimulationlength(), config.getModeldefinition());
      job.setState(State.PARAMETERIZING);
      LOG.info("Job {} in Status {} Jahr: {}", jobid, job.getState(), yearIndex);
   }

   /**
    * Verbindet die Ausgabe Parameter des Vorjahres mit den Eingabe Parametern des aktuellen Jahres.
    * Alternativ können Eingabe Parameter des Ersten Modells mit den Eingabe Parametern des Folgemodells verbunden werden.
    *
    * @param syncableParameter Parameter welche in beiden Modellen enthalten sind.
    * @param predecessorYear vorheriges [OptimisationYear]
    * @param isOutputToInput Flag ob die Parameter output_to_Input oder Input_to_input verbinden
    */
   private void connectParameter(SyncableParameter syncableParameter, OptimisationYear predecessorYear,boolean isOutputToInput) {
      Map<DependencyType, List<String>> parameterMap = syncableParameter.getParameterMap();
      ConnectedModelParameter year = result.getModels()[predecessorYear.modelIndex];
      int preModelDefinition = predecessorYear.yearParameters.getConfig().getModeldefinition();

      for (Map.Entry<DependencyType, List<String>> parameterEntry : parameterMap.entrySet()) {
         DependencyType dependencyType = parameterEntry.getKey();
         List<String> paramList = parameterEntry.getValue();
         LOG.debug("KEY: {} Value: {}", dependencyType.name(), paramList.size());
         try {
            switch (dependencyType) {
            case SCALAR:
            case SET_SCALAR: {
               if (isOutputToInput) {
                  connectScalarOutToIn(predecessorYear, year, preModelDefinition, paramList);
               } else {
                  connectScalarInTOIn(predecessorYear, preModelDefinition, paramList);
               }
               break;
            }
            case TIMESERIES:
            case SET_TIMESERIES: {
               if (isOutputToInput) {
                  connectTimeSeriesOutToIn(predecessorYear, year, preModelDefinition, paramList);
               } else {
                  connectTimeSeriesInToIn(predecessorYear, preModelDefinition, paramList);
               }
               break;
            }
            case TABLE_SCALAR:
            case TABLE_TIMESERIES: {
               if (isOutputToInput) {
                  connectTablesOutToIn(predecessorYear, year, preModelDefinition, paramList);
               } else {
                  connectTablesInToIn(predecessorYear, preModelDefinition, paramList);
               }
               break;
            }
            default:
               LOG.error("Unimplemented DependencyType {}", dependencyType.name());
               throw new RuntimeException("Unimplemented DependencyType handling" + dependencyType.name());
            }
         } catch (Exception e) {
            e.printStackTrace();
            LOG.error("SOMETHING WENT HORRIBLY WRONG {}!!!!", dependencyType);
         }
      }
   }

   /**
    * Wrapper to connect/synchronize output scalar to input scalar which must share the same name,
    * from the [predecessorYear] ( starts with par_out_X)to the current year ( par_X).
    *
    * @param predecessorYear OptimisationYear of the previous year (or same year but from another model)
    * @param preModelDefinition Model definition of the predecessor year (for dependency query)
    * @param paramList List of parameter names which are available in both years
    */
   private void connectScalarOutToIn(OptimisationYear predecessorYear, ConnectedModelParameter year, int preModelDefinition, List<String> paramList) {
      for (String targetParameter : paramList) {
         // need to add _out_
         String OutputParam = targetParameter.replace("par_", "par_out_");
         if (year != null) {

            // get outputs of last year
            BackendParametersYearData[] yearDatas = year.getYeardata();
            BackendParametersYearData yearData = yearDatas[predecessorYear.yearIndex];
            if (yearData != null) {

               // find dependencies
               List<String> dependencies = ParameterOutputDependenciesUtil.getInstance()
                     .getAllOutputDependencies(preModelDefinition).get(OutputParam);

               List<String> setDependencies = dependencies.stream()
                     .filter(s -> !s.startsWith("set_i")).collect(Collectors.toList());

               copyScalar(targetParameter, OutputParam, yearData, setDependencies);
            } else {
               LOG.error("BackendParametersYearData is null!");
            }
         } else {
            LOG.error("ConnectedModelParameter is null!");
         }
      }
   }

   /**
    * Wrapper to connect/synchronize input scalars which must share the same name,
    * from the [predecessorYear] to the current year.
    *
    * @param predecessorYear OptimisationYear of the previous year (or same year but from another model)
    * @param preModelDefinition Model definition of the predecessor year (for dependency query)
    * @param paramList List of parameter names which are available in both years
    */
   private void connectScalarInTOIn(OptimisationYear predecessorYear, int preModelDefinition, List<String> paramList) {
      for (String targetParameter : paramList){
         if (predecessorYear !=null && predecessorYear.yearParameters != null) {
            BackendParametersYearData yearData = predecessorYear.yearParameters;
            List<String> dependencies = ParameterInputDependenciesUtil.getInstance()
                  .getAllInputDependencies(preModelDefinition).get(targetParameter);

            copyScalar(targetParameter, targetParameter, yearData, dependencies);
         }
      }
   }

   /**
    * Copies Scalar Parameter from the last year to the current year
    * @param targetParameter copy this parameter
    * @param outputParameter equals targetParameter if DependencyType is INPUT else it starts with "par_out_"
    * @param yearData BackendParametersYearData of the last year
    * @param dependencies Dependency list corresponding to the targetParameter
    */
   private void copyScalar(String targetParameter, String outputParameter, BackendParametersYearData yearData, List<String> dependencies) {
      if (dependencies.isEmpty()) {
         Map<String, Object> params = yearData.getScalars();
         boolean isIn = params.containsKey(outputParameter);
         LOG.debug("{} is a TableScalar {}", outputParameter, isIn);
         Object param = params.get(outputParameter);
         if (param != null) {
            yearParameters.getScalars().put(targetParameter, param);
         }
      } else if (dependencies.size() == 1){
         Map<String, Set> sets = yearData.getSets();
         Set set = sets.get(dependencies.get(0));
         List<SetElement> els = set.getElements();
         List<SetElement> filteredEls = els.stream()
               .filter(t -> t.getAttributes().containsKey(outputParameter))
               .collect(Collectors.toList());

         for (SetElement filteredEl : filteredEls) {
            // Object oldValue;
            try {
               if (set.getName().equals("set_BinaryPersistData")) {
                  // IRPACT special case
                  Map<String, Set> sets2 = yearParameters.getSets();
                  Set namedSet = sets2.get(set.getName());
                  if (namedSet.getElement(filteredEl.getName()) == null)
                     namedSet.getElements().add(new SetElement(filteredEl.getName()));
               }

               Object value = filteredEl.getAttributes().get(outputParameter);
               Set setParameter = yearParameters.getSets().get(set.getName());
               SetElement eleParameter = setParameter.getElement(filteredEl.getName());
               if (eleParameter != null){
                  eleParameter.getAttributes().put(targetParameter, value);
               }
            } catch (NullPointerException e) {
               LOG.debug(targetParameter + " is not in {} {} possible inconsistent model", set, filteredEl.getName());
               e.printStackTrace();
            }
         }
      }
   }

   /**
    * Wrapper to connect/synchronize output tables to input tables which must share the same name,
    * from the [predecessorYear] ( starts with par_out_X)to the current year ( par_X).
    *
    * @param predecessorYear OptimisationYear of the previous year (or same year but from another model)
    * @param preModelDefinition Model definition of the predecessor year (for dependency query)
    * @param paramList List of parameter names which are available in both years
    */
   private void connectTablesOutToIn(OptimisationYear predecessorYear, ConnectedModelParameter year, int preModelDefinition, List<String> paramList) {
      for (String targetParameter : paramList) {
         // need to add _out_
         String OutputParam = targetParameter.replace("par_", "par_out_");
         if (year != null) {

            // get outputs of last year
            BackendParametersYearData[] yearDatas = year.getYeardata();
            BackendParametersYearData yearData = yearDatas[predecessorYear.yearIndex];
            if (yearData != null) {

               // find dependencies
               List<String> dependencies = ParameterOutputDependenciesUtil.getInstance()
                     .getAllOutputDependencies(preModelDefinition).get(OutputParam);

               List<String> setDependencies = dependencies.stream()
                     .filter(s -> !s.startsWith("set_i")).collect(Collectors.toList());

               copyTable(targetParameter, OutputParam, yearData, setDependencies);
            } else {
               LOG.error("BackendParametersYearData is null!");
            }
         } else {
            LOG.error("ConnectedModelParameter is null!");
         }
      }
   }

   /**
    * Wrapper to connect/synchronize input tables which must share the same name,
    * from the [predecessorYear] to the current year.
    *
    * @param predecessorYear OptimisationYear of the previous year (or same year but from another model)
    * @param preModelDefinition Model definition of the predecessor year (for dependency query)
    * @param paramList List of parameter names which are available in both years
    */
   private void connectTablesInToIn(OptimisationYear predecessorYear, int preModelDefinition, List<String> paramList) {
      for (String targetParameter : paramList){
         if (predecessorYear !=null && predecessorYear.yearParameters != null) {
            List<String> dependencies = ParameterInputDependenciesUtil.getInstance()
                  .getAllInputDependencies(preModelDefinition).get(targetParameter);

            copyTable(targetParameter, targetParameter, predecessorYear.yearParameters, dependencies);
         }
      }
   }

   /**
    * Copies Table Parameter from the last year to the current year
    * @param targetParameter copy this parameter
    * @param outputParameter equals targetParameter if DependencyType is INPUT else it starts with "par_out_"
    * @param yearData BackendParametersYearData of the last year
    * @param dependencies Dependency list corresponding to the targetParameter
    */
   private void copyTable(String targetParameter, String outputParameter, BackendParametersYearData yearData, List<String> dependencies) {
      if (dependencies.size() == 2) {
         // Tables mit primitiven Parametern: par_SOH_pss_sector
         Map<String, Map<String, Map<String, Object>>> params = yearData.getTableValues();
         boolean isIn = params.containsKey(outputParameter);
         LOG.debug("{} is a TableScalar {}", outputParameter, isIn);
         Map<String, Map<String, Object>> previousParam = params.get(outputParameter);
         if (previousParam !=null) {
            Map<String, Map<String, Object>> currentParam = yearParameters.getTableValues().get(targetParameter);
            Map<String, Map<String, Object>> param = filterTableParameterToCopyOnlyDefinedSubSets(currentParam, previousParam);

            yearParameters.getTableValues().put(targetParameter, param);
         }
      } else if (dependencies.size() == 3) {
         // Tables mit Zeitreihen: par_F_E_EGrid_energy
         Map<String, Map<String, Map<String, Timeseries>>> params = yearData.getTableTimeseries();
         boolean isIn = params.containsKey(outputParameter);
         LOG.debug("{} is a TableTimeSeries {}", outputParameter, isIn);
         Map<String, Map<String, Timeseries>> previousParam = params.get(outputParameter);
         if (previousParam !=null) {
            Map<String, Map<String, Object>> currentParam = yearParameters.getTableValues().get(targetParameter);
            Map<String, Map<String, Timeseries>> param = filterTableParameterToCopyOnlyDefinedSubSets(currentParam, previousParam);

            yearParameters.getTableTimeseries().put(targetParameter, param);
         }
      }
   }

   /**
    * Filter sub sets which are undefined in the scenario.
    * @param currentParam  current parameter list for the year
    * @param previousParam parameter list for the previous year
    * @param <T> generic to handle TableTimeSeries and TableScalars
    * @return new List with overwritten SubSets form the previous year which are also defined in the current year
    */
   private <T> Map<String, Map<String, T>> filterTableParameterToCopyOnlyDefinedSubSets(Map<String, Map<String, Object>> currentParam
         , Map<String, Map<String, T>> previousParam) {
      Map<String, Map<String, T>> params = new LinkedHashMap(currentParam);

      for (Map.Entry<String, Map<String, Object>> entry: currentParam.entrySet() ) {
         if (previousParam.containsKey(entry.getKey())) {
            Map<String, T> previousEntryMap = previousParam.get(entry.getKey());
            for (Map.Entry<String, Object> mapValue : currentParam.get(entry.getKey()).entrySet()) {
               String key = mapValue.getKey();
               if (previousEntryMap.containsKey(key)){
                  params.get(entry.getKey()).put(key, previousEntryMap.get(key));
               }
            }
         }
      }
      return params;
   }

   /**
    * Wrapper to connect/synchronize input timeSeries which must share the same name,
    * from the [predecessorYear] to the current year.
    *
    * @param predecessorYear OptimisationYear of the previous year (or same year but from another model)
    * @param preModelDefinition Model definition of the predecessor year (for dependency query)
    * @param paramList List of parameter names which are available in both years
    */
   private void connectTimeSeriesInToIn(OptimisationYear predecessorYear, int preModelDefinition, List<String> paramList) {
      for (String targetParameter : paramList){

         if (predecessorYear != null && predecessorYear.yearParameters != null) {

            List<String> dependencies = ParameterInputDependenciesUtil.getInstance()
                  .getAllInputDependencies(preModelDefinition).get(targetParameter);

            List<String> setDependencies = dependencies.stream()
                  .filter(s -> !s.startsWith("set_i")).collect(Collectors.toList());

            copyTimeSeries(targetParameter, targetParameter, predecessorYear.yearParameters, setDependencies);
         } else {
            LOG.error("predecessorYear.yearParameters is null");
         }
      }
   }

   /**
    * Wrapper to connect/synchronize output timeSeries to input timeSeries which must share the same name,
    * from the [predecessorYear] ( starts with par_out_X)to the current year ( par_X).
    *
    * @param predecessorYear OptimisationYear of the previous year (or same year but from another model)
    * @param preModelDefinition Model definition of the predecessor year (for dependency query)
    * @param paramList List of parameter names which are available in both years
    */
   private void connectTimeSeriesOutToIn(OptimisationYear predecessorYear, ConnectedModelParameter year, int preModelDefinition, List<String> paramList) {
      for (String targetParameter : paramList) {
         // need to add _out_
         String OutputParam = targetParameter.replace("par_", "par_out_");
         if (year != null) {

            // get outputs of last year
            BackendParametersYearData[] yearDatas = year.getYeardata();
            BackendParametersYearData yearData = yearDatas[predecessorYear.yearIndex];
            if (yearData != null) {

               // find dependencies
               List<String> dependencies = ParameterOutputDependenciesUtil.getInstance()
                           .getAllOutputDependencies(preModelDefinition).get(OutputParam);

               List<String> setDependencies = dependencies.stream()
                           .filter(s -> !s.startsWith("set_i")).collect(Collectors.toList());

               copyTimeSeries(targetParameter, OutputParam, yearData, setDependencies);
            } else {
               LOG.error("BackendParametersYearData is null!");
            }
         } else {
            LOG.error("ConnectedModelParameter is null!");
         }
      }
   }

   /**
    * Copies TimeSeries from the last year to the current year
    * @param targetParameter copy this parameter
    * @param outputParam equals targetParameter if DependencyType is INPUT else it starts with "par_out_"
    * @param yearData BackendParametersYearData of the last year
    * @param setDependencies Dependency list corresponding to the targetParameter
    */
   private void copyTimeSeries(String targetParameter, String outputParam, BackendParametersYearData yearData, List<String> setDependencies) {

      try {
         if (setDependencies.isEmpty()) {
            Map<String, Timeseries> params = yearData.getTimeseries();
            LOG.debug("{} exists: {} ", outputParam, params.containsKey(outputParam));
            Timeseries param = params.get(outputParam);
            if (param != null) {
               if (!param.hasTimeseries())
                  // TODO ist das Richtig / Wichtig?
                  param = param.loadTimeseries(false);
               yearParameters.getTimeseries().put(targetParameter, param);
            }
         } else {
            Map<String, Set> sets = yearData.getSets();
            Set set = sets.get(setDependencies.get(0));
            List<SetElement> els = set.getElements();
            List<SetElement> filteredEls = els.stream()
                  .filter(t -> t.getTimeseries().containsKey(outputParam))
                  .collect(Collectors.toList());
            if (!filteredEls.isEmpty()) {
               for (SetElement filteredEl : filteredEls) {
                  try {
                     Timeseries param = filteredEl.getTimeseries().get(outputParam);
                     // TODO ist das Richtig?
                     if (!param.hasTimeseries())
                        param = param.loadTimeseries(false);
                     Set setParameter = yearParameters.getSets().get(set.getName());
                     SetElement setElement = setParameter.getElement(filteredEl.getName());
                     if (setElement != null) {
                        setElement.getTimeseries().put(targetParameter, param);
                     }

                  } catch (NullPointerException e) {
                     LOG.debug(targetParameter + " is not in {} {} possible inconsistent model", set, filteredEl.getName());
                     e.printStackTrace();
                  }
               }
            } else {
               LOG.error("Filtered Set Elements are Empty!");
            }
         }
      } catch (NullPointerException e) {
         //something went wrong
         e.printStackTrace();
      }
   }

   private void persistStart() {
      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final OptimisationJobPersistent persistentJob = em.find(OptimisationJobPersistent.class, jobid);
         if (persistentJob.getOptimisationState().getState().equals(State.ABORTED)) {
            LOG.debug("Beende Job {} wegen Status ABORTED", jobid);
         } else {
            LOG.debug("Job {} in Status {}, beginne Jahr {}", jobid, persistentJob.getOptimisationState(), simulatedYearIndex);
         }

         final OptimisationYearPersistent year = new OptimisationYearPersistent(persistentJob, simulatedYearIndex, yearIndex, modelIndex,
               this.yearParameters.getConfig().getYear());
         final EntityTransaction et = em.getTransaction();
         et.begin();
         em.persist(year);
         em.persist(persistentJob);
         et.commit();
      }
   }

   private void persistResults() {
      if (Thread.currentThread().isInterrupted()) {
         return;
      }
      LOG.info("Persistiere Ergebnisdaten");
      BackendParametersYearData yeardata = caller.getResult(postProcessors[yearIndex], yearParameters.getConfig());
      result.getModels()[modelIndex].getYeardata()[yearIndex] = yeardata;
      // addYearCSVResults(postProcessors[yearIndex]);
      if (Thread.currentThread().isInterrupted()) {
         return;
      }
   }

   public void kill() {
      if (caller != null) {
         caller.kill();
      }
      OptimisationYearHandler.kill(this);
   }

   public YearState getState() {
      final YearState state = new YearState();
      state.setStart(start);
      state.setYear(yearParameters.getConfig().getYear());
      return state;
   }

   public long getJobid() {
      return jobid;
   }

   public int getYearIndex() {
      return yearIndex;
   }

   public boolean isStartable() {
      if (job.getGenericParameters().getModels().length == 1) {
         return true;
      } else {
         OptimisationYear year = getPredecessorYear();
         if (year == null) {
            return true;
         } else
            return year.finished;
      }
   }

   private OptimisationYear getPredecessorYear() {
      int thisIndex = job.getCurrentYears().indexOf(this);
      OptimisationYear year;
      if (thisIndex > 0) {
         year = job.getCurrentYears().get(thisIndex - 1);
      } else {
         year = null;
      }
      return year;
   }

   private OptimisationYear getPreviousActYear(OptimisationYear currentYear) {
      List<OptimisationYear> filter = job.getCurrentYears()
            .stream()
            .filter(t -> t.yearParameters.getConfig().getModeldefinition() == IRPACT_MODELDEFINITION)
            .collect(Collectors.toList());
      int idx = filter.indexOf(currentYear);
      OptimisationYear year;
      if (idx > 0 ) {
         year = job.getCurrentYears().get(idx - 1);
      } else {
         year = null;
      }
      return year;
   }

   @Override
   public String toString() {
      return "Year " + jobid + "-" + modelIndex + "-" + yearIndex;
   }

}
