package de.unileipzig.irpsim.core.simulation.data;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.joda.time.DurationFieldType;
import org.joda.time.MutableDateTime;

public enum TimeInterval {

	YEAR("Jahr", "'Wert'", Calendar.YEAR, DurationFieldType.years(), 1, 1, 1), MONTH("Monat", "MMMM", Calendar.MONTH, DurationFieldType.months(), 1, 12, 12), WEEK("Woche", "w", Calendar.DATE,
			DurationFieldType.weeks(), 7, 52, 52), DAY("Tag", "dd.MM", Calendar.DATE, DurationFieldType.days(), 1, 365, 366), HOUR("Stunde", "dd.MM HH:00", Calendar.HOUR_OF_DAY,
					DurationFieldType.hours(), 1, 365 * 24, 366 * 24), QUARTERHOUR("Viertelstunde", "dd.MM HH:mm", Calendar.MINUTE, DurationFieldType.minutes(), 15, 35040, 35136);

	private final String label;
	private int type;
	private DurationFieldType type2;
	private int countInYear;
	private int countInLeafYear;
	private int unitAmount;
	private SimpleDateFormat format;

	TimeInterval(final String label, final String format, final int type, final DurationFieldType type2, final int unitAmount, final int countInYear, final int countInLeafYear) {
		this.label = label;
		this.type = type;
		this.type2 = type2;
		this.unitAmount = unitAmount;
		this.format = new SimpleDateFormat(format);
		this.countInYear = countInYear;
		this.countInLeafYear = countInLeafYear;
	}

	public String getLabel() {
		return this.label;
	}

	public void addTo(final Calendar cal) {
		cal.add(this.type, this.unitAmount);
	}

	public void addTo(final MutableDateTime cal) {
		cal.add(type2, unitAmount);
	}

	public String format(final Calendar cal) {
		return this.format.format(cal.getTime());
	}

	public int getCountInYear() {
		return getCountInYear(false);
	}

	public int getCountInYear(final boolean isLeafYear) {
		return isLeafYear ? countInLeafYear : countInYear;
	}

	public static TimeInterval getInterval(final String name) {
		for (final TimeInterval interval : values()) {
			if (interval.getLabel().equals(name)) {
				return interval;
			}
		}
		return null;
	}

	/**
	 * Gibt das Zeitintervall zurück, das im normalen oder im Schaltjahr die übergebene Anzahl an Werten hat.
	 * 
	 * @param countOfValues
	 * @return
	 */
	public static TimeInterval getInterval(final int countOfValues) {
		TimeInterval concreteInterval = null;
		for (final TimeInterval interval : TimeInterval.values()) {
			if (interval.getCountInYear() == countOfValues || interval.getCountInYear(true) == countOfValues) {
				concreteInterval = interval;
			}
		}
		return concreteInterval;
	}
}
