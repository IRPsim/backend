package de.unileipzig.irpsim.core.data.simulationparameters;

/**
 * Wird geworfen, wenn eine Zeitreihe hinzugefügt wird, die bereits existiert.
 * 
 * @author reichelt
 */
public class TimeseriesExistsException extends Exception {

	private static final long serialVersionUID = 4070508526400602342L;
	private final String timeseriesName;

	/**
	 * Konstruktor mit übergebenem Namen der Zeitreihe.
	 * 
	 * @param timeseriesName
	 *            Name der Zeitreihen
	 */
	public TimeseriesExistsException(final String timeseriesName) {
		this.timeseriesName = timeseriesName;
	}

	/**
	 * @return the timeseriesName
	 */
	public final String getTimeseriesName() {
		return timeseriesName;
	}

	@Override
	public final String getMessage() {
		return "Die Zeitreihe " + timeseriesName + " existiert bereits, ein Hinzufügen ist nicht möglich.";
	}
}