package de.unileipzig.irpsim.utils.data.excel;

/**
 * Beinhatlet die Daten einer Definition eines Parameters in Excel, d.h. einer Zeile in einer input-specification.
 * 
 * @author reichelt
 *
 */
public class ExcelDefinitionsData {

	private final String parameterName;
	private final String sheetName;
	private final String[] rangeElements;
	private final int declaredDimension;
	private final ExcelPosition[] rangePositions = new ExcelPosition[2];

	public ExcelDefinitionsData(final String line) {
		final String[] parts = line.split("=");
		// LOG.debug("Teile: {}", Arrays.toString(parts));
		parameterName = parts[1].split(" ")[0];
		// LOG.debug(parts[2]);
		final String[] thirdpart = parts[2].split("!");
		sheetName = thirdpart[0];

		// LOG.trace("Teile: {}", thirdpart[1]);
		final String range = thirdpart[1].substring(0, thirdpart[1].indexOf(" "));
		rangeElements = range.split(":");
		System.out.println(parts[1] + " " + parts[2] + " " + parts[3]);
		declaredDimension = Integer.parseInt(parts[3].replace(" ", "").replace("	", ""));

		rangePositions[0] = new ExcelPosition(rangeElements[0]);
		rangePositions[1] = new ExcelPosition(rangeElements[1]);
		// LOG.info("Name: {} Sheet: {} Range: {} Range1: {} Dimension: {}", parameterName, sheetName, range, rangePositions[0], declaredDimension);
	}

	public ExcelPosition[] getRangePositions() {
		return rangePositions;
	}

	public final String getParameterName() {
		return parameterName;
	}

	public final String getSheetName() {
		return sheetName;
	}

	public final String[] getRangeElements() {
		return rangeElements;
	}

	public final int getDeclaredDimension() {
		return declaredDimension;
	}
}