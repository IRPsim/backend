package de.unileipzig.irpsim.server.optimisation.postprocessing.summarizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fasst Ergebniswerte zusammen, ermittelt das Minimum aller übergebenen Werte.
 *
 * @author krauss
 */
public class MinSummarizer implements Summarizer {

	private double minimum = Double.POSITIVE_INFINITY;

	@Override
	public final void addValue(final double add) {
		if (minimum > add) {
			minimum = add;
		}
	}

	@Override
	public final List<Double> getValue() {
		return new ArrayList<Double>(Arrays.asList(new Double[] {minimum}));
	}
}
