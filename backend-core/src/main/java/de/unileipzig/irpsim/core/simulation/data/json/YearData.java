package de.unileipzig.irpsim.core.simulation.data.json;

import java.util.LinkedHashMap;
import java.util.Map;

import de.unileipzig.irpsim.core.simulation.data.Globalconfig;
import de.unileipzig.irpsim.core.simulation.data.PostProcessing;

/**
 * Kapselt die Jahresdaten.
 *
 * @author reichelt
 */
public class YearData {

	private Globalconfig config = new Globalconfig();
	private Map<String, Object> scalars = new LinkedHashMap<>();
	private Map<String, LinkedHashMap<String, LinkedHashMap<String, Object>>> sets = new LinkedHashMap<>();
	private Map<String, Object> timeseries = new LinkedHashMap<>();
	private Map<String, Map<String, Map<String, Object>>> tables = new LinkedHashMap<>();
	private PostProcessing postprocessing = new PostProcessing();

	/**
	 * Erzeugt neue YearData-Instanz, diese wird leer initialisiert.
	 */
	public YearData() {
	}

	/**
	 * Erzeugt neue YearData Instanz mit gegebenem Jahr.
	 *
	 * @param year das übergebene Jahr
	 */
	public YearData(final int year) {
		this();
		config.setYear(year);
	}

	/**
	 * Liefert scalars der Jahresdaten.
	 *
	 * @return Die Scalare der Jahresdaten als Map<String, Number>
	 */
	public final Map<String, Object> getScalars() {
		return scalars;
	}

	/**
	 * Setzt scalars der Jahresdaten.
	 *
	 * @param scalars Die scalars der Jahresdaten
	 */
	public final void setScalars(final Map<String, Object> scalars) {
		this.scalars = scalars;
	}

	/**
	 * Liefert sets der Jahresdaten.
	 *
	 * @return Die sets der Jahresdaten als Map<String, LinkedHashMap<String, Map<String, Object>>>
	 */
	public final Map<String, LinkedHashMap<String, LinkedHashMap<String, Object>>> getSets() {
		return sets;
	}

	/**
	 * Setzt sets der Jahresdaten.
	 *
	 * @param sets Die sets der Jahresdaten
	 */
	public final void setSets(final Map<String, LinkedHashMap<String, LinkedHashMap<String, Object>>> sets) {
		this.sets = sets;
	}

	/**
	 * Liefert timeseries der Jahresdaten.
	 *
	 * @return Die Zeitreihen der Jahresdaten als Map<String, Object>
	 */
	public final Map<String, Object> getTimeseries() {
		return timeseries;
	}

	/**
	 * Setzt die Zeitreihen der Jahresdaten.
	 *
	 * @param timeseries Die zu setzende Zeitreihe der Jahresdaten
	 */
	public final void setTimeseries(final Map<String, Object> timeseries) {
		this.timeseries = timeseries;
	}

	/**
	 * Liefert die tables der Jahresdaten.
	 *
	 * @return Die tables der Jahresdaten als Map<String, Map<String, Object>>
	 */
	public final Map<String, Map<String, Map<String, Object>>> getTables() {
		return tables;
	}

	/**
	 * Setzt tables der Jahresdaten.
	 *
	 * @param tables Die zu setzenden tables
	 */
	public final void setTables(final Map<String, Map<String, Map<String, Object>>> tables) {
		this.tables = tables;
	}

	public final Globalconfig getConfig() {
		return config;
	}

	public final void setConfig(final Globalconfig config) {
		this.config = config;
	}

	public final PostProcessing getPostprocessing() {
		return postprocessing;
	}

	public final void setPostprocessing(final PostProcessing postprocessing) {
		this.postprocessing = postprocessing;
	}
}