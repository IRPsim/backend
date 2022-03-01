package de.unileipzig.irpsim.core.simulation.data;

import java.util.Arrays;

/**
 * Hält alle möglichen Berechnungen.
 *
 * @author krauss
 */
public enum Calculation {
	MIN, MAX, AVG, SUM, OUTLINE;

	/**
	 * Liefert die {@link Calculation}, die durch den übergebenen Namen beschrieben wird, ignoriert Groß- und
	 * Kleinschreibung.
	 *
	 * @param name Der Name der Berechnung
	 * @return Die Berechnung, die durch den Namen beschrieben wird.
	 */
	public static Calculation fetchCalculation(final String name) {
		return Arrays.stream(Calculation.values()).filter(c -> c.name().equalsIgnoreCase(name)).findAny().orElse(null);
	}
}
