package de.unileipzig.irpsim.server.optimisation.postprocessing.summarizer;

import java.util.List;

/**
 * Schnittstelle für Klassen, die Ergebnismesswerte zusammenfassen.
 * 
 * @author reichelt
 *
 */
public interface Summarizer {

	/**
	 * Fügt einen Wert zu den Ergebnissen zurück.
	 * 
	 * @param value Wert, der hinzugefügt werden soll
	 */
	void addValue(double value);

	/**
	 * Gibt den aktuellen Zusammenfassungswert aus.
	 * 
	 * @return Zusammenfassungswert
	 */
	List<Double> getValue();
}
