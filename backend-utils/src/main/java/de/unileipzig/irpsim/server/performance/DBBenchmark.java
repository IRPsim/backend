package de.unileipzig.irpsim.server.performance;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.data.simulationparameters.TimeseriesExistsException;
import de.unileipzig.irpsim.core.data.timeseries.DatabaseConnectionHandler;
import de.unileipzig.irpsim.core.data.timeseries.LoadElement;
import de.unileipzig.irpsim.core.data.timeseries.TimeseriesImportHandler;
import de.unileipzig.irpsim.core.standingdata.DataLoader;

/**
 * Ziel dieses Benchmarks ist es, folgende Charakteristika von MariaDB und Infobright (oder ggf. spätere andere Datenbankprodukte) unter dem Einfluss der Steierung der Zeitreihenmenge zu messen: 1.
 * Anfragegeschwindigkeit 2. Speichergeschwindigkeit 3. Speicherverbrauch
 * 
 * @author reichelt
 *
 */
public class DBBenchmark {

	private static final Logger LOG = LogManager.getLogger(DBBenchmark.class);

	public static void insertData(final int index, final int count) throws SQLException, TimeseriesExistsException {
		final TimeseriesImportHandler handler = new TimeseriesImportHandler(index);
		final List<Number> values = new LinkedList<>();
		final Random random = new Random();
		for (int i = 0; i < count; i++) {
			values.add(random.nextDouble());
		}
		handler.executeImport(new DateTime(), values, true);
	}

	public static void requestData(final int count) {
		for (int i = 0; i < count; i++) {
			final int firstId = new Random().nextInt(count - 1);
			final DataLoader dl = new DataLoader(Arrays.asList(firstId, firstId, firstId));
			final List<LoadElement> list = (List<LoadElement>) dl.getResultData().get(firstId);
			System.out.println(list.get(0));
		}
	}

	public static void main(final String[] args) throws SQLException, IOException, TimeseriesExistsException {

		String dburl = null, user = null, pw = null;
		dburl = System.getenv(Constants.IRPSIM_DATABASE_URL);
		user = System.getenv(Constants.IRPSIM_DATABASE_USER);
		pw = System.getenv(Constants.IRPSIM_DATABASE_PASSWORD);

		DatabaseConnectionHandler.getInstance().setUser(user);
		DatabaseConnectionHandler.getInstance().setPassword(pw);
		DatabaseConnectionHandler.getInstance().setUrl(dburl);

		final String dataFolderName = System.getenv(Constants.IRPSIM_PERSISTENCEFOLDER);

		final File dataFolder = new File(dataFolderName);

		try (final FileWriter timeWriter = new FileWriter(new File("db_time.csv"))) {
			for (int count = 1000; count < 20000; count += 1000) {

				LOG.info("Beginne Import {}", count);

				final long overallStart = System.nanoTime();

				final Connection c = DatabaseConnectionHandler.getInstance().getConnection();
				c.createStatement().executeUpdate("DROP TABLE IF EXISTS series_data_in");
				c.createStatement().executeUpdate("DROP TABLE IF EXISTS series_data_out");

				DataLoader.initializeTimeseriesTables();

				// c.createStatement().executeUpdate("CREATE INDEX series_in_index ON series_data_in(seriesid)");
				// c.createStatement().executeUpdate("CREATE INDEX series_in_index_timestamp ON series_data_in(seriesid, unixtimestamp)");

				final long startInsert = System.nanoTime();
				for (int index = 0; index < count; index++) {
					insertData(index, 35040);
				}
				final long durationInsert = System.nanoTime() - startInsert;

				final long startRequests = System.nanoTime();
				requestData(100);
				final long durationRequests = System.nanoTime() - startRequests;

				final long size = FileUtils.sizeOfDirectory(dataFolder);

				final long overallEnd = System.nanoTime() - overallStart;

				timeWriter.write(count + ";" + durationRequests / 10E6 + ";" + durationInsert / 10E6 + ";" + (size / 10E6) + ";" + overallEnd + "\n");
				timeWriter.flush();

				System.gc();
				Runtime.getRuntime().runFinalization();

				try {
					Thread.sleep(5);
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}
}
