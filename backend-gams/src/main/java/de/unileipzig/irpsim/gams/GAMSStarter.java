package de.unileipzig.irpsim.gams;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gams.api.GAMSExecutionException;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.utils.PersistenceFolderUtil;
import de.unileipzig.irpsim.core.utils.StreamGobbler;

public class GAMSStarter {
	private static final Logger LOG = LogManager.getLogger(GAMSStarter.class);

	public static void main(final String[] args) {
		if (args.length < 3) {
			LOG.error("Job-Id, Modellindex und Jahr müssen als Parameter übergeben werden.");
			System.exit(1);
		}

		final long id = Long.parseLong(args[0]);
		final int modelIndex = Integer.parseInt(args[1]);
		try {
			final int yearIndex = Integer.parseInt(args[2]);

			GAMSParameterizer.initDatabase(id);

			final File concreteWorkspaceFolder = PersistenceFolderUtil.getWorkspaceFolder(id, modelIndex, yearIndex);

			final GAMSHandler handler = new GAMSHandler(concreteWorkspaceFolder);
			handler.startBlocking();

			LOG.info("Berechnung {} beendet", id);

			createSQLite(concreteWorkspaceFolder, handler);
		} catch (final GAMSExecutionException e) {
			GAMSParameterizer.persistError(id, e);
			e.printStackTrace();
			System.exit(2);
		} catch (final Throwable t) {
			GAMSParameterizer.persistError(id, t);
			t.printStackTrace();
			System.exit(4);
		}
	}

	public static void createSQLite(final File concreteWorkspaceFolder, final GAMSHandler handler) throws IOException {
		final String gamsPath = System.getenv(Constants.LD_GAMS_PATH) != null ? System.getenv(Constants.LD_GAMS_PATH) : System.getenv("GAMS_PATH");
		LOG.info("LD_LIBRARY_PATH: {} DYLD_LIBRARY_PATH {} Gams: {}", System.getenv(Constants.LD_GAMS_PATH), System.getenv("GAMS_PATH"), gamsPath);
		final File gdx2sqliteTool;
		if (gamsPath.contains(":")) {
			gdx2sqliteTool = new File(gamsPath.substring(gamsPath.lastIndexOf(":") + 1), "gdx2sqlite");
		} else {
			gdx2sqliteTool = new File(gamsPath, "gdx2sqlite");
		}

		final File gdxResultFile = handler.getGDXResultFile();
		final File destination = new File(concreteWorkspaceFolder, "sqlite.sql");

		final ProcessBuilder builder = new ProcessBuilder(gdx2sqliteTool.getAbsolutePath(), "-fast", "-i", gdxResultFile.getAbsolutePath(), "-o", destination.getAbsolutePath());
		final Process process = builder.start();
		StreamGobbler.showFullProcess(process);
	}
}
