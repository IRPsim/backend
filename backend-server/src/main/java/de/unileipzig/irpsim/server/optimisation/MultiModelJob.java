package de.unileipzig.irpsim.server.optimisation;

import de.unileipzig.irpsim.core.simulation.data.BackendParametersMultiModel;
import de.unileipzig.irpsim.core.simulation.data.ConnectedModelParameter;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.utils.PersistenceFolderUtil;
import de.unileipzig.irpsim.server.optimisation.persistence.OptimisationJobPersistenceManager;
import de.unileipzig.irpsim.server.optimisation.postprocessing.PostProcessorHandler;

public class MultiModelJob extends Job {

   public MultiModelJob(JSONParametersMultimodel parameters, boolean useInterpolation, boolean interpolateTimeseries) {
      super(useInterpolation, interpolateTimeseries);
      this.genericParameters = new BackendParametersMultiModel(parameters);

      this.genericResults = new BackendParametersMultiModel(parameters.getModels().size(), parameters.getModels().get(0).getYears().size());
      configurePersistentJob(parameters.getModeldefinition(), persistentJob);
      persistencemanager = new OptimisationJobPersistenceManager(persistentJob);
      jobid = persistentJob.getId();

      postProcessors = new PostProcessorHandler[genericParameters.getAllYears().size()];

      this.workspaceFolder = makeWorkspaceFolder(PersistenceFolderUtil.getRunningJobFolder());
   }

   @Override
   protected void addYearsToQueue() {
      for (int modelIndex = 0; modelIndex < genericParameters.getModels().length; modelIndex++) {
         ConnectedModelParameter model = genericParameters.getModels()[modelIndex];
         addModelYearsToQueue(modelIndex, model);
      }
   }

   @Override
   protected void interpolate() {
      // Interpolation was originally wanted by IWB, but is no longer required. It is left in single model jobs but will not be implemented for multi model jobs for now.
   }

   @Override
   protected void postProcess() {
      // Postprocessing was originally wanted by IWB, but is no longer required. It is left in single model jobs but will not be implemented for multi model jobs for now.
   }

   @Override
   public BackendParametersMultiModel fetchPostProcessingResults() {
      // Postprocessing was originally wanted by IWB, but is no longer required. It is left in single model jobs but will not be implemented for multi model jobs for now.
      return null;
   }

   @Override
   protected int getFinishedSteps() {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   protected String getModelVersionInfo() {
      // TODO Auto-generated method stub
      return null;
   }

}
