package de.unileipzig.irpsim.core.simulation.data;

import java.util.List;

import de.unileipzig.irpsim.core.simulation.data.json.UserDefinedDescription;

public interface GenericParameters {
   
   UserDefinedDescription getDescription();
   
   List<BackendParametersYearData> getAllYears();

   int fetchSimulationLength();
   
   Object createJSONParameters();
}
