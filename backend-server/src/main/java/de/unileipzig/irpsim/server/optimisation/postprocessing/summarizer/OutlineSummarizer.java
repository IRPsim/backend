package de.unileipzig.irpsim.server.optimisation.postprocessing.summarizer;

import java.util.ArrayList;
import java.util.List;

/**
 * Fasst Ergebniswerte zusammen, ermittelt für jeden Tag einen Durchschnittswert.
 * 
 * @author krauss
 */
public class OutlineSummarizer implements Summarizer {

	private List<Double> outline = new ArrayList<Double>();
	private double currentDay = 0;
	private int count = 0;
	private static final int DAYLENGTH = 96; // TODO: Den Wert von geeigneter Klasse bestimmen lassen.

	@Override
	public final void addValue(final double value) {
		currentDay += value;
		count++;
		if (count >= DAYLENGTH) {
			outline.add(currentDay / DAYLENGTH);
			currentDay = 0;
			count = 0;
		}
	}

	@Override
	public final List<Double> getValue() {
		return outline;
	}

	@Override
	public final String toString() {
		return outline.toString();
	}
}
