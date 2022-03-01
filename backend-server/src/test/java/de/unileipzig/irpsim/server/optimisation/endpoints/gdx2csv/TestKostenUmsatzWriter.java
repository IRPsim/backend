package de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading.DataMapReader;
import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading.GDXSQLiteData;

public class TestKostenUmsatzWriter {
   
   private DataMapReader mapReader;

   @Before
   public void init() {
      mapReader = buildDataMap();
   }
   
   @Test
   public void testKostenUmsatz2Cost1RevenueWriter() throws Exception {
      final List<List<String>> orga = Arrays.asList(Arrays.asList("W", "WGrid", "load_W1"), Arrays.asList("E", "EMarket", "EGrid"));
      final List<List<String>> cust = Arrays.asList(Arrays.asList("W", "WGrid", "load_W1"));
      final List<RequestedParametersYear> requestedParameters = initParameters(2, orga, cust);

      final String data = new KostenUmsatzWriter(mapReader, requestedParameters).write();
      final String expected = "0.0;21.0;15;1;[ID:1][E, EMarket, EGrid]" + "\n" +
            "21.2;21.1;15;1;[ID:1][W, WGrid, load_W1]" + "\n" +
            "0.0;21.1;15;2;[ID:2][E, EMarket, EGrid]" + "\n" +
            "21.3;21.2;15;2;[ID:2][W, WGrid, load_W1]" + "\n";
      Assert.assertEquals(expected, data);
   }

   @Test
   public void testKostenUmsatz1Cost1RevenueWriter() throws Exception {
      final List<List<String>> orga = Arrays.asList(Arrays.asList("E", "EMarket", "EGrid"));
      final List<List<String>> cust = Arrays.asList(Arrays.asList("W", "WGrid", "load_W1"));
      final List<RequestedParametersYear> requestedParameters = initParameters(1, orga, cust);

      final String data = new KostenUmsatzWriter(mapReader, requestedParameters).write();
      final String expected = "0.0;21.0;15;1;[ID:1][E, EMarket, EGrid]" + "\n" + "21.2;0.0;15;1;[ID:1][W, WGrid, load_W1]" + "\n";
      Assert.assertEquals(expected, data);
   }

   @Test
   public void testKostenUmsatz1Cost1RevenueSwitchedOrderWriter() throws Exception {
      final List<List<String>> orga = Arrays.asList(Arrays.asList("W", "WGrid", "load_W1"));
      final List<List<String>> cust = Arrays.asList(Arrays.asList("E", "EMarket", "EGrid"));
      final List<RequestedParametersYear> requestedParameters = initParameters(1, orga, cust);

      final String data = new KostenUmsatzWriter(mapReader, requestedParameters).write();
      final String expected = "21.3;0.0;15;1;[ID:1][E, EMarket, EGrid]" + "\n" + "0.0;21.1;15;1;[ID:1][W, WGrid, load_W1]" + "\n";
      Assert.assertEquals(expected, data);
   }

   private DataMapReader buildDataMap() {
      final DataMapReader mapReader = Mockito.mock(DataMapReader.class);
      Map<Integer, Map<Integer, GDXSQLiteData>> dataMap = new HashMap<>();
      dataMap.put(1, getIntegerGDXSQLiteDataMap(0));
      dataMap.put(2, getIntegerGDXSQLiteDataMap(0.1));

      PowerMockito.when(mapReader.getGdxDataMap()).thenReturn(dataMap);
      return mapReader;
   }

   private Map<Integer, GDXSQLiteData> getIntegerGDXSQLiteDataMap(double x) {
      Map<Integer, GDXSQLiteData> simulationData = new HashMap<>();
      GDXSQLiteData yearData = Mockito.mock(GDXSQLiteData.class);
      Mockito.when(yearData.getValues("par_energyFlow_Orga", Arrays.asList(new String[]{"E", "EMarket", "EGrid"}))).thenReturn(new double[]{0, 1, 2, 3, 4, 5, 6 + x});
      Mockito.when(yearData.getValues("par_energyFlow_Orga", Arrays.asList(new String[]{"W", "WGrid", "load_W1"}))).thenReturn(new double[]{0, 1, 2, 3, 4, 5, 6.1 + x});
      Mockito.when(yearData.getValues("par_energyFlow_Cust", Arrays.asList(new String[]{"W", "WGrid", "load_W1"}))).thenReturn(new double[]{0, 1, 2, 3, 4, 5, 6.2 + x});
      Mockito.when(yearData.getValues("par_energyFlow_Cust", Arrays.asList(new String[]{"E", "EMarket", "EGrid"}))).thenReturn(new double[]{0, 1, 2, 3, 4, 5, 6.3 + x});
      simulationData.put(0, yearData);
      return simulationData;
   }

   private List<RequestedParametersYear> initParameters(int numberOfIds, List<List<String>> orga, List<List<String>> cust) {
      final List<RequestedParametersYear> requestedParameters = new ArrayList<>();
      for (int i = 0; i < numberOfIds; i++) {
         final RequestedParametersYear requestedParametersYear = new RequestedParametersYear();
         requestedParametersYear.setYear(0);
         requestedParametersYear.setSimulationid(i + 1);
         final Combination combinationOrga = new Combination();
         combinationOrga.setSet_ii("Viertelstundenwerte");
         combinationOrga.setCombinations(orga);
         final Combination combinationCust = new Combination();
         combinationCust.setSet_ii("Viertelstundenwerte");
         combinationCust.setCombinations(cust);

         Map<String, Combination> params = new LinkedHashMap<>();
         params.put("par_energyFlow_Orga", combinationOrga);
         params.put("par_energyFlow_Cust", combinationCust);
         requestedParametersYear.setParameters(params);
         requestedParameters.add(requestedParametersYear);
      }
      return requestedParameters;
   }

}