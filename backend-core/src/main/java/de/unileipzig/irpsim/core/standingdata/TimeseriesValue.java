package de.unileipzig.irpsim.core.standingdata;

/**
 * Repräsentiert ein Wert einer Zeitreihe, d.h. eine Abbildung von Zeitstempel auf einen Wert, im JSON-Wrapper.
 */
public class TimeseriesValue {
	public TimeseriesValue() {

	}

	public TimeseriesValue(final long unixtimestamp, final double value) {
		this.unixtimestamp = unixtimestamp;
		this.value = value;
	}

	private long unixtimestamp;
	private double value;

	public long getUnixtimestamp() {
		return unixtimestamp;
	}

	public void setUnixtimestamp(final long unixtimestamp) {
		this.unixtimestamp = unixtimestamp;
	}

	public double getValue() {
		return value;
	}

	public void setValue(final double value) {
		this.value = value;
	}
}