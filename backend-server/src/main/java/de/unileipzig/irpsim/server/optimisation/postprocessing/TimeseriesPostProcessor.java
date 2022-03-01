package de.unileipzig.irpsim.server.optimisation.postprocessing;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.simulation.data.Calculation;
import de.unileipzig.irpsim.server.optimisation.postprocessing.summarizer.Summarizer;

/**
 * PostProcessor für die Nachverarbeitung einer einzelnen Zeitreihe.
 * 
 * @author reichelt
 */
public class TimeseriesPostProcessor extends PostProcessor {

	private static final Logger LOG = LogManager.getLogger(TimeseriesPostProcessor.class);

	private final Summarizer measurer;

	/**
	 * Initialisiert den PostProcessor mit dem übergebenen Namen und der übergebenen Nachberechnung.
	 * 
	 * @param name
	 *            Name des Postprocessors
	 * @param calculation
	 *            Berechnungsvorschrift des PostProcessors.
	 */
	public TimeseriesPostProcessor(final String name, final Calculation calculation) {
		super(name, calculation);

		measurer = getNewSummarizer();
	}

	/**
	 * Fügt den übergebenen aktuellen Wert zur Nachberechnung hinzu.
	 * 
	 * @param value
	 *            Aktueller Wert der Zeitreihe
	 */
	@Override
	public final void addValue(final String[] emptyDependents, final double value) {

		if (emptyDependents.length > 0) {
			LOG.error("Falsche Zahl an Abhängigen!");
		}

		measurer.addValue(value);
	}

	@Override
	public final int getSize() {
		return measurer.getValue().size();
	}

	/**
	 * Gibt den aktuellen Nachverarbeitungswert aus.
	 * 
	 * @return Aktueller Nachverarbeitungswert
	 */
	public final List<Double> getValue() {
		return measurer.getValue();
	}

}
