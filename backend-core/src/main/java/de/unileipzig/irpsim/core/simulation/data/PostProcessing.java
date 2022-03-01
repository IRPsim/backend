package de.unileipzig.irpsim.core.simulation.data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Hält alle Daten der Nachberechnung.
 * 
 * @author reichelt
 */
public final class PostProcessing {
	private Map<String, AggregatedResult> scalars = new LinkedHashMap<>();;
	private Map<String, LinkedHashMap<String, Map<String, AggregatedResult>>> sets = new LinkedHashMap<>();
	private Map<String, LinkedHashMap<String, Map<String, AggregatedResult>>> tables = new LinkedHashMap<>();

	public Map<String, AggregatedResult> getScalars() {
		return scalars;
	}

	public void setScalars(final Map<String, AggregatedResult> scalars) {
		this.scalars = scalars;
	}

	public Map<String, LinkedHashMap<String, Map<String, AggregatedResult>>> getSets() {
		return sets;
	}

	public void setSets(final Map<String, LinkedHashMap<String, Map<String, AggregatedResult>>> sets) {
		this.sets = sets;
	}

	public Map<String, LinkedHashMap<String, Map<String, AggregatedResult>>> getTables() {
		return tables;
	}

	public void setTables(final Map<String, LinkedHashMap<String, Map<String, AggregatedResult>>> tables) {
		this.tables = tables;
	}
}
