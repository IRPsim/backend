package de.unileipzig.irpsim.server.optimisation.postprocessing;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.unileipzig.irpsim.core.simulation.data.Calculation;

public class PostProcessorTest {

	@Test
	public void testTimeseriesPostProcessor() {
		final TimeseriesPostProcessor timeseriesMin = new TimeseriesPostProcessor("timeseriesTest", Calculation.MIN);
		final TimeseriesPostProcessor timeseriesMax = new TimeseriesPostProcessor("timeseriesTest", Calculation.MAX);
		final TimeseriesPostProcessor timeseriesAvg = new TimeseriesPostProcessor("timeseriesTest", Calculation.AVG);
		final TimeseriesPostProcessor timeseriesSum = new TimeseriesPostProcessor("timeseriesTest", Calculation.SUM);
		final TimeseriesPostProcessor timeseriesOut = new TimeseriesPostProcessor("timeseriesTest", Calculation.OUTLINE);

		final List<TimeseriesPostProcessor> processors = Arrays.asList(timeseriesMin, timeseriesMax, timeseriesAvg, timeseriesSum, timeseriesOut);

		for (int i = 0; i < 960; i++) {
			for (final TimeseriesPostProcessor processor : processors) {
				processor.addValue(new String[0], i * 0.5);
			}
		}
		Assert.assertEquals((Double) 0d, timeseriesMin.getValue().get(0));
		Assert.assertEquals((Double) (959d / 2), timeseriesMax.getValue().get(0));
		Assert.assertEquals((Double) (959d / 4), timeseriesAvg.getValue().get(0));
		Assert.assertEquals((Double) (959d * 960 / 4), timeseriesSum.getValue().get(0));
		final Double d = 95d / 4;
		Assert.assertEquals(Arrays.asList(d, d + 48, d + 96, d + 144, d + 192, d + 240, d + 288, d + 336, d + 384, d + 432), timeseriesOut.getValue());
	}

	@Test
	public void testSetPostProcessor() {

		final SetPostProcessor timeseriesMin = new SetPostProcessor("timeseriesTest", Calculation.MIN);
		final SetPostProcessor timeseriesMax = new SetPostProcessor("timeseriesTest", Calculation.MAX);
		final SetPostProcessor timeseriesAvg = new SetPostProcessor("timeseriesTest", Calculation.AVG);
		final SetPostProcessor timeseriesSum = new SetPostProcessor("timeseriesTest", Calculation.SUM);
		final SetPostProcessor timeseriesOut = new SetPostProcessor("timeseriesTest", Calculation.OUTLINE);

		final List<SetPostProcessor> processors = Arrays.asList(timeseriesMin, timeseriesMax, timeseriesAvg, timeseriesSum, timeseriesOut);

		for (int i = 0; i < 960; i++) {
			for (final SetPostProcessor processor : processors) {
				processor.addValue(new String[] {"TestSet"}, i * 0.5);
			}
		}
	}
}
