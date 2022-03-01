package de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;

public class ParameterMetadataTest {
   @Test
   public void testSimpleTimeseries() throws IOException {
      List<String> dependencies = new LinkedList<>(Arrays.asList(new String[] {"set_ii"}));
      ParametermetaData pd = new ParametermetaData("par_out_IuO_Sector_Cust", dependencies);
      
      String query = pd.getDataQuery(new LinkedList<>());
      
      MatcherAssert.assertThat(query, Matchers.containsString("FROM par_out_IuO_Sector_Cust"));
      
   }
   
   @Test
   public void testScalar() throws IOException {
      List<String> dependencies = new LinkedList<>();
      ParametermetaData pd = new ParametermetaData("sca_C_MS_B", dependencies);
      
      String query = pd.getDataQuery(new LinkedList<>());
      
      MatcherAssert.assertThat(query, Matchers.containsString("SELECT value FROM scalars WHERE"));
      
   }
}
