package de.unileipzig.irpsim.server.data.timeseries;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.unileipzig.irpsim.core.data.timeseries.Timeseries;

/**
 * Testet die Methoden zur Anpassung von Zeitreihen.
 */
public class TimeseriesTransformerTest {

	private static TransformationInput source;
	private static TransformationInput target;

	static {
		final List<Number> testdata = new ArrayList<>();
		for (int i = 1; i <= 365; i++) {
			for (int j = 0; j < 96; j++) {
				testdata.add(i);
			}
		}
		source = new TransformationInput(2013, Timeseries.build(testdata));
		target = new TransformationInput(2015, null);
	}

	/**
	 * Testet dass alle Daten gleich sind UND dass Änderungen am Ergebnis die Daten von der Eingabe nicht beeinflussen.
	 */
	@Test
	public void testCopy() {
		TimeseriesTransformer.copy(source, target);
		Assert.assertEquals(source.getTimeseries().getData(), target.getTimeseries().getData());
		target.getTimeseries().getData().add(17);
		Assert.assertEquals(35040, source.getTimeseries().size());
	}

	/**
	 * Testet die Korrektheit der dayDiff Funktion.
	 */
	@Test
	public void testDayDiff() {
		Assert.assertEquals(-1, TimeseriesTransformer.dayDiff(2013, 2014));
		Assert.assertEquals(-2, TimeseriesTransformer.dayDiff(2013, 2015));
		Assert.assertEquals(-3, TimeseriesTransformer.dayDiff(2013, 2016));
		Assert.assertEquals(2, TimeseriesTransformer.dayDiff(2013, 2017));
		Assert.assertEquals(1, TimeseriesTransformer.dayDiff(2013, 2018));
		Assert.assertEquals(0, TimeseriesTransformer.dayDiff(2013, 2019));

		Assert.assertEquals(-2, TimeseriesTransformer.dayDiff(2020, 2021));
		Assert.assertEquals(-3, TimeseriesTransformer.dayDiff(2019, 2021));
		Assert.assertEquals(3, TimeseriesTransformer.dayDiff(2018, 2021));
		Assert.assertEquals(2, TimeseriesTransformer.dayDiff(2017, 2021));
		Assert.assertEquals(0, TimeseriesTransformer.dayDiff(2016, 2021));
	}

	/**
	 * Testet die Korrektheit der shiftWeekdays Funktion.
	 *
	 * @throws Exception
	 */
	@Test
	public void testWeekShift() throws Exception {
		TimeseriesTransformer.shiftWeekdays(source, target);
		final List<Number> sData = source.getTimeseries().getData();
		Assert.assertEquals(sData.subList(192, 35040), target.getTimeseries().getData().subList(0, 35040 - 192));

		target.setYear(2018);
		TimeseriesTransformer.shiftWeekdays(source, target);
		Assert.assertEquals(sData.subList(0, 35040 - 96), target.getTimeseries().getData().subList(96, 35040));
	}

	/**
	 * Testet die Korrektheit der adaptCalendar Funktion.
	 */
	@Test
	public void testAdaptCalendar_HolyDays_NoLeap() throws Exception {
		TimeseriesTransformer.adaptCalendar(source, target);
		final int[] holyDays = new int[] { LocalDate.of(2013, 1, 1).getDayOfYear(),
				LocalDate.of(2013, 5, 1).getDayOfYear(),
				LocalDate.of(2013, 8, 1).getDayOfYear(),
				LocalDate.of(2013, 12, 25).getDayOfYear() };
		for (final int day : holyDays) {
			final List<Number> sourceDay = source.getTimeseries().getData().subList((day - 1) * 96, day * 96);
			final List<Number> targetDay = target.getTimeseries().getData().subList((day - 1) * 96, day * 96);
			Assert.assertEquals(sourceDay, targetDay);
		}
	}

	/**
	 * Testet die Korrektheit der adaptCalendar Funktion.
	 */
	@Test
	public void testAdaptCalendar_HolyDays_Leap() throws Exception {
		target.setYear(2016);
		TimeseriesTransformer.adaptCalendar(source, target);
		final int[] holyDays = new int[] { LocalDate.of(2013, 1, 1).getDayOfYear(),
				LocalDate.of(2013, 5, 1).getDayOfYear(),
				LocalDate.of(2013, 8, 1).getDayOfYear(),
				LocalDate.of(2013, 12, 25).getDayOfYear() };
		final int[] holyLeapDays = new int[] { LocalDate.of(2016, 1, 1).getDayOfYear(),
				LocalDate.of(2016, 5, 1).getDayOfYear(),
				LocalDate.of(2016, 8, 1).getDayOfYear(),
				LocalDate.of(2016, 12, 25).getDayOfYear() };
		for (int j = 0; j < holyDays.length; j++) {
			final int day2013 = holyDays[j];
			final int day2016 = holyLeapDays[j];
			final List<Number> sourceDay = source.getTimeseries().getData().subList((day2013 - 1) * 96, day2013 * 96);
			final List<Number> targetDay = target.getTimeseries().getData().subList((day2016 - 1) * 96, day2016 * 96);
			Assert.assertEquals(sourceDay, targetDay);
		}
	}

	/**
	 * Testet die Korrektheit der adaptCalendar Funktion.
	 */
	@Test
	public void testAdaptCalendar_HolyDays_Easter() throws Exception {
		target.setYear(2015);
		TimeseriesTransformer.adaptCalendar(source, target);
		final int[] holyDays2013 = new int[] { LocalDate.of(2013, 3, 29).getDayOfYear(), // Karfreitag
				LocalDate.of(2013, 5, 9).getDayOfYear(), // Christi Himmelfahrt
				LocalDate.of(2013, 5, 20).getDayOfYear() }; // PfingstMontag
		final int[] holyDays2015 = new int[] { LocalDate.of(2015, 4, 3).getDayOfYear(),
				LocalDate.of(2015, 5, 14).getDayOfYear(),
				LocalDate.of(2015, 5, 25).getDayOfYear() };
		for (int j = 0; j < holyDays2013.length; j++) {
			final int day2013 = holyDays2013[j];
			final int day2016 = holyDays2015[j];
			final List<Number> sourceDay = source.getTimeseries().getData().subList((day2013 - 1) * 96, day2013 * 96);
			final List<Number> targetDay = target.getTimeseries().getData().subList((day2016 - 1) * 96, day2016 * 96);
			Assert.assertEquals(sourceDay, targetDay);
		}
	}
}
