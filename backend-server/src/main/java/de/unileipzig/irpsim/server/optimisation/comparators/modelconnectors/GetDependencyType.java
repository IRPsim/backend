package de.unileipzig.irpsim.server.optimisation.comparators.modelconnectors;

import java.util.List;

import de.unileipzig.irpsim.core.utils.ParameterInputDependenciesUtil;
import de.unileipzig.irpsim.core.utils.ParameterOutputDependenciesUtil;

public class GetDependencyType {
   public static DependencyType getDependencyType(String parameter, boolean input, int modelid) {
      List<String> dependencies;
      if (input == true) {
         dependencies = ParameterInputDependenciesUtil.getInstance().getAllInputDependencies(modelid).get(parameter);
      } else {
         dependencies = ParameterOutputDependenciesUtil.getInstance().getOutputParameterDependencies(parameter, modelid);
      }
      if (dependencies.size() == 0) {
         return DependencyType.SCALAR;
      }
      if (dependencies.size() == 1) {
         if (dependencies.get(0).equals("set_ii")) {
            return DependencyType.TIMESERIES;
         } else {
            return DependencyType.SET_SCALAR;
         }
      }
      if (dependencies.size() == 2) {
         if (dependencies.get(0).equals("set_ii")) {
            return DependencyType.SET_TIMESERIES;
         } else {
            return DependencyType.TABLE_SCALAR;
         }
      }
      if (dependencies.size() == 3) {
         if (dependencies.get(0).equals("set_ii")) {
            return DependencyType.TABLE_TIMESERIES;
         } else {
            throw new RuntimeException("Unexpected: size 3 and no set_ii as dependency 0");
         }
      }
      return null;
   }
}
