package de.unileipzig.irpsim.server.data.timeseries;

import de.unileipzig.irpsim.core.data.timeseries.Timeseries;

/**
 * Datensammlung für die Berechnung von neuen Zeitreihendaten aus alten. In den Berechnungsprozess geht diese Datensammlung zwifach ein: je einmal für die ursprüngliche Zeitreihe (darin sind die
 * Zeitreihenwerte enthalten) und für die zu berechnende Zeitreihe (leere Zeitreihenwerte).
 */
public class TransformationInput {

	private int year;
	private Timeseries timeseries;

	public TransformationInput(final int year, final Timeseries timeseries) {
		super();
		this.year = year;
		this.timeseries = timeseries;
	}

	public int getYear() {
		return year;
	}

	public void setYear(final int year) {
		this.year = year;
	}

	public Timeseries getTimeseries() {
		return timeseries;
	}

	public void setTimeseries(final Timeseries timeseries) {
		this.timeseries = timeseries;
	}

}
