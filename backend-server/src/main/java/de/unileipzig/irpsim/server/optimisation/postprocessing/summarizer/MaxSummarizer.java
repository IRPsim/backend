package de.unileipzig.irpsim.server.optimisation.postprocessing.summarizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fasst Ergebniswerte zusammen, ermittelt das Maximum aller übergebenen Werte.
 *
 * @author krauss
 */
public class MaxSummarizer implements Summarizer {

	private double maximum = Double.NEGATIVE_INFINITY;

	@Override
	public final void addValue(final double add) {
		if (maximum < add) {
			maximum = add;
		}
	}

	@Override
	public final List<Double> getValue() {
		return new ArrayList<Double>(Arrays.asList(new Double[] {maximum}));
	}
}
