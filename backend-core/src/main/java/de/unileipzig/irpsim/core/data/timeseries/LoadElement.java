package de.unileipzig.irpsim.core.data.timeseries;

/**
 * Beinhaltet Daten für einen Zeitraum und seine Lastdaten.
 *
 * @author reichelt
 */
public class LoadElement {
	private double min, avg, max;
	private long time, count;

	/**
	 * Erzeugt neue LoadElement-Instanz.
	 *
	 * @param time
	 *            Setzt den Zeitraum
	 * @param min
	 *            Setzt minimale Last für den Zeitraum
	 * @param avg
	 *            Setzt die durchschnittliche Last für den Zeitraum
	 * @param max
	 *            Setzt maximale Last für den Zeitraum
	 * @param count
	 *            Anzahl der Rückgabewerte
	 */
	public LoadElement(final long time, final double min, final double avg, final double max, final long count) {
		this.time = time;
		this.min = min;
		this.avg = avg;
		this.max = max;
		this.count = count;
	}

	public LoadElement() {

	}

	public final long getTime() {
		return time;
	}

	public final void setTime(final long time) {
		this.time = time;
	}

	public final double getMin() {
		return min;
	}

	public final void setMin(final double min) {
		this.min = min;
	}

	public final double getAvg() {
		return avg;
	}

	public final void setAvg(final double avg) {
		this.avg = avg;
	}

	public final double getMax() {
		return max;
	}

	public final void setMax(final double max) {
		this.max = max;
	}

	public final long getCount() {
		return count;
	}

	public final void setCount(final long count) {
		this.count = count;
	}

	@Override
	public String toString() {
		return "LoadElement [min=" + min + ", avg=" + avg + ", max=" + max + ", time=" + time + ", count=" + count
				+ "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(avg);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (int) (count ^ (count >>> 32));
		temp = Double.doubleToLongBits(max);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(min);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + (int) (time ^ (time >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LoadElement other = (LoadElement) obj;
		if (Double.doubleToLongBits(avg) != Double.doubleToLongBits(other.avg))
			return false;
		if (count != other.count)
			return false;
		if (Double.doubleToLongBits(max) != Double.doubleToLongBits(other.max))
			return false;
		if (Double.doubleToLongBits(min) != Double.doubleToLongBits(other.min))
			return false;
		if (time != other.time)
			return false;
		return true;
	}

}
