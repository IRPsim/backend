package de.unileipzig.irpsim.utils.data;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * Beinhaltet Arten von Zeitintervallwerten, die in Excel-Zeitreihendateien zulässig sind.
 *
 * @author reichelt
 */
public enum Intervals {
	// TODO Einheitlche Repräsentation: TimeInterval benutzen
	Skalar(0), Jahreswert(0), Stundenwert(4), Viertelstundenwert(1), Tageswert(96), Halbtageswert(48), Monatswert(0);

	private static final Logger LOG = LogManager.getLogger(Intervals.class);

	private final int numberOfQuarters;

	/**
	 * Privater Konstruktor.
	 *
	 * @param numberOfQuarters
	 *            Die Anzahl Viertelstundenwerte pro gegebenem Wert. Jahr- und Monatswerte müssen gesondert behandelt werden.
	 */
	Intervals(final int numberOfQuarters) {
		this.numberOfQuarters = numberOfQuarters;
	}

	/**
	 * Liefert Zeitintervalle.
	 *
	 * @param name
	 *            Der Name des Zeitintervalls
	 * @return Das Intervall, falls ein Intervall mit dem Name definiert ist.
	 */
	public static final Optional<Intervals> getInterval(final String name) {
		final Stream<Intervals> stream = Arrays.stream(Intervals.values());
		LOG.trace("Suche " + name + " in " + stream);
		return stream.filter(val -> name.equalsIgnoreCase(val.name())).findAny();
	}

	public int getNumberOfQuarters() {
		return numberOfQuarters;
	}
}