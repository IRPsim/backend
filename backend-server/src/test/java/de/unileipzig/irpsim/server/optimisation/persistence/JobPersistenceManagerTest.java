package de.unileipzig.irpsim.server.optimisation.persistence;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Date;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.json.UserDefinedDescription;
import de.unileipzig.irpsim.core.simulation.data.json.YearData;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.core.simulation.data.persistence.State;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.server.modelstart.GAMSModelStarter;
import de.unileipzig.irpsim.server.utils.ServerTests;

/**
 * Unit-Test, der den OptimisationJobPersistent darauf testet, ob die zu persistierenden Dateien angelegt werden.
 * 
 * @author krauss
 */
public final class JobPersistenceManagerTest extends ServerTests {

	private static OptimisationJobPersistent persistentJob;
	private static OptimisationJobPersistenceManager manager;

	@Rule
	public TemporaryFolder temp = new TemporaryFolder(TestFiles.TEST_PERSISTENCE_FOLDER);

	/**
	 * init.
	 */
	@BeforeClass
	public static void init() {
		try {
			if (!Files.exists(TestFiles.TEST_PERSISTENCE_FOLDER.toPath())) {
				Files.createDirectory(TestFiles.TEST_PERSISTENCE_FOLDER.toPath());
			}
		} catch (final IOException e1) {
			e1.printStackTrace();
		}
		persistentJob = new OptimisationJobPersistent();
		persistentJob.setState(State.WAITING);
		persistentJob.setStart(new Date());
		persistentJob.setModelVersionHash("TestModelVersionHash42");
		persistentJob.setDescription(new UserDefinedDescription());
		persistentJob.getDescription().setSupportiveYears("0");
		persistentJob.setJsonParameter("test");
		manager = new OptimisationJobPersistenceManager(persistentJob);
	}

	/**
	 * Testet den Konstruktor.
	 */
	@Test
	public void testInitialisation() {
		Assert.assertTrue(TestFiles.TEST_PERSISTENCE_FOLDER.exists());
		try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final OptimisationJobPersistent jobPersistent = em.find(OptimisationJobPersistent.class, persistentJob.getId());
			Assert.assertNotNull(jobPersistent);
		}
	}

	/**
	 * Testet das korrekte erstellen der Ergebnisdateien.
	 */
	@Test
	@Ignore // Kann nicht mehr getestet werden, da YearDataPersistent im Subprozess angelegt werden
	public void testFilePersistence() {
		try {
			final JSONParametersMultimodel result = new JSONParametersMultimodel(1);
			final YearData year = new YearData();
			year.getConfig().setYear(2015);
			result.getModels().get(0).getYears().add(year);
			// Man müsste den caller mit Dateien mocken, test ist aber seit ewig außer Betrieb
			manager.persistYear(0, 0, Mockito.mock(GAMSModelStarter.class), result, temp.newFile("testCSV.csv"));
		} catch (final IOException e) {
			e.printStackTrace();
		}
		final File folder = new File(TestFiles.TEST_PERSISTENCE_FOLDER, persistentJob.getId() + File.separator + 0);
		// final File lstFile = new File(folder, "listing.lst");
		// Assert.assertTrue("Datei nicht gefunden: " + lstFile, lstFile.exists());
		Assert.assertTrue(new File(folder, "csvdata.csv").exists());
		Assert.assertTrue(new File(folder, "gdxresult.gdx").exists());
		Assert.assertTrue(new File(folder, "irpsim.gdx").exists());
	}
}
