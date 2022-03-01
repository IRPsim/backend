package de.unileipzig.irpsim.utils;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/**
 * Nennt alle mehrfach genutzten Optionen aus dem Util-Paket und stellt Sie sowie eine Funktionalität, die dazugehörigen Options zu erstellen, bereit.
 *
 * @author reichelt
 */
public enum UtilOptions {
	PARTIALSCENARIOFILE("ps", "partialScenarioFile", "Pfad zur Teilszenariobeschreibung");

	private String shortName, longName, description;

	/**
	 * @param shortName
	 *            Optionsname - Kurzform
	 * @param longName
	 *            Optionsname - Langform
	 * @param description
	 *            Beschreibung der Option
	 */
	UtilOptions(final String shortName, final String longName, final String description) {
		this.shortName = shortName;
		this.longName = longName;
		this.description = description;
	}

	/**
	 * Baut die Option aus der aktuellen UtilOption.
	 *
	 * @return Option, die für einen CLI-Aufruf genutzt werden kann
	 */
	public Option build() {
		return Option.builder(shortName).longOpt(longName).hasArg().desc(description).build();
	}

	public String getLongName() {
		return longName;
	}

	/**
	 * Baut ein Options-Objekt aus einer Auflistung von UtilOptions.
	 *
	 * @param options
	 *            UtilOption-Objekte, aus denen das Options-Objekt erstellt werden soll
	 * @return Options-Objekt für einen CLI-Aufurf
	 */
	public static Options buildAll(final UtilOptions... options) {
		final Options resultOptions = new Options();
		for (final UtilOptions o : options) {
			resultOptions.addOption(o.build());
		}
		return resultOptions;
	}

}
