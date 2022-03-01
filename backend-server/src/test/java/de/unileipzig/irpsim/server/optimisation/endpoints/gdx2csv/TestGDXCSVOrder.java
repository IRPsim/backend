package de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading.DataMapReader;
import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading.GDXSQLiteData;

public class TestGDXCSVOrder {
   
   @Test
   public void testOrder() throws IOException {
      List<RequestedParametersYear> requestedParameters = createRequest();
      
      DataMapReader gdxdata = Mockito.mock(DataMapReader.class);
      Map<Integer, Map<Integer, GDXSQLiteData>> value = new LinkedHashMap<>();
      Mockito.when(gdxdata.getGdxDataMap()).thenReturn(value);
      Map<Integer, GDXSQLiteData> simulation = new LinkedHashMap<>();
      simulation.put(0, Mockito.mock(GDXSQLiteData.class));
      value.put(0, simulation);

      CSVWriter writer = new CSVWriter(gdxdata, requestedParameters);
      
      String data = writer.write();
      
      String[] metadata = data.split(";");
      Assert.assertEquals("0-0-var_energyFlow-E-EMarket-EGrid", metadata[2]);
      Assert.assertEquals("0-0-var_energyFlow-E-EGrid-tech_EY1", metadata[3]);
      Assert.assertEquals("0-0-var_energyFlow-CL-tech_EY1-tech_CLS1", metadata[4]);
      Assert.assertEquals("0-0-var_energyFlow-CL-tech_CLS1-load_CL1", metadata[5]);
      Assert.assertEquals("0-0-var_energyFlow-CL-CLMarket-load_CL1", metadata[6]);
      
      System.out.println(data);
   }
   
   private List<RequestedParametersYear> createRequest() {
      final List<List<String>> combinations = Arrays.asList(
            Arrays.asList("E", "EMarket", "EGrid"), 
            Arrays.asList("E", "EGrid", "tech_EY1"),
            Arrays.asList("CL", "tech_EY1", "tech_CLS1"),
            Arrays.asList("CL", "tech_CLS1", "load_CL1"),
            Arrays.asList("CL", "CLMarket", "load_CL1"));
      
      List<RequestedParametersYear> requestedParameters = new LinkedList<>();
      RequestedParametersYear year = new RequestedParametersYear();
      Combination combination = new Combination();
      combination.setSet_ii(ParameterdataWriter.VIERTELSTUNDENWERTE);
      combination.setCombinations(combinations);
      year.setParameters(new LinkedHashMap<>());
      year.getParameters().put("var_energyFlow", combination);
      requestedParameters.add(year);
      return requestedParameters;
   }
}
