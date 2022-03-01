package de.unileipzig.irpsim.server.optimisation.comparators.modelDifferences;

import de.unileipzig.irpsim.core.simulation.data.persistence.State;

public class PerformanceModel {

   private final String inputJson;
   // in Sec
   private final Long runtime;
   // or simulationID
   private final Long jobId;

   private final State state;

   public PerformanceModel(Long id, String inputJson, long runtime, State state) {
      this.jobId = id;
      this.inputJson = inputJson;
      this.runtime = runtime;
      this.state = state;
   }

   public String getInputJson() {
      return this.inputJson;
   }

   public Long getRuntime() {
      return this.runtime;
   }

   public Long getJobId() {
      return this.jobId;
   }

   public State getState(){return this.state;}
}
