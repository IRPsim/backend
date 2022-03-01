package de.unileipzig.irpsim.server.optimisation.json2sqlite;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.simulation.data.json.YearData;
import de.unileipzig.irpsim.core.utils.ParameterBaseDependenciesUtil;

public class TransformJSON {

   private final YearData input;
   private static final Logger LOG = LogManager.getLogger(TransformJSON.class);

   public TransformJSON(YearData input) {
      this.input = input;

   }

   public void transform(File sqlite) throws SQLException {
      try (final Connection connection = DriverManager.getConnection("jdbc:sqlite:" + sqlite.getAbsolutePath())) {

         Statement statement = connection.createStatement();

         transformScalars(statement);
         transformTimeseries(statement);
         transformSets(statement);

         for (Entry<String, Map<String, Map<String, Object>>> parameterEntry : input.getTables().entrySet()) {
            String parameterName = parameterEntry.getKey();
            List<String> dependencies = ParameterBaseDependenciesUtil.getInstance().getDependencies(parameterName, input.getConfig().getModeldefinition());
            dependencies.remove("set_ii");
            dependencies.remove("set_ii_0");
            String parentSetName = dependencies.get(0);
            for (Entry<String, Map<String, Object>> firstDependentMap : parameterEntry.getValue().entrySet()) {
               String firstDependent = firstDependentMap.getKey();
               for (Entry<String, Object> secondDependentMap : firstDependentMap.getValue().entrySet()) {
                  String secondDepentent = secondDependentMap.getKey();
                  if (secondDependentMap.getValue() instanceof Number) {
                     statement.execute("CREATE TABLE IF NOT EXISTS " + parameterName + "(" + parentSetName + " TEXT, " + dependencies.get(1) + " TEXT, value REAL)");
                     statement.execute("INSERT INTO main." + parameterName + "(" + parentSetName + ", " + dependencies.get(1) + ", value)"
                           + "VALUES ('" + firstDependent + "', '" + secondDepentent + "', "
                           + secondDependentMap.getValue() + ")");
                  } else if (secondDependentMap.getValue() instanceof ArrayList) {

                  }
               }
            }
         }

      }
   }

   private void transformTimeseries(Statement statement) throws SQLException {
      for (Map.Entry<String, Object> timeseries : input.getTimeseries().entrySet()) {
         String parameterName = timeseries.getKey();
         if (timeseries.getValue() instanceof ArrayList) {
            List values = (List) timeseries.getValue();

            String query = "CREATE TABLE IF NOT EXISTS " + parameterName + "(set_ii TEXT, value REAL)";
            LOG.debug(query);
            statement.execute(query);
            fillTimeseries(statement, parameterName, values);
         }
      }
   }

   private void transformSets(Statement statement) throws SQLException {
      for (Entry<String, LinkedHashMap<String, LinkedHashMap<String, Object>>> set : input.getSets().entrySet()) {
         for (Entry<String, LinkedHashMap<String, Object>> setElement : set.getValue().entrySet()) {
            final String setElementName = setElement.getKey();
            for (Entry<String, Object> parameter : setElement.getValue().entrySet()) {
               String parameterName = parameter.getKey();
               List<String> dependencies = ParameterBaseDependenciesUtil.getInstance().getDependencies(parameterName, input.getConfig().getModeldefinition());
               dependencies.remove("set_ii");
               dependencies.remove("set_ii_0");
               String parentSetName = dependencies.get(0);
               if (parameter.getValue() instanceof Number) {
                  statement.execute("CREATE TABLE IF NOT EXISTS " + parameterName + "(" + parentSetName + " TEXT, value REAL)");
                  statement.execute("INSERT INTO main." + parameterName + "(" + parentSetName + ", value) VALUES ('" + setElementName + "', " + parameter.getValue() + ")");
               } else if (parameter.getValue() instanceof ArrayList) {
                  transformSetTimeseries(statement, setElementName, parameter, parameterName, parentSetName);
               } else {
                  LOG.debug("Unkown parameter: {}", parameter.getValue().getClass());
               }
            }
         }
      }
   }

   private void transformSetTimeseries(Statement statement, final String setElementName, Entry<String, Object> parameter, String parameterName, String parentSetName)
         throws SQLException {
      String query = "CREATE TABLE IF NOT EXISTS " + parameterName + "(set_ii TEXT, " + parentSetName + " TEXT, value REAL)";
      LOG.debug(query);
      statement.execute(query);
      List values = (List) parameter.getValue();
      fillTimeseriesSet(statement, setElementName, parameterName, parentSetName, values);
   }
   
   private void fillTimeseries(Statement statement, String parameterName, List values) throws SQLException {
      String insertQuery = "INSERT INTO main." + parameterName + "(set_ii, value) VALUES ";
      for (int i = 0; i < values.size(); i++) {
         double value = (double) values.get(i);
         insertQuery += "('ii" + (i + 1) + "', " + value + "),";
      }
      insertQuery = insertQuery.substring(0, insertQuery.length() -1);
      statement.execute(insertQuery);
   }

   private void fillTimeseriesSet(Statement statement, final String setElementName, String parameterName, String parentSetName, List values) throws SQLException {
      String insertQuery = "INSERT INTO main." + parameterName + "(set_ii, " + parentSetName + ", value) VALUES ";
      for (int i = 0; i < values.size(); i++) {
         double value = (double) values.get(i);
         insertQuery+= "('ii" + (i + 1) + "','" + setElementName + "', " + value + "),";
      }
      insertQuery = insertQuery.substring(0, insertQuery.length() -1);
      statement.execute(insertQuery);
   }

   private void transformScalars(Statement statement) throws SQLException {
      String request = "CREATE TABLE scalars(name TEXT PRIMARY KEY, value REAL);";
      statement.execute(request);

      for (Map.Entry<String, Object> entry : input.getScalars().entrySet()) {
         String insert = "INSERT INTO main.scalars(name, value) VALUES ('" + entry.getKey() + "', '" + entry.getValue() + "')";
         statement.execute(insert);
      }
   }
}
