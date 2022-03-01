package de.unileipzig.irpsim.server.data.timeseries;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import de.jollyday.Holiday;
import de.jollyday.HolidayCalendar;
import de.jollyday.HolidayManager;
import de.jollyday.ManagerParameter;
import de.jollyday.ManagerParameters;
import de.unileipzig.irpsim.server.data.timeseries.IRPsimHolidays.IRPsimHoliday;

/**
 * Feiertagimplementierung für das IRPsim Projekt.
 */
public class IRPsimHolidays implements Iterable<IRPsimHoliday> {

	/**
	 * Feiertagimplementierung, die alle Feiertage beinhaltet, die im IRPsim Projekt genutzt werden. Im Gegensatz zur jollyday Implimentierung sind Feiertage Jahresübergreifend definiert. <br>
	 * Da variable Feiertage auf jollyday basierend definiert sind können diese auch nur für die Jahre ausgegeben werden, die in den jollyday Daten vorhanden sind.
	 */
	public enum IRPsimHoliday {
		Neujahrstag(1, 1), TAG_DER_ARBEIT(5, 1), Nationalfeiertag(8, 1), Weihnachtstag1(12, 25), Weihnachtstag2(12,
				26), Ostersonntag("christian.EASTER"), Karfreitag(Ostersonntag, -2), Ostermontag(Ostersonntag, 1), CHRISTI_HIMMELFAHRT(Ostersonntag,
				39), Pfingstsonntag(Ostersonntag, 49), Pfingstmontag(Ostersonntag, 50);

		private static final ManagerParameter PARAMS = ManagerParameters.create(HolidayCalendar.SWITZERLAND);
		private static final HolidayManager MANAGER = HolidayManager.getInstance(PARAMS);
		private int day = 0;
		private int month = 0;
		private IRPsimHoliday reference = null;
		private String jollydayRef = null;

		/**
		 * Initialisiert den Feiertag mit gegebenem Monat und Tag.
		 *
		 * @param day
		 *            Tag
		 * @param month
		 *            Monat
		 */
		IRPsimHoliday(final int month, final int day) {
			this.day = day;
			this.month = month;
		}

		/**
		 * @author krauss Initialisiert den Feiertag in Bezug auf einen anderen Feiertag mit Tagesdifferenz.
		 * @param reference
		 *            anderer Feiertag
		 * @param difference
		 *            Tagesdifferenz
		 */
		IRPsimHoliday(final IRPsimHoliday reference, final int difference) {
			this.reference = reference;
			day = difference;
		}

		/**
		 * Initialisiert den Feiertag mit Referenz auf einen {@link de.jollyday.Holiday#getPropertiesKey()}.
		 *
		 * @param jollydayRef
		 *            Referenz
		 */
		IRPsimHoliday(final String jollydayRef) {
			this.jollydayRef = jollydayRef;
			day = 0;
		}

		/**
		 * Gibt das Datum des Feiertages im übergebenen Jahr zurück.
		 *
		 * @param year
		 *            das Jahr
		 * @return das Datum des Feiertages
		 */
		public LocalDate getDate(final int year) {
			if (jollydayRef != null) {
				for (final Holiday holiday : MANAGER.getHolidays(year)) {
					if (holiday.getPropertiesKey().equals(jollydayRef)) {
						return holiday.getDate();
					}
				}
			} else if (month > 0) {
				return LocalDate.of(year, month, day);
			} else if (reference != null) {
				return reference.getDate(year).plusDays(day);
			}
			throw new RuntimeException("Kein Datum vorhanden! Evtl. Jahr außerhalb der in jollyday verfügbaren Werte gewählt?");
		}
	}

	private static final List<IRPsimHoliday> HOLIDAYS = Arrays.asList(IRPsimHoliday.values());

	public static boolean isHoliday(final LocalDate date) {
		for (final IRPsimHoliday holiday : HOLIDAYS) {
			if (date == holiday.getDate(date.getYear())) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Iterator<IRPsimHoliday> iterator() {
		return HOLIDAYS.iterator();
	}
}
