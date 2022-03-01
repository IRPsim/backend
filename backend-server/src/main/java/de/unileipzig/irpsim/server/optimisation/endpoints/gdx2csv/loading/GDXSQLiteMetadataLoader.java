package de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.ParametermetaData;

class GDXSQLiteMetadataLoader{
   
   private static final Logger LOG = LogManager.getLogger(GDXSQLiteMetadataLoader.class);
   private final Map<String, ParametermetaData> data = new HashMap<>();

   public GDXSQLiteMetadataLoader(File sqlite) {
      final String query = "SELECT name FROM main.sqlite_master WHERE type='table';";

      try (final Connection connection = DriverManager.getConnection("jdbc:sqlite:" + sqlite.getAbsolutePath());
            final ResultSet rs = connection.createStatement().executeQuery(query)) {

         while (rs.next()) {
            final String tableName = rs.getString(1);
            LOG.trace(tableName);
            readTable(connection, tableName);
         }
      } catch (final SQLException e) {
         e.printStackTrace();
      }
   }
   
   public Map<String, ParametermetaData> getData() {
      return data;
   }
   
   private void readTable(final Connection connection, final String tableName) throws SQLException {
      if (tableName.startsWith("par_") || tableName.startsWith("sca_") || tableName.startsWith("var_")) {
         final List<String> columns = getTableColumns(connection, tableName);
         final ParametermetaData currentParameter = new ParametermetaData(tableName, columns);
         data.put(tableName, currentParameter);
         
         if (columns.size() > 0) {
            readElements(connection, tableName, currentParameter, columns);
         }
      } else if (tableName.equals("scalars")) {
         try (final ResultSet values = connection.createStatement().executeQuery("SELECT DISTINCT name, value FROM scalars")) {
            while (values.next()) {
               String scalar = values.getString("name");
               ParametermetaData metadata = new ParametermetaData(scalar, new LinkedList<>());
               data.put(scalar, metadata);
            }
         }
      }
   }
   
   private void readElements(final Connection connection, final String tableName, final ParametermetaData currentParameter, final List<String> columns) throws SQLException {
      String selectString = "";
      for (String dependent : columns) {
         selectString += dependent + ",";
      }
      selectString = selectString.substring(0, selectString.length() - 1);

      try (final ResultSet values = connection.createStatement().executeQuery("SELECT DISTINCT " + selectString + " FROM " + tableName)) {

         while (values.next()) {
            final List<String> dependents = new LinkedList<>();
            for (final String column : columns) {
               final String dependent = values.getString(column);
               dependents.add(dependent);
            }
            currentParameter.getDependencies().add(dependents);
         }
      }
   }

   private List<String> getTableColumns(final Connection connection, final String tableName) throws SQLException {
      final List<String> column = new LinkedList<>();
      try (final ResultSet columns = connection.createStatement().executeQuery("PRAGMA table_info(" + tableName + ")")) {
         while (columns.next()) {
            column.add(columns.getString("name"));
         }
         return column;
      }
   }

}