package de.unileipzig.irpsim.server.optimisation.postprocessing;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.simulation.data.Calculation;
import de.unileipzig.irpsim.server.optimisation.postprocessing.summarizer.Summarizer;

/**
 * Der Postprozessor für Zeitreihen, die von zwei Sets abhängen.
 * 
 * @author reichelt
 */
public class TablePostProcessor extends PostProcessor {

	private static final Logger LOG = LogManager.getLogger(TablePostProcessor.class);

	private final Map<String, Map<String, Summarizer>> values;

	/**
	 * @param name
	 *            Parametername
	 * @param calculation
	 *            Die Berechnung
	 */
	public TablePostProcessor(final String name, final Calculation calculation) {
		super(name, calculation);

		values = new HashMap<>();
	}

	@Override
	public final void addValue(final String[] dependents, final double value) {
		if (dependents.length != 2) {
			LOG.error("Falsche Zahl an Abhängigen!");
		}

		Map<String, Summarizer> result = values.get(dependents[0]);
		synchronized (values) {
			if (result == null) {
				result = new HashMap<>();
				values.put(dependents[0], result);
			}
		}

		LOG.trace("Name: " + getName() + " " + Arrays.toString(dependents));
		Summarizer summarizer = result.get(dependents[1]);
		if (summarizer == null) {
			summarizer = getNewSummarizer();
			result.put(dependents[1], summarizer);
		}
		summarizer.addValue(value);
	}

	@Override
	public final int getSize() {
		final SortedSet<Integer> biggestSize = new TreeSet<>();
		synchronized (values) {
			values.values().stream().forEach(map -> {
				map.values().stream().forEach(outliner -> {
					biggestSize.add(outliner.getValue().size());
				});
			});
		}
		return biggestSize.isEmpty() ? 0 : biggestSize.last();
	}

	public final Map<String, Map<String, List<Double>>> getValues() {
		final Map<String, Map<String, List<Double>>> result = new HashMap<>();
		values.forEach((key1, pair) -> {
			final HashMap<String, List<Double>> key1Map = new HashMap<>();
			result.put(key1, key1Map);
			pair.forEach((key2, value) -> {
				key1Map.put(key2, value.getValue());
			});
		});

		return result;
	}
}
