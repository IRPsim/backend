package de.unileipzig.irpsim.server.optimisation.json2sqlite;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.json.YearData;
import de.unileipzig.irpsim.core.utils.ParameterBaseDependenciesUtil;
import de.unileipzig.irpsim.core.utils.TestFiles;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading.GDXSQLiteData;

public class TestJSONTransformation {

   private static final File sqliteFile = new File("target/test.sqlite");

   private static GDXSQLiteData data;

   @BeforeClass
   public static void executeTransformation() throws SQLException, JsonParseException, JsonMappingException, IOException {
      sqliteFile.delete();

      File testFile = TestFiles.TEST.make();

      JSONParametersMultimodel mm = Constants.MAPPER.readValue(testFile, JSONParametersMultimodel.class);
      YearData yearData = mm.getModels().get(0).getYears().get(0);
      removeTablesWithDuplicatedColumnNames(yearData);

      new TransformJSON(yearData).transform(sqliteFile);
      data = new GDXSQLiteData(672, sqliteFile);
   }

   /**
    * Function for removal of tables with duplicated column names.
    * Duplicated column names can exist within the input json and will be only removed for this test case.
    * @param yearData contains the relevant parameter
    */
   private static void removeTablesWithDuplicatedColumnNames(YearData yearData) {
      List<String> removeFromTabels = new LinkedList<>();
      for (Map.Entry<String, Map<String, Map<String, Object>>> parameterEntry : yearData.getTables().entrySet()) {
         String parameterName = parameterEntry.getKey();
         List<String> dependencies = ParameterBaseDependenciesUtil.getInstance().getDependencies(parameterName, yearData.getConfig().getModeldefinition());
         dependencies.remove("set_ii");
         dependencies.remove("set_ii_0");
         for (Map.Entry<String, Map<String, Object>> firstDependentMap : parameterEntry.getValue().entrySet()) {
            String firstDependent = firstDependentMap.getKey();
            for (Map.Entry<String, Object> secondDependentMap : firstDependentMap.getValue().entrySet()) {
               String secondDepentent = secondDependentMap.getKey();
               if (firstDependent.equals(secondDepentent)){
                  System.out.println("remove "+ parameterName);
                  removeFromTabels.add(parameterName);
               }
            }
         }
      }

      for (String parameter : removeFromTabels){
         yearData.getTables().remove(parameter);
      }
   }

   @Test
   public void testScalars() throws JsonParseException, JsonMappingException, IOException, SQLException {
      double[] values = data.getValues("sca_X_MS_CHF_C_RF", new LinkedList<>());
      Assert.assertEquals(1.0, values[0], 0.001);
   }
   
   @Test
   public void testTimeseries() throws JsonParseException, JsonMappingException, IOException, SQLException {
      double[] valuesTimeseries = data.getValues("par_C_MS_RF", new LinkedList<>());
      Assert.assertEquals(672, valuesTimeseries.length);
   }
   
   @Test
   public void testSingleDependent() throws JsonParseException, JsonMappingException, IOException, SQLException {
      List<String> dependents = new LinkedList<>();
      dependents.add("tech_CLS1");
      double[] values = data.getValues("par_Alpha_DES_CLS_OuM", dependents);
      Assert.assertEquals(0.3, values[0], 0.001);
   }
   
   @Test
   public void testSingleDependentTimeseries() throws JsonParseException, JsonMappingException, IOException, SQLException {
      List<String> dependents = new LinkedList<>();
      dependents.add("tech_CLS1");
      double[] valuesTimeseries = data.getValues("par_SOC_DES_CLS_utilpercent", dependents);
      Assert.assertEquals(672, valuesTimeseries.length);
   }
   
   @Test
   public void testTableScalar() throws JsonParseException, JsonMappingException, IOException, SQLException {
      List<String> dependents = new LinkedList<>();
      dependents.add("SMS");
      dependents.add("Sonnentank");
      double[] values = data.getValues("par_OH_Side_BM", dependents);
      Assert.assertEquals(1.0, values[0], 0.001);
   }

}
