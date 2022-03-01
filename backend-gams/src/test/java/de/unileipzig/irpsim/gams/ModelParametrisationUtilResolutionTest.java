package de.unileipzig.irpsim.gams;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;

@RunWith(Parameterized.class)
public class ModelParametrisationUtilResolutionTest {
   private static final int SIMULATION_LENGTH = 192;

   private final int saveLength = 96;
   private final int optimizationLength = 192;
   private final int resolution;

   private final GAMSHandler gamsHandler;
   private final GAMSModelParametrisationUtil gamsModelParametrisationUtil;

   @Captor
   private ArgumentCaptor<List<String>> argumentCaptor;

   public ModelParametrisationUtilResolutionTest(final int resolution) {
      this.resolution = resolution;
      this.gamsHandler = Mockito.mock(GAMSHandler.class);
      final BackendParametersYearData yeardata = createYeardata();
      gamsModelParametrisationUtil = new GAMSModelParametrisationUtil(gamsHandler, yeardata, 0);
      MockitoAnnotations.initMocks(this);
   }

   @Parameters
   public static Collection<Object[]> data() {
      return Arrays.asList(new Object[][] { { 35040 },  { 8760} });
   }

   @Test
   public void testTimeElementsSize() {
      Assert.assertEquals(SIMULATION_LENGTH, gamsModelParametrisationUtil.getTimeElements().size());
      testSetLength("set_t", optimizationLength);
      System.out.println("Test resolution: " + resolution + " Val: " + (resolution == 35040 ? 0.25 : 1));
      Mockito.verify(gamsHandler).addScalarParameter("sca_delta_ii", resolution == 35040 ? 0.25 : 1);
   }

   private void testSetLength(final String name, final int length) {
      Mockito.verify(gamsHandler).addSetParameter(ArgumentMatchers.eq(name), argumentCaptor.capture());
      Assert.assertEquals(length, argumentCaptor.getValue().size());
   }

   private BackendParametersYearData createYeardata() {
      final BackendParametersYearData yeardata = new BackendParametersYearData();
      yeardata.getConfig().setSavelength(saveLength);
      yeardata.getConfig().setSimulationlength(SIMULATION_LENGTH);
      yeardata.getConfig().setOptimizationlength(optimizationLength);
      yeardata.getConfig().setResolution(resolution);
      yeardata.createTimeseriesSets();
      return yeardata;
   }

}