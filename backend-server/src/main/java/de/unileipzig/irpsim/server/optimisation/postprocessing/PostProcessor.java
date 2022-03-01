package de.unileipzig.irpsim.server.optimisation.postprocessing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.simulation.data.Calculation;
import de.unileipzig.irpsim.server.optimisation.postprocessing.summarizer.AvgSummarizer;
import de.unileipzig.irpsim.server.optimisation.postprocessing.summarizer.MaxSummarizer;
import de.unileipzig.irpsim.server.optimisation.postprocessing.summarizer.MinSummarizer;
import de.unileipzig.irpsim.server.optimisation.postprocessing.summarizer.OutlineSummarizer;
import de.unileipzig.irpsim.server.optimisation.postprocessing.summarizer.SumSummarizer;
import de.unileipzig.irpsim.server.optimisation.postprocessing.summarizer.Summarizer;

/**
 * Repräsentiert eine Nachverarbeitung mit ihrem Namen und ihrer Berechnungsvorschrift. Konkrete Nachberechnungen sollen
 * von dieser Klasse erben.
 *
 * @author reichelt
 */
public abstract class PostProcessor {

	private static final Logger LOG = LogManager.getLogger(PostProcessor.class);

	private final String name;
	private final Calculation calculation;

	/**
	 * Initialisiert den PostProcessor mit dem übergebenen Namen und der übergebenen Nachberechnung.
	 *
	 * @param name Name des Postprocessors
	 * @param calculation Berechnungsvorschrift des PostProcessors.
	 */
	public PostProcessor(final String name, final Calculation calculation) {
		this.name = name;
		this.calculation = calculation;
	}

	public final String getName() {
		return name;
	}

	public final Calculation getCalculation() {
		return calculation;
	}

	/**
	 * @param dependents Die abhängigen Sets bzw. Elemente
	 * @param value Der hinzuzufügende Wert
	 */
	public abstract void addValue(String[] dependents, double value);

	public final Summarizer getNewSummarizer() {
		Summarizer val;
		switch (calculation) {
		case SUM:
			val = new SumSummarizer();
			break;
		case MAX:
			val = new MaxSummarizer();
			break;
		case MIN:
			val = new MinSummarizer();
			break;
		case AVG:
			val = new AvgSummarizer();
			break;
		case OUTLINE:
			val = new OutlineSummarizer();
			break;
		default:
			throw new RuntimeException(name + " derzeitig nicht implementiert");
		}
		return val;
	}

	/**
	 * @return Die Menge der Eintagesmittelwerte, 0 wenn der {@link PostProcessor} nicht die Übersichtszeitreihe
	 *         berechnet
	 */
	public final int fetchOutlineSize() {
		if (!calculation.equals(Calculation.OUTLINE)) {
			LOG.error("Prozessor ist kein Übersichtsprozessor!");
			return 0;
		}
		return getSize();
	}

	protected abstract int getSize();
}
