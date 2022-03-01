package de.unileipzig.irpsim.core.simulation.data.persistence;

/**
 * Listet die verschiedenen Phasen der Simulation auf einschließlich ihrer Beschreibung.
 *
 * Die Reihenfolge ist wichtig und sollte der chronologischen Abfolge im Simulationsprozess entsprechen.
 *
 * Wichtig! Wird die Reihenfolge geändert (oder ein neuer Status nicht am Ende eingefügt) muss die Datenbank entsprechend angepasst werden!
 */
public enum State implements Comparable<State> {
	WAITING("Job wartet in Warteschlange."), LOADING("Lade Parameterzeitreihen aus Datenbank..."), PARAMETERIZING("Initialisiere Parameter..."), RUNNING(
			"Berechnen..."), PERSISTING("Speichern..."), INTERPOLATING("Interpoliere Zwischenjahre..."), POSTPROCESSING("Führe Nachberechnungen durch..."), INTERRUPTED(
					"Durch Server unterbrochen. Ein neuer Ersatzjob sollte gestartet sein."), FINISHED("Optimierung ist beendet."), ERROR("Anfrage liefert Fehler!"), FINISHEDERROR(
							"Beendet, teilweise mit Fehlern!"), ABORTED("Von Nutzer abgebrochen.");

	private final String desc;

	/**
	 * Enumkonstruktor.
	 *
	 * @param desc
	 *            Beschreibung
	 */
	State(final String desc) {
		this.desc = desc;
	}

	public String getDesc() {
		return desc;
	}
}