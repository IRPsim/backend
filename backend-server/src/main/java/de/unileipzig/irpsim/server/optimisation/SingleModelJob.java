package de.unileipzig.irpsim.server.optimisation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.simulation.data.BackendParametersMultiModel;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.ConnectedModelParameter;
import de.unileipzig.irpsim.core.simulation.data.PostProcessing;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.persistence.State;
import de.unileipzig.irpsim.core.utils.PersistenceFolderUtil;
import de.unileipzig.irpsim.server.data.modeldefinitions.ModelDefinitionsEndpoint;
import de.unileipzig.irpsim.server.optimisation.persistence.OptimisationJobPersistenceManager;
import de.unileipzig.irpsim.server.optimisation.postprocessing.MultipleYearPostprocessorHandler;
import de.unileipzig.irpsim.server.optimisation.postprocessing.PostProcessorHandler;

public class SingleModelJob extends Job {
   
   private static final Logger LOG = LogManager.getLogger(SingleModelJob.class);
   
   private final ConnectedModelParameter model;
   
   public SingleModelJob(JSONParametersMultimodel gp, boolean useInterpolation, boolean interpolateTimeseries) {
      super(useInterpolation, interpolateTimeseries);
      
      genericParameters = new BackendParametersMultiModel(gp);
      
      model = genericParameters.getModels()[0];
      
      genericResults = new BackendParametersMultiModel(1, gp.getModels().get(0).getYears().size());
      
      postProcessors = new PostProcessorHandler[gp.getModels().get(0).getYears().size()];
      
      configurePersistentJob(gp.getModels().get(0).getYears().get(0).getConfig().getModeldefinition(), persistentJob);
      persistencemanager = new OptimisationJobPersistenceManager(persistentJob);
      jobid = persistentJob.getId();
      
      // Id is needed, so this should happen after persisting to database
      this.workspaceFolder = makeWorkspaceFolder(PersistenceFolderUtil.getRunningJobFolder());
      LOG.debug("Basisdaten persistiert, ID: {} {}", persistentJob.getId(), PersistenceFolderUtil.getRunningJobFolder().getAbsolutePath());
   }
   
   protected String getModelVersionInfo() {
      return ModelDefinitionsEndpoint.getModelVersion(model.getYeardata()[0].getConfig().getModeldefinition());
   }
   
   
   
   protected synchronized void addYearsToQueue() {
      addModelYearsToQueue(0, model);
   }
   
   /**
    * Interpoliert alle Jahre zwischen diesem und dem letzten berechneten Jahr.
    *
    * @param toInterpolate Anzahl zu interpolierender Jahre
    * @param totalYearIndex Index des nachfolgenden Stützjahres
    */
   private void interpolateYears(final int totalYearIndex, final int toInterpolate) {
      if (totalYearIndex <= toInterpolate) {
         LOG.error("Leere Jahre am Anfang des Dokuments!");
         return;
      }
      final BackendParametersYearData yearBefore = genericResults.getModels()[0].getYeardata()[totalYearIndex - toInterpolate - 1];
      final BackendParametersYearData yearAfter = genericResults.getModels()[0].getYeardata()[totalYearIndex];

      for (int yearIndex = totalYearIndex - toInterpolate; yearIndex < totalYearIndex; yearIndex++) {
         final int weightBefore = totalYearIndex - yearIndex;
         final int weightAfter = toInterpolate + 1 - weightBefore;

         final InterpolationHandler interpolationHandler = new InterpolationHandler(yearBefore, weightBefore, yearAfter, weightAfter);
         if (interpolateTimeseries) {
            interpolationHandler.interpolateCompleteYearData();
         } else {
            interpolationHandler.interpolatePostprocessing();
         }
         final BackendParametersYearData yearData = interpolationHandler.getYearData();
         genericResults.getModels()[0].getYeardata()[yearIndex] = yearData;
      }
   }
   
   protected void postProcess() {
      setState(State.POSTPROCESSING);
      final PostProcessing postprocessingResults = postProcessing();
      genericResults.getModels()[0].setPostprocessing(postprocessingResults);
   }
   
   public void interpolate() {
      int currentInterpolationCount = 0;
      int yearIndex = 0;
      for (final BackendParametersYearData yeardata : model.getYeardata()) {
         if (yeardata == null) {
            yearIndex++;
            if (useInterpolation) {
               currentInterpolationCount++;
            }
            continue;
         }

         if (currentInterpolationCount > 0 && !hasError) {
            LOG.info("Interpoliere Zwischenjahre: {} {}", yearIndex, currentInterpolationCount);
            setState(State.INTERPOLATING);
            interpolateYears(yearIndex, currentInterpolationCount);
            persistencemanager.persistInterpolatedYears(currentInterpolationCount, genericResults.createJSONParameters());
            currentInterpolationCount = 0;
            LOG.info("Interpolation beendet");
         }

         LOG.debug("Nachverarbeitung beendet");
         if (yearIndex < model.getYeardata().length) {
            yearIndex++;
         }
      }
   }
   
   public PostProcessing postProcessing() {
      Object interestObject = model.getYeardata()[0].getScalars().get("sca_i_DES");
      if (interestObject != null) {
         final double interest = 1 + (double) interestObject;
         final MultipleYearPostprocessorHandler finalHandler = new MultipleYearPostprocessorHandler(interest, model.getYeardata()[0].getConfig().getModeldefinition());

         int yearIndex = 0;
         for (final BackendParametersYearData yearData : genericResults.getModels()[0].getYeardata()) {
            if (yearData != null) {
               finalHandler.addResult(yearData.getPostprocessing(), yearIndex);
            } else if (useInterpolation) {
               LOG.error("Kein Postprocessing in Jahr {}!");
            }
            yearIndex++;
         }
         final PostProcessing postprocessingResults = finalHandler.fetchPostprocessingResults();
         return postprocessingResults;
      } else {
         return null;
      }
   }
   
   /**
    * Gibt die Anzahl der beendeten Simulationsschritte zurück.
    *
    * @return Anzahl der beendeten Simulationsschritte.
    */
   public int getFinishedSteps() {
      int finishedSteps = 0;
      for (final PostProcessorHandler handler : postProcessors) {
         if (handler != null) {
            finishedSteps += handler.fetchFinishedSteps();
         }
      }
      return finishedSteps;
   }
   
   /**
    * Gibt die aggregierten Ergebnisdaten des Optimierungslaufes aus, ggf. noch unvollständig.
    *
    * @return Aggregierte Ergebnisdaten
    */
   public BackendParametersMultiModel fetchPostProcessingResults() {
      final BackendParametersMultiModel postProcessingResult = new BackendParametersMultiModel(1, postProcessors.length);
      ConnectedModelParameter singleModelParameters = postProcessingResult.getModels()[0];
      for (int yearIndex = 0; yearIndex < postProcessors.length; yearIndex++) {
         final PostProcessorHandler yearPostProcessingHandler = postProcessors[yearIndex];
         if (yearPostProcessingHandler != null) {
            final int year = model.getYeardata()[yearIndex].getConfig().getYear();
            singleModelParameters.getYeardata()[yearIndex] = yearPostProcessingHandler.fetchCurrentResults();
            singleModelParameters.getYeardata()[yearIndex].getConfig().setYear(year);
         }
      }
      return postProcessingResult;
   }
}
