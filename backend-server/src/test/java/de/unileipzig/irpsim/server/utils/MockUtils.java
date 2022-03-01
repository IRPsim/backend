package de.unileipzig.irpsim.server.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityTransaction;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationYearPersistent;
import de.unileipzig.irpsim.core.utils.TestFiles;

/**
 * Werkzeuge und Methoden, um Mockobjekte für irpsim zu erzeugen.
 *
 * @author krauss
 */
public final class MockUtils {

	private static final Logger LOG = LogManager.getLogger(MockUtils.class);

	/**
	 * Hilfsklassenkonstruktor.
	 */
	private MockUtils() {
	}

	/**
	 * Erstelle die Ergebnisse eines Optimierungsvorganges für Testzwecke.
	 * Berechnet NICHT!
	 *
	 * @return Die JobId in der DB
	 */
	public static long createSimpleResults() {
		final OptimisationJobPersistent sjp = new OptimisationJobPersistent();
		try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			try {
				final EntityTransaction et = em.getTransaction();
				et.begin();
				em.persist(sjp);
				final Path folder = TestFiles.TEST_PERSISTENCE_FOLDER.toPath().resolve(sjp.getId().toString());
				FileUtils.deleteDirectory(folder.toFile());
				final List<OptimisationYearPersistent> yearsPersistent = new LinkedList<>();
				for (int yearIndex = 0; yearIndex < 3; yearIndex++) {
					LOG.debug("Creating: {}", yearIndex);
				   OptimisationYearPersistent year = createYear(folder, yearIndex);
					yearsPersistent.add(year);
					year.setJob(sjp);
               em.persist(yearsPersistent.get(yearIndex));
				}
				sjp.setYears(yearsPersistent);
				em.flush();
				et.commit();
				LOG.debug("Mockeinträge in der DB erzeugt.");
			} catch (final Exception e) {
				e.printStackTrace();
			}
		}
		return sjp.getId();
	}

   private static OptimisationYearPersistent createYear(final Path folder, int yearIndex) throws IOException {
      OptimisationYearPersistent year = new OptimisationYearPersistent();
      final Path lstFile = folder.resolve("model-0" + File.separator + "year-" + yearIndex + File.separator + "listing.lst");
      year.setLstFile(lstFile.toAbsolutePath().toString());
      Files.createDirectories(Paths.get(year.getLstFile()).getParent());
      Files.createFile(Paths.get(year.getLstFile()));
      LOG.debug("leere Lstdatei {} erstellt: {}", year.getLstFile(),
      		Files.exists(Paths.get(year.getLstFile())));
      return year;
   }

}
