package de.unileipzig.irpsim.core.simulation.data;

import java.util.LinkedHashMap;
import java.util.Map;

import de.unileipzig.irpsim.core.data.timeseries.Timeseries;

/**
 * Beinhaltet Daten eines SetElements.
 *
 * @author hildebrandt
 */
public final class SetElement {
	private String name;
	private Map<String, Object> attributes = new LinkedHashMap<>();
	private Map<String, Timeseries> timeseries = new LinkedHashMap<>();

	/**
	 * Erstellt neues SetElement initialisiert den Namen den SetElements.
	 *
	 * @param name
	 *            Der Name des SetElements
	 */
	public SetElement(final String name) {
		this.name = name;
	}

	/**
	 * Leerer Konstruktor.
	 */
	public SetElement() {
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(final Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	public Map<String, Timeseries> getTimeseries() {
		return timeseries;
	}

	public void setTimeseries(final Map<String, Timeseries> timeseries) {
		this.timeseries = timeseries;
	}

	/**
	 * Überladung der von Object geerbten equals()-Methode da SetElement eine "Value-Klasse ist".
	 *
	 * @param o
	 *            Das mit dem SetElement zu vergleichende Objekt
	 * @return Liefert false fall o null ist, falls der Name des SetElements nicht mit dem Namen von o übereinstimmt oder o nicht zuweisungskompatibel ist liefert true falls o und das SetElement
	 *         gleich sind oder der Name von o mit dem Namen des SetElements übereinstimmt
	 */
	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null) {
			return false;
		}
		if (o instanceof SetElement) {
			return ((SetElement) o).name.equals(name);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	/**
	 * Liefert den Namen des SetElements.
	 *
	 * @return Der Name des SetElements als String
	 */
	@Override
	public String toString() {
		final StringBuilder stringer = new StringBuilder(name).append("{");
		stringer.append("attributes: ").append(attributes.size());
		stringer.append(", timeseries: ").append(timeseries.size());
		return stringer.append("}").toString();
	}
}