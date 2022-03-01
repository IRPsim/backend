package de.unileipzig.irpsim.server.optimisation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import de.unileipzig.irpsim.core.data.timeseries.DatabaseConnectionHandler;
import de.unileipzig.irpsim.core.standingdata.data.StaticData;

/**
 * Repräsentiert Metadaten einer Datenreihe für einen Import. TODO: Wird zur Zeit nur von ImportResultHandler genutzt, man könnte es als interne Klasse deklarieren.
 *
 * @author reichelt
 */
public class ImportMetadata {
	private final String filename;
	private String reference;
	private final String parametername;
	private int count;
	private final File file;
	private FileWriter filewriter;
	private boolean isZeroTimeseries = true;

	public ImportMetadata(final StaticData datensatz, String parametername) {
		this.parametername = parametername;
		filename = "import_" + datensatz.getId() + ".csv";
		file = new File(DatabaseConnectionHandler.getInstance().getMysqlJavaPath(), filename);
		if (file.exists()) {
			file.delete();
		}
		try {
			filewriter = new FileWriter(file);
		} catch (final IOException e) {
			e.printStackTrace();
		}
		reference = datensatz.getSeriesid();
	}

	public final void setReference(final String reference) {
		this.reference = reference;
	}

	public final boolean isZeroTimeseries() {
		return isZeroTimeseries;
	}

	public final void setZeroTimeseries(final boolean isZeroTimeseries) {
		this.isZeroTimeseries = isZeroTimeseries;
	}

	public final String getFilename() {
		return filename;
	}

	public final String getReference() {
		return reference;
	}

	public final int getRowCount() {
		return count;
	}

	/**
	 * Erhöht die Anzahl der Zeitreihen-Reihen.
	 */
	public final void addRow() {
		count++;
	}

	public final File getFile() {
		return file;
	}

	public final FileWriter getWriter() {
		return filewriter;
	}

	public String getParametername() {
		return parametername;
	}
}