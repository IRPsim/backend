package de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.ParametermetaData;

/**
 * Handles raw SQLite-Data by saving the metadata and providing access to the data itself; does not provide transformed data
 * 
 * @author reichelt
 *
 */
public class GDXSQLiteData {

   private final Map<String, ParametermetaData> data;
   private final int steps;
   private final File sqlite;

   public GDXSQLiteData(final int steps, final File sqlite) {
      this.sqlite = sqlite;
      this.steps = steps;
      data = new GDXSQLiteMetadataLoader(sqlite).getData();
   }

   public int getSteps() {
      return steps;
   }

   public Map<String, ParametermetaData> getData() {
      return data;
   }

   public double[] getValues(final String parameterName, final List<String> dependents) {
      double[] result = new double[steps];
      final ParametermetaData parameter = data.get(parameterName);
      final String fullQuery = parameter.getDataQuery(dependents);

      try (final Connection connection = DriverManager.getConnection("jdbc:sqlite:" + sqlite.getAbsolutePath());
            final ResultSet values = connection.createStatement().executeQuery(fullQuery)) {

         if (!parameter.isScalar()) {
            readTimeseries(result, parameter, values);
         } else {
            final double value = values.getDouble(parameter.getValueName());
            result[0] = value;
         }
         return result;
      } catch (final SQLException e) {
         e.printStackTrace();
      }
      return null;

   }

   private void readTimeseries(double[] result, final ParametermetaData parameter, final ResultSet values) throws SQLException {
      while (values.next()) {
         int index = Integer.parseInt(values.getString(parameter.getTimeseriesName()).replace("ii", "")) - 1;
         final double value = values.getDouble(parameter.getValueName());
         result[index] = value;
      }
   }

}