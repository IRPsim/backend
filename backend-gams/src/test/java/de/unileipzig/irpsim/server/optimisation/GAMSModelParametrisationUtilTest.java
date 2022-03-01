package de.unileipzig.irpsim.server.optimisation;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.TimeseriesTooShortException;
import de.unileipzig.irpsim.core.simulation.data.json.YearData;
import de.unileipzig.irpsim.core.standingdata.DataLoader;
import de.unileipzig.irpsim.gams.GAMSHandler;
import de.unileipzig.irpsim.gams.GAMSModelParametrisationUtil;

@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.*", "javax.management.*" })
@RunWith(PowerMockRunner.class)
@PrepareForTest({ DataLoader.class })
public class GAMSModelParametrisationUtilTest {

   @Before
   public void setUp() throws Exception {
      MockitoAnnotations.initMocks(this);
   }

   public void mockLoadDataProvider() {
      final Map<Integer, List<Number>> result = new HashMap<>();
      final List<Number> valuesStunden = new LinkedList<>();
      for (int i = 0; i < 8760; i++) {
         valuesStunden.add((double) i);
      }

      result.put(1, valuesStunden);

      final List<Number> jahrValues = new LinkedList<>();
      jahrValues.add((double) 15);
      result.put(2, jahrValues);

      PowerMockito.mockStatic(DataLoader.class);

      PowerMockito.when(DataLoader.getTimeseries(Arrays.asList(new Integer[] { 1, 2 }), true)).thenReturn(result);
   }

   @Test
   public void testRollout() throws TimeseriesTooShortException {
      mockLoadDataProvider();

      final GAMSHandler handler = Mockito.mock(GAMSHandler.class);

      final YearData yd = new YearData();
      yd.getConfig().setSimulationlength(35040);
      yd.getConfig().setSavelength(96);
      final BackendParametersYearData yeardata = new BackendParametersYearData(yd);

      final Timeseries timeseries_stunden = new Timeseries();
      timeseries_stunden.setSeriesname(1);

      final Timeseries ts_jahr = new Timeseries();
      ts_jahr.setSeriesname(2);

      yeardata.getTimeseries().put("par_test", timeseries_stunden);
      yeardata.getTimeseries().put("par_test2", ts_jahr);
      final GAMSModelParametrisationUtil util = new GAMSModelParametrisationUtil(handler, yeardata, 0);
      util.loadParameters();
      util.parameterizeModel();

      final Map<String, Number> expected = new LinkedHashMap<>();
      for (int hour = 0; hour < 8760; hour++) {
         for (int i = 0; i < 4; i++) {
            expected.put("ii" + (1 + hour * 4 + i), (double) hour);
         }
      }

      MatcherAssert.assertThat(expected.entrySet(), Matchers.hasSize(35040));

      Mockito.verify(handler).addSingleDependentParameter("par_test", expected);

      final Map<String, Number> expectedJahr = new LinkedHashMap<>();
      for (int i = 1; i <= 35040; i++) {
         expectedJahr.put("ii" + i, 15.0);
      }

      MatcherAssert.assertThat(expectedJahr.entrySet(), Matchers.hasSize(35040));
      Mockito.verify(handler).addSingleDependentParameter("par_test2", expectedJahr);
   }
}
