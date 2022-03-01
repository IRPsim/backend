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
public class ModelParametrisationUtilTest
{
    private static final int SIMULATION_LENGTH = 672;
    private final int saveLength;
    private final int optimizationLength;

    private final GAMSHandler gamsHandler;
    private final GAMSModelParametrisationUtil gamsModelParametrisationUtil;

    @Captor
    private ArgumentCaptor<List<String>> argumentCaptor;

    public ModelParametrisationUtilTest(final int savelength, final int optimizationLength) {
        this.saveLength = savelength;
        this.optimizationLength = optimizationLength;
        this.gamsHandler = Mockito.mock(GAMSHandler.class);
        final BackendParametersYearData yeardata = createYeardata();
        gamsModelParametrisationUtil = new GAMSModelParametrisationUtil(gamsHandler, yeardata, 0);
        MockitoAnnotations.initMocks(this);
    }

    @Parameters
    public static Collection<Object[]> data()
    {
        return Arrays.asList(new Object[][] { { 24, 85 }, { 48, 97 }, { 96, 256 }, { 12, 120 }, { 50, 97 }, { 97, 333 },
                { 11, 79 }, { 23, 128 }, { 341, 672 } });
    }

    @Test
    public void testTimeElementsSize()
    {
        Assert.assertEquals(SIMULATION_LENGTH, gamsModelParametrisationUtil.getTimeElements().size());
        testSetLength("set_t", optimizationLength);
        testSetLength("set_optstore", saveLength);
        testSetLength("set_optsteps", getNumberOfOptsteps());
    }

    private void testSetLength( final String name, final int length )
    {
        Mockito.verify(gamsHandler).addSetParameter(ArgumentMatchers.eq(name), argumentCaptor.capture());
        Assert.assertEquals(length, argumentCaptor.getValue().size());
    }

    private BackendParametersYearData createYeardata()
    {
        final BackendParametersYearData yeardata = new BackendParametersYearData();
        yeardata.getConfig().setSavelength(saveLength);
        yeardata.getConfig().setSimulationlength(SIMULATION_LENGTH);
        yeardata.getConfig().setOptimizationlength(optimizationLength);
        yeardata.createTimeseriesSets();
        return yeardata;
    }

    private int getNumberOfOptsteps()
    {
        return (int) Math.ceil(SIMULATION_LENGTH / (double) saveLength);
    }
}