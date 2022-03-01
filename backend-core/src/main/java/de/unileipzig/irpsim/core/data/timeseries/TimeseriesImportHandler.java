package de.unileipzig.irpsim.core.data.timeseries;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import javax.persistence.EntityTransaction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import de.unileipzig.irpsim.core.data.simulationparameters.TimeseriesExistsException;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;

/**
 * Verwaltet den Import einer vollständigen Zeitreihe in die Datenbank.
 *
 * @author reichelt
 */
public class TimeseriesImportHandler {
	private static final Logger LOG = LogManager.getLogger(TimeseriesImportHandler.class);

	private static final double MILLI = 10E6;

	private final Integer seriesid;

	/**
	 * Konstruktor mit übergebenem Namen der Zeitreihe.
	 *
	 * @param timeseriesname
	 *            Name der Zeitreihe
	 */
	public TimeseriesImportHandler(final Integer timeseriesname) {
		this.seriesid = timeseriesname;
	}

	/**
	 * Konstruktor mit übergebenen Zeitreihendaten.
	 *
	 * @param data
	 *            Zeitreihendaten
	 */
	public TimeseriesImportHandler(final Timeseries data) {
		this.seriesid = data.getSeriesname();
	}

	/**
	 * Importiert die übergebenen Zeitreihenwerte in eine Zeitreihe mit dem übergebenen Namen in die Datenbank. Das Startdatum gibt an, ab welchem Datum die Zeitreihe beginnt; ab dort werden alle
	 * Werte mit 15-Minuten-Abstand importiert.
	 *
	 * @param date
	 *            Startdatum des Imports
	 * @param values
	 *            Liste der Werte, die importiert werden sollen
	 * @return Ob der Import durchgeführt werden konnte bzw. die neue Zeitreihe der zuvor Vorhandenen entspricht
	 * @throws SQLException
	 *             Wenn ein Fehler beim Schreiben in die Datenbank vorkommt
	 * @throws TimeseriesExistsException
	 *             Wenn die Zeitreihe mit diesem Namen bereits existiert
	 */
	public final boolean executeImport(final DateTime date, final List<Number> values, final boolean isIn)
			throws SQLException, TimeseriesExistsException {
		final String tableName = isIn ? "series_data_in" : "series_data_out";

		LOG.trace("Füge import hinzu: {} Länge: {}", seriesid, values.size());

		LOG.trace("Beginne Zeitreihenimport: {}", seriesid);
		final long start = System.nanoTime();
		File outputFile = findFreeOutputFileName();

		final long timeInMillis = date.getMillis();
		LOG.debug("Schreibe in: {} Zeit: {} ({})", outputFile.getAbsolutePath(), date, timeInMillis);
		writeData(values, outputFile, timeInMillis);

		// LOAD DATA INFILE ist hier deutlich schneller als INSERT INTO; deshalb ist es besser, die umständliche Datei-schreiben-LOAD-Variante beizubehalten
		final String sql = "LOAD DATA infile '" + DatabaseConnectionHandler.getInstance().getMysqlSQLPath() + outputFile.getName()
				+ "' INTO TABLE " + tableName + " fields terminated by ';' lines terminated by '\n'";
		LOG.trace("Statement: {}", sql);
		try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final EntityTransaction et = em.getTransaction();
			et.begin();
			em.createNativeQuery(sql).executeUpdate();
			et.commit();
		}
		outputFile.delete();
		LOG.debug("Import-Zeit: {} ms für {}", (System.nanoTime() - start) / MILLI, seriesid);
		return true;
	}

	private void writeData(final List<Number> values, File outputFile, final long timeInMillis) {
		try (FileWriter fw = new FileWriter(outputFile)) {
			long i = 0;
			for (final Number value : values) {
				final long time = timeInMillis + i * 15 * 60 * 1000;
				fw.write(seriesid + ";" + time + ";" + value + "\n");
				i++;
			}
			fw.flush();
			LOG.trace("Anzahl Werte: {}", values.size());
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private File findFreeOutputFileName() {
		File outputFile = new File(DatabaseConnectionHandler.getInstance().getMysqlJavaPath(), "temp_" + seriesid + ".csv");
		while (outputFile.exists()) {
			outputFile = new File(DatabaseConnectionHandler.getInstance().getMysqlJavaPath(), outputFile.getName() + "_2");
		}
		return outputFile;
	}

}
