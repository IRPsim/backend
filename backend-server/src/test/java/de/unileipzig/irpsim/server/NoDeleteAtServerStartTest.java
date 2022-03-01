package de.unileipzig.irpsim.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.unileipzig.irpsim.core.data.simulationparameters.OptimisationScenario;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.server.utils.ServerTests;

/**
 * Testet, ob Szenarien, die als nicht zu löschen gekennzeichnet sind auch nicht gelöscht werden, wenn der anfängliche Import bei Serverstart durchgeführt wird.
 *
 * @author krauss
 */
public final class NoDeleteAtServerStartTest extends ServerTests {

	private static final Path RESOURCES = Paths.get("target", "test-classes");
	private static final Path SCENARIO_DIR = RESOURCES.resolve("scenarios");
	private final List<Path> copies = new ArrayList<>();
	private Path bin;

	/**
	 * Kopiert und löscht die Dateien aus den Testressourcen, die nicht ordentlich formatiert sind.
	 *
	 * @throws IOException
	 */
	@Before
	public void takeOutUnreadableFiles() throws IOException {
		bin = Files.createTempDirectory(RESOURCES, "bin");
		for (final Iterator<Path> pathIterator = Files.list(SCENARIO_DIR).iterator(); pathIterator.hasNext();) {
			final Path oldFile = pathIterator.next();
			final Path copy = Files.copy(oldFile, bin.resolve(oldFile.getFileName()));
			if (Files.isWritable(copy) && Files.isReadable(copy)) {
				copies.add(copy);
				Files.delete(oldFile);
			} else { // Kann egtl nicht passieren.
				throw new IOException("Temporäre Datei konnte nicht wiederholbar angelegt werden!");
			}
		}
	}

	/**
	 * Kopiert die Dateien zurück.
	 *
	 * @throws IOException
	 */
	@After
	public void refillUnreadableFiles() throws IOException {
		for (final Path badFile : copies) {
			final Path badPath = SCENARIO_DIR.resolve(badFile.getFileName());
			final Path copy = Files.copy(badFile, badPath);
			if (Files.isReadable(copy)) {
				Files.delete(badFile);
			} else { // Kann egtl nicht passieren.
				throw new IOException("Temporäre Datei konnte nicht wiederholbar angelegt werden!");
			}
		}
		if (Files.list(bin).count() == 0) {
			Files.delete(bin);
		} else {
			throw new IOException("Nicht alle Testszenarien zurückverschoben!");
		}
	}

	/**
	 * {@link NoDeleteAtServerStartTest}.
	 */
	@Test
	public void testUndeletability() {
		final OptimisationScenario spm = new OptimisationScenario();
		spm.setCreator("Test-System");
		spm.setDate(new Date());
		spm.setDeletable(true);
		spm.setDescription("Dieses Szenario wurde zu Testzwecken erstellt und sollte im Nachgang des Tests automatisch wieder gelöscht werden.");
		spm.setName("Test: Anwender erstelltes Szenario.");
		spm.setData("{}");
		final OptimisationScenario spm2 = new OptimisationScenario();
		spm2.setCreator("Test-System");
		spm2.setDate(new Date());
		spm2.setDeletable(false);
		spm2.setDescription("Dieses Szenario wurde zu Testzwecken erstellt und sollte während des Tests automatisch wieder gelöscht werden.");
		spm2.setName("Test: System erstelltes Szenario.");
		spm2.setData("{}");
		try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			em.getTransaction().begin();
			em.persist(spm);
			em.persist(spm2);
			em.getTransaction().commit();
		}
		new StandardszenarioImporter().initializeScenarios();
		try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			em.getTransaction().begin();
			final OptimisationScenario findSpm = em.find(OptimisationScenario.class, spm.getId());
			final OptimisationScenario findSpm2 = em.find(OptimisationScenario.class, spm2.getId());
			em.flush();
			Assert.assertNull(findSpm2);
			Assert.assertNotNull(findSpm);
			em.remove(findSpm);
			em.getTransaction().commit();
		}
	}
}
