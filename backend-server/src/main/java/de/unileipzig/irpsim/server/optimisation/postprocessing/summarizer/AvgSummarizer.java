package de.unileipzig.irpsim.server.optimisation.postprocessing.summarizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fasst Ergebniswerte zusammen, ermittelt den arithmetischen Mittelwert aller übergebenen Werte.
 *
 * @author krauss
 */
public class AvgSummarizer implements Summarizer {

	private double avgvalue = 0;
	private int count = 0;

	@Override
	public final void addValue(final double add) {
		avgvalue = (avgvalue * count + add) / (count + 1);
		count++;
	}

	@Override
	public final List<Double> getValue() {
		return new ArrayList<Double>(Arrays.asList(new Double[] {avgvalue}));
	}

}
