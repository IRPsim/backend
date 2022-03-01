package de.unileipzig.irpsim.server.optimisation.postprocessing.summarizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fasst Ergebniswerte zusammen, indem Summen gebildet werden.
 * 
 * @author reichelt
 */
public class SumSummarizer implements Summarizer {

	private double value = 0;

	@Override
	public final void addValue(final double add) {
		value += add;
	}

	@Override
	public final List<Double> getValue() {
		return new ArrayList<Double>(Arrays.asList(new Double[] {value }));
	}

}
