package de.unileipzig.irpsim.gams;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gams.api.GAMSDatabase;
import com.gams.api.GAMSException;

import de.unileipzig.irpsim.core.simulation.data.TimeseriesTooShortException;
import de.unileipzig.irpsim.core.utils.TestFiles;

/**
 * Testet die Parametrisierung des GAMS-Basismodells.
 *
 * @author reichelt
 */
@RunWith(Parameterized.class)
public final class IRPoptDirectTest {

	/**
	 * Kopiert den GAMS-Ordner in ein Standartverzeichnis für den BasismodellDirectTest.
	 */
	@BeforeClass
	public static void copyWorkspace() throws IOException {
		FileUtils.copyDirectory(SOURCE, WORKSPACE);
	}

	private static final Logger LOG = LogManager.getLogger(IRPoptDirectTest.class);

	/**
	 * @return Die Parameter
	 */
	@Parameterized.Parameters
	public static Collection<File> timeseriesData() {
		final Set<File> testfiles = TestFiles.makeAll();
		testfiles.remove(TestFiles.FULL_YEAR.make());
		testfiles.remove(TestFiles.ERROR.make());
		testfiles.remove(TestFiles.IRPACT.make());
		testfiles.remove(TestFiles.IRPACT_MULTIPLE_YEAR.make());
		return testfiles;
	}

	private final File file;
	private static final File SOURCE = new File("src/main/resources/gams/1/");
	private static final File WORKSPACE = new File("target/directTest");

	/**
	 * @param filename
	 *            Die Quelldateien
	 */
	public IRPoptDirectTest(final File filename) {
		this.file = filename;
	}

	/**
	 * @throws IOException
	 */
	@Test
	public void testBasismodell() throws IOException {
		LOG.debug("Parametrisiere mit: {}", file.getAbsolutePath());
		final GAMSHandler gamshandler = new GAMSHandler(WORKSPACE);

		try {
			ParametrisationUtil.parameterizeModel(gamshandler, file);
		} catch (final TimeseriesTooShortException e) {
			e.printStackTrace();
		}
		GAMSDatabase gdb = null;
		try {
			gamshandler.startBlocking();
			gdb = gamshandler.getResults();

			Assert.assertNotNull(gdb);
		} catch (final GAMSException e) {
			final File lstFile = new File(WORKSPACE, "_gams_java_gjo1.lst");
			printErrorOutput(lstFile);
			Assert.fail("GAMS-Fehler aufgetreten");
		} finally {
			if (gdb != null) {
				gdb.dispose();
			}

			final File inputFile = new File(WORKSPACE, GAMSHandler.GAMSPARAMETERFILE);
			final File resultFile = new File(WORKSPACE, GAMSHandler.GAMSRESULTFILE);
			Assert.assertTrue(inputFile.exists());
			Assert.assertTrue(resultFile.exists());
		}
	}

   private void printErrorOutput(final File lstFile) throws IOException, FileNotFoundException {
      LOG.info("Printing GAMS-error output");
      try (BufferedReader br = new BufferedReader(new FileReader(lstFile))) {
      	String line;
      	while ((line = br.readLine()) != null) {
      		System.out.println(line);
      	}
      }
   }

	/**
	 * Löscht alle erstellten Dateien.
	 */
	@After
	public void cleanup() {
		FileUtils.listFiles(new File("src/main/resources/gams/"), new WildcardFileFilter("*.gdx"),
				TrueFileFilter.INSTANCE).stream().forEach(f -> f.delete());
		FileUtils.listFiles(new File("src/main/resources/gams/"), new WildcardFileFilter("*.csv"),
				TrueFileFilter.INSTANCE).stream().forEach(f -> f.delete());
		FileUtils.listFiles(new File("src/main/resources/gams/"), new WildcardFileFilter("*.log"),
				TrueFileFilter.INSTANCE).stream().forEach(f -> f.delete());
		FileUtils.listFiles(new File("src/main/resources/gams/"), new WildcardFileFilter("*.lst"),
				TrueFileFilter.INSTANCE).stream().forEach(f -> f.delete());
	}

}
