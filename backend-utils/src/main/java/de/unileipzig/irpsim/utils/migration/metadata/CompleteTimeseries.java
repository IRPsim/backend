package de.unileipzig.irpsim.utils.migration.metadata;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.utils.data.Intervals;

/**
 * Enthält die Daten einer Zeitreihe inklusive allgemeine und spezifische Metadaten.
 *
 * @author reichelt
 */
public class CompleteTimeseries {
	private static final Logger LOG = LogManager.getLogger(CompleteTimeseries.class);

	/**
	 * Wir könnten Schaltjahresdaten bekommen, dann müssen diese zumindest anfänglich gespeichert werden können.
	 */
	private static final int CAPACITY = 35040;
	private String irmKey;
	private final String basename;

	// Fachlich definierte Metadaten
	private String bezeichnung, unit, source, modul, freitext;

	private String scenario = "Default";
	private final Map<String, String> metadata = new LinkedHashMap<>();
	private Integer year = 2015, calendarYear = null;
	private Intervals intervals;
	private Double factor = null;
	private List<Double> data = new ArrayList<>(CAPACITY);
	private int index = 0;
	private boolean testdata = false;

	public String getIrmKey() {
		return irmKey;
	}

	public void setIrmKey(final String irmKey) {
		this.irmKey = irmKey;
	}

	public String getBezeichnung() {
		return bezeichnung;
	}

	public void setBezeichnung(final String bezeichnung) {
		this.bezeichnung = bezeichnung;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(final String unit) {
		this.unit = unit;
	}

	public String getSource() {
		return source;
	}

	public void setSource(final String source) {
		this.source = source;
	}

	public String getModul() {
		return modul;
	}

	public void setModul(final String modul) {
		this.modul = modul;
	}

	public String getFreitext() {
		return freitext;
	}

	public void setFreitext(final String freitext) {
		this.freitext = freitext;
	}

	public String getScenario() {
		return scenario;
	}

	public void setScenario(final String scenario) {
		this.scenario = scenario;
	}

	public Integer getYear() {
		return year;
	}

	public void setYear(final Integer year) {
		this.year = year;
	}

	public Integer getCalendarYear() {
		return calendarYear;
	}

	public void setCalendarYear(final Integer calendarYear) {
		this.calendarYear = calendarYear;
	}

	public Intervals getIntervals() {
		return intervals;
	}

	public void setIntervals(final Intervals intervals) {
		this.intervals = intervals;
	}

	public Double getFactor() {
		return factor;
	}

	public void setFactor(final Double factor) {
		this.factor = factor;
	}

	public List<Double> getData() {
		return data;
	}

	public void setData(final List<Double> data) {
		this.data = data;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(final int index) {
		this.index = index;
	}

	public boolean isTestdata() {
		return testdata;
	}

	public void setTestdata(final boolean testdata) {
		this.testdata = testdata;
	}

	public String getBasename() {
		return basename;
	}

	public Map<String, String> getMetadata() {
		return metadata;
	}

	/**
	 * Erstellt neue CompleteTimerow-Instanz.
	 *
	 * @param irmKey
	 *            Der zu setzende IRM_key
	 */
	public CompleteTimeseries(final String irmKey, final String basename) {
		this.irmKey = irmKey;
		this.basename = basename;
	}

	public CompleteTimeseries(final String irmKey) {
		this.irmKey = irmKey;
		this.basename = irmKey;
	}

	/**
	 * Fügt einen Wert zum double Array hinzu.
	 *
	 * @param val
	 *            der hinzuzufügende Wert
	 */
	public void add(final double val) {
		data.add(val);
	}

	/**
	 * Schreibt Zeitreihendaten in eine Datei.
	 *
	 * @param f
	 *            Die Datei
	 * @throws IOException
	 *             Tritt auf falls Fehler beim Lesen oder Schreiben auftreten
	 */
	public final void createImportableFile(final File f) throws IOException {
		final BufferedWriter bw = new BufferedWriter(new FileWriter(f));

		for (int i = 0; i < getData().size(); i++) {
			bw.write("'" + getIrmKey() + "'," + (i + 1) + "," + getData().get(i));
		}
		bw.flush();
		bw.close();
	}

	/**
	 * Liefert den Namen der Zeitreihe mit dem mitgegebenen Index und dem Anhang "_test" falls es sich um Testdaten handelt.
	 *
	 * @param index
	 *            Der in den Namen zu setzende Index
	 * @return Der zusammengesetzte Name der Zeitreihe
	 */
	public final String getName(final int index) {
		return basename + "_" + index + (testdata ? "_test" : "");
	}

	/**
	 * Liefert das Zeitreihenintervall.
	 *
	 * @return Das Zeitreihenintervall
	 */
	public final String getInterval() {
		if (getIntervals() != null) {
			return getIntervals().name();
		}
		return "Undefiniert";
	}

	/**
	 * Setzt Zeitreihenintervall.
	 *
	 * @param interval
	 *            Das zu setzende Intervall, null oder leerer String werden als Viertelstundenwert behandelt
	 */
	public final void setInterval(final String interval) {
		LOG.trace("Intervall: {} Key: {}", interval, irmKey);
		if (interval == null || interval.equals("Viertelstunde") || interval.equals("")) {
			intervals = Intervals.Viertelstundenwert;
		} else if (interval.equals("Jahr")) {
			intervals = Intervals.Jahreswert;
		} else {
			this.setIntervals(Intervals.getInterval(interval).get());
		}
	}

	/**
	 * Kopiert die Metadaten (alle immutable) in eine neue Instanz und Berechnet die Daten aus den bestehenden, indem jeder Wert mit dem Faktor multipliziert wird. Die Jahreszahl wird dabei um 1
	 * erhöht.
	 *
	 * @return Kopie mit verrechneten Werten für das nächste Jahr
	 */
	public CompleteTimeseries calculateNextYear() {
		final CompleteTimeseries nextYear = new CompleteTimeseries(irmKey, basename);
		nextYear.bezeichnung = bezeichnung;
		nextYear.factor = factor;
		nextYear.intervals = intervals;
		nextYear.testdata = testdata;
		nextYear.metadata.putAll(metadata);
		nextYear.scenario = scenario;
		nextYear.source = source;
		nextYear.freitext = freitext;
		nextYear.modul = modul;
		nextYear.unit = unit;
		nextYear.year = year;
		nextYear.calendarYear = calendarYear + 1;
		data.forEach(value -> nextYear.data.add(value * factor));
		return nextYear;
	}

}