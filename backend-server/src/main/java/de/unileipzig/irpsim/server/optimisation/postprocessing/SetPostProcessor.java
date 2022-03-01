package de.unileipzig.irpsim.server.optimisation.postprocessing;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.simulation.data.Calculation;
import de.unileipzig.irpsim.server.optimisation.postprocessing.summarizer.Summarizer;

// TODO: Scheint irgendwie sinnlos zu sein. Der PostProcessor ist durch den Parameternamen identifiziert und der gehört
// immer zu genau einem Set.
// todo Den Namen des Sets kann man sich von den Outputdependencies holen. Deshalb kann man auch einfach den
// SimplePostprocessor holen!
// todo Das selbe könnte man wahrscheinlich auch für die Tabellen machen.

/**
 * Repräsentiert eine Nachverarbeitung auf Mengen.
 *
 * @author reichelt
 */
public class SetPostProcessor extends PostProcessor {

	private static final Logger LOG = LogManager.getLogger(SetPostProcessor.class);

	private final Map<String, Summarizer> values;

	/**
	 * @param name Der Name des Postprozessors, normalerweise == Parameternamen
	 * @param calculation Die Berechnungsmethode dieses Postprozessors
	 */
	public SetPostProcessor(final String name, final Calculation calculation) {
		super(name, calculation);

		values = new HashMap<String, Summarizer>();
	}

	/**
	 * Fügt zur aktuellen Nachberechnung den übergebenen Wert für das übergebene Set zurück.
	 *
	 * @param setElementName Name des Setelements, für das der Wert hinzugefügt werden soll
	 * @param value Wert zum Hinzufügen
	 */
	@Override
	public final void addValue(final String[] setElementName, final double value) {

		if (setElementName.length != 1) {
			LOG.error("Falsche Zahl an dependents übergeben!");
		}

		Summarizer val = values.get(setElementName[0]);

		if (val == null) {
			val = getNewSummarizer();
			values.put(setElementName[0], val);
		}

		val.addValue(value);
	}

	/**
	 * Gibt die Werte der Nachberechnungen zurück.
	 *
	 * @return Abbildung der Set-Namen auf die aktuellen Werte
	 */
	public final Map<String, List<Double>> getValues() {
		final Map<String, List<Double>> result = new HashMap<>();
		values.forEach((name, measuerer) -> {
			result.put(name, measuerer.getValue());
		});
		return result;
	}

	@Override
	public final int getSize() {
		final SortedSet<Integer> biggestSize = new TreeSet<>();
		values.values().stream().forEach(outliner -> {
			biggestSize.add(outliner.getValue().size());
		});
		return biggestSize.isEmpty() ? 0 : biggestSize.last();
	}
}
