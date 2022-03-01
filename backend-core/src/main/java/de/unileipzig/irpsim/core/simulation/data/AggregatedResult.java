package de.unileipzig.irpsim.core.simulation.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.unileipzig.irpsim.core.Constants;

/**
 * Verwaltet ein aggregiertes Ergebnis, d.h. alle statistischen Kennwerte, die über eine Zeitreihe gebildet werden. Zurzeit sind Minimum, Maximum, Durchschnitt
 * und Summe möglich.
 * 
 * @author reichelt
 */
@JsonInclude(Include.NON_NULL)
public class AggregatedResult {

	private static final Logger LOG = LogManager.getLogger(AggregatedResult.class);
	private Double min, max, avg, sum;

	/**
	 * Leerer Konstruktor, initialisiert alle Werte als leere Objekte.
	 */
	public AggregatedResult() {
		min = null;
		max = null;
		avg = null;
		sum = null;
	}

	public final Double getMin() {
		return min;
	}

	public final void setMin(final Double min) {
		this.min = min;
	}

	public final Double getMax() {
		return max;
	}

	public final void setMax(final Double max) {
		this.max = max;
	}

	public final Double getAvg() {
		return avg;
	}

	public final void setAvg(final Double avg) {
		this.avg = avg;
	}

	public final Double getSum() {
		return sum;
	}

	public final void setSum(final Double sum) {
		this.sum = sum;
	}

	@Override
	public final boolean equals(final Object other) {
		if (other == null || !(other instanceof AggregatedResult)) {
			return false;
		} else {
			final AggregatedResult otherResult = (AggregatedResult) other;
			boolean equals = true;
			if (sum == null != (otherResult.sum == null)) {
				equals = false;
			}
			if (avg == null != (otherResult.avg == null)) {
				equals = false;
			}
			if (min == null != (otherResult.min == null)) {
				equals = false;
			}
			if (max == null != (otherResult.max == null)) {
				equals = false;
			}
			if (equals && sum != null) {
				equals = Math.abs(otherResult.sum - sum) < Constants.DOUBLE_DELTA;
			}
			if (equals && avg != null) {
				equals = Math.abs(otherResult.avg - avg) < Constants.DOUBLE_DELTA;
			}
			if (equals && max != null) {
				equals = Math.abs(otherResult.max - max) < Constants.DOUBLE_DELTA;
			}
			if (equals && min != null) {
				equals = Math.abs(otherResult.min - min) < Constants.DOUBLE_DELTA;
			}
			return equals;
		}
	}

	@Override
	public final int hashCode() {
		int hash = 71;
		hash = min == null ? 1 : hash * min.intValue() + 11;
		hash = avg == null ? 1 : hash * avg.intValue() + 13;
		hash = max == null ? 1 : hash * max.intValue() + 17;
		hash = sum == null ? 1 : hash * sum.intValue() + 19;
		return hash;
	}

	@Override
	public final String toString() {
		return "Sum: " + sum + " Avg: " + avg + " Min: " + min + " Max: " + max;
	}

	/**
	 * Gibt den Wert zurück, der in diesen Ergebnissen für die entsprechende Berechnungsmethode gespeichert ist.
	 *
	 * @param calculation Die Berechnungsmethode
	 * @return Der Wert den die Methode hat
	 */
	public final double fetchValue(final Calculation calculation) {
		switch (calculation) {
		case MIN:
			return getMin();
		case MAX:
			return getMax();
		case SUM:
			return getSum();
		case AVG:
			return getAvg();
		default:
			LOG.error("Die Berechnung {} ist in den AggregatedResults nicht vorhanden!", calculation);
			return 0;
		}
	}

	/**
	 * Speichert den übergebenen Wert in diesen Ergebnissen für die spezifizierte Berechnungsmethode.
	 *
	 * @param calculation Die Berechnungsmethode
	 * @param value Der zu speichernde Wert
	 */
	public final void placeValue(final Calculation calculation, final double value) {
		switch (calculation) {
		case MIN:
			setMin(value);
			return;
		case MAX:
			setMax(value);
			return;
		case SUM:
			setSum(value);
			return;
		case AVG:
			setAvg(value);
			return;
		default:
			LOG.error("Die Berechnung {} ist in den AggregatedResults nicht vorhanden!", calculation);
			return;
		}
	}
}
