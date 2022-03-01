package de.unileipzig.irpsim.utils.migration;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Beinhaltet Abbildungen von generierten und realen IDs einer Datentabelle.
 * @author reichelt
 *
 */
public class AnonymusIdHandler {

	private final Map<String, String> artificialIdMap;
	private final String tablename;

	/**
	 * Erzeugt neue AnonymusIdHandler-Instanz, setzt tablename und erzeugt neue artificialIdMap.
	 * @param tablename Der Name der Tabelle
	 */
	public AnonymusIdHandler(final String tablename) {
		artificialIdMap = new HashMap<>();
		this.tablename = tablename;
	}

	/**
	 * Fügt einen Wert in die artificialIdMap ein.
	 * @param realid Die reale ID des Wertes
	 * @param generatedid Die generierte ID des Wertes
	 */
	public final void addValue(final String realid, final String generatedid) {
		artificialIdMap.put(realid, generatedid);
	}

	/** Erstell und liefert eine Query.
	 * @return Die erstellte Query
	 */
	public final String getQuery() {
		String query = "INSERT INTO AnonymMapping(tablename, realname, generatedname) VALUES ";
		for (Map.Entry<String, String> entry : artificialIdMap.entrySet()) {
			query += "('" + tablename + "', '" + entry.getKey() + "', '" + entry.getValue() + "'),\n";
		}
		query = query.substring(0, query.length() - 2) + ";";
		return query;
	}

	/**
	 * Schreibt die erstellte Query in eine .sql-Datei.
	 */
	public final void writeQueryToFile() {
		try (BufferedWriter bw = new BufferedWriter(new FileWriter("query.sql", true))) {
			String query = getQuery();
			bw.write(query);
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
