package de.unileipzig.irpsim.utils.data.excel;

import org.apache.poi.ss.util.CellReference;

/**
 * Beinhaltet die Position eines Datenelements in einer Excel-Tabelle.
 * @author reichelt
 *
 */
public class ExcelPosition {
	private String column;
	private int row;

	/**
	 * Erstellt neue ExcelPosition-Instanz.
	 * @param string TODO
	 */
	public ExcelPosition(final String string) {
		column = string.replaceAll("[0-9]", "");
		setRow(Integer.valueOf(string.replaceAll("[A-z]", "")));
	}

	/**
	 * Liefert die Spaltennummer.
	 * @return Die Nummer der Spalte
	 */
	public final int getColumnNumber() {
		return CellReference.convertColStringToIndex(column);
	}

	/**
	 * Liefert die Nummer der Reihe.
	 * @return Die Nummer der Reihe als Integer
	 */
	public final int getRow() {
		return row;
	}

	/**
	 * Setzt die Reihe.
	 * @param row Die zu setzende Reihe
	 */
	public final void setRow(final int row) {
		this.row = row;
	}
}