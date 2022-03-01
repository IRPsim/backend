package de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class ParametermetaData {

   private static final Logger LOG = LogManager.getLogger(ParametermetaData.class);

   private final String name;
   private final List<String> sets;
   private Set<List<String>> dependencies = new HashSet<>();
   private final boolean scalar;
   private final boolean containsSetII0;

   public ParametermetaData(String name, List<String> sets) {
      this.name = name;
      scalar = !(sets.contains("set_ii") || sets.contains("set_ii_0"));
      containsSetII0 = sets.contains("set_ii_0");
      sets.remove("set_ii");
      sets.remove("set_ii_0");
      sets.remove("up");
      sets.remove("lo");
      sets.remove("level");
      sets.remove("marginal");
      sets.remove("value");
      this.sets = sets;
   }

   public boolean isScalar() {
      return scalar;
   }

   public Set<List<String>> getDependencies() {
      return dependencies;
   }

   public void setDependencies(final Set<List<String>> values) {
      this.dependencies = values;
   }

   public List<String> getSets() {
      return sets;
   }

   public boolean isContainsSetII0() {
      return containsSetII0;
   }

   @JsonIgnore
   public String getDataQuery(final List<String> dependents) {
      if (dependents.size() == 0 && scalar) {
         return "SELECT value FROM scalars WHERE name='" + name + "'";
      } else {
         String whereString = buildWhere(name, dependents);
         String valueName = getValueName();
         final String fullQuery = "SELECT " +
               (!isScalar() ? getTimeseriesName() + "," : "") +
               valueName + " FROM " + name + whereString;
          LOG.debug(fullQuery);
         return fullQuery;
      }
   }

   @JsonIgnore
   public String getValueName() {
      String valueName;
      if (name.startsWith("var_")) {
         valueName = "level";
      } else {
         valueName = "value";
      }
      return valueName;
   }

   @JsonIgnore
   public String getTimeseriesName() {
      String timeseriesName;
      if (isContainsSetII0()) {
         timeseriesName = "set_ii_0";
      } else if (!isScalar()) {
         timeseriesName = "set_ii";
      } else {
         throw new RuntimeException("Trying to get timeseries name for scalar " + name);
      }
      return timeseriesName;
   }

   private String buildWhere(final String parameterName, final List<String> dependents) {
      String whereString = " WHERE ";

      for (int i = 0; i < dependents.size(); i++) {
         final String column = sets.get(i);
         whereString += column + " = '" + dependents.get(i) + "' AND ";
      }
      if (isContainsSetII0()) {
         whereString += "set_ii_0 != 'ii0'";
      } else {
         whereString = whereString.substring(0, whereString.length() - 4);
      }
      if (!isScalar()) {
         whereString += " ORDER BY " + getTimeseriesName();
      }
      return whereString;
   }

}