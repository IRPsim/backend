package de.unileipzig.irpsim.core.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Werkzeuge zur Beeinflussung der Daten, insbesondere für den IRPsim Kontext spezifische Methoden.
 *
 * @author krauss
 */
public final class DataUtils {

	/**
	 * Hilfsklassenkonstruktor.
	 */
	private DataUtils() {
	}

	/**
	 * Wenn der Schlüssel im Schlüsselset der Map enthalten ist, liefert diese Funktion die dazugehörige map, ansonsten erzeugt es eine neue map zu diesem Schlüssel und liefert diese zurück. Wenn die
	 * zu durchsuchende Map null ist wird null zurückgegeben.
	 *
	 * @param map
	 *            Die zu durchsuchende Map. Darf null sein
	 * @param parametername
	 *            Der zu suchende Schlüssel
	 * @param <T>
	 *            Typ des Schlüssels
	 * @param <U>
	 *            Schlüsseltyp der zu suchenden/erstellenden Map
	 * @param <V>
	 *            Werttyp der zu suchenden/erstellenden Map
	 * @return Gibt die gefundene oder neu erstellte Map zurück, oder null wenn die Eingabemap null war
	 */
	public static <T, U, V> Map<U, V> getOrNewMap(final Map<T, Map<U, V>> map, final T parametername) {
		if (map == null) {
			return null;
		}
		if (map.containsKey(parametername)) {
			return map.get(parametername);
		} else {
			final Map<U, V> newMap = new LinkedHashMap<>();
			map.put(parametername, newMap);
			return newMap;
		}
	}

	/**
	 * @param value
	 *            Einganswert
	 * @return Gibt den Eingangswert zurück außer double, dann wird auf float gecastet, erhöht die Lesbarkeit
	 */
	public static Object convertToReadable(final Object value) {
		Object value2 = value;
		if (value instanceof Number) {
			value2 = ((Number) value).doubleValue();
		}
		return value2;
	}

	public static <T, U, V> LinkedHashMap<U, V> getOrNewLinkedMap(final Map<T, LinkedHashMap<U, V>> map, final T parametername) {
		if (map == null) {
			return null;
		}
		if (map.containsKey(parametername)) {
			return map.get(parametername);
		} else {
			final LinkedHashMap<U, V> newMap = new LinkedHashMap<>();
			map.put(parametername, newMap);
			return newMap;
		}
	}
}
