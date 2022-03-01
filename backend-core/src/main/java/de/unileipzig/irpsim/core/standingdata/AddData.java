package de.unileipzig.irpsim.core.standingdata;

import java.util.Calendar;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * JSON-Wrapperklasse für das Hinzufügen von Daten via REST.
 */
public class AddData {

	private int jahr;
	private int szenario;

	private TimeseriesValue[] values;

	@JsonIgnore
	public boolean isSchaltjahr() {
		final Calendar cal = Calendar.getInstance();
		cal.set(Calendar.YEAR, jahr);
		return cal.getActualMaximum(Calendar.DAY_OF_YEAR) > 365;
	}

	public int getJahr() {
		return jahr;
	}

	public void setJahr(final int jahr) {
		this.jahr = jahr;
	}

	public int getSzenario() {
		return szenario;
	}

	public void setSzenario(final int szenario) {
		this.szenario = szenario;
	}

	public TimeseriesValue[] getValues() {
		return values;
	}

	public void setValues(final TimeseriesValue[] values) {
		this.values = values;
	}

}
