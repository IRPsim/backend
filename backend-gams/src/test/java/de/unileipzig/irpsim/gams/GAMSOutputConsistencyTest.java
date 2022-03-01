package de.unileipzig.irpsim.gams;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.rules.TemporaryFolder;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersMultiModel;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.TimeseriesTooShortException;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.core.utils.ParameterOutputDependenciesUtil;
import de.unileipzig.irpsim.core.utils.TestFiles;

/**
 * Testet, ob die Ausgaben von GAMS mit den Angaben in den outputDependencies übereinstimmen.
 *
 * @author krauss
 */
public class GAMSOutputConsistencyTest {

	private static final Logger LOG = LogManager.getLogger(GAMSOutputConsistencyTest.class);

	private Path workspace = null;

	@Rule
	public ErrorCollector collector = new ErrorCollector();

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	
	final Map<String, List<String>> outputDependencies = ParameterOutputDependenciesUtil.getInstance().getAllOutputDependencies(1);

	/**
	 * Führt die Berechnung und alle vorherigen Schritte einmal aus.
	 * 
	 * @throws SQLException
	 */
	@Before
	public final void copyGAMSFile() throws IOException, TimeseriesTooShortException, SQLException {
		// DatabaseTestUtils.getDatabaseConnection();
		workspace = folder.getRoot().toPath();
		Files.setPosixFilePermissions(workspace, PosixFilePermissions.fromString("rwxrwxrwx"));
		FileUtils.copyDirectory(OptimisationFolderUtil.getSimulationFolder(1), workspace.toFile());
		final String parameters = DatabaseTestUtils.getParameterText(TestFiles.TEST.make());
		JSONParametersMultimodel jsonParameters = Constants.MAPPER.readValue(parameters, JSONParametersMultimodel.class);
      final BackendParametersYearData yearData = new BackendParametersMultiModel(jsonParameters).getModels()[0].getYeardata()[0];
		final GAMSHandler handler = new GAMSHandler(workspace.toFile());
		final GAMSModelParametrisationUtil parametrizer = new GAMSModelParametrisationUtil(handler, yearData, 0);
		parametrizer.loadParameters();
		parametrizer.parameterizeModel();
		handler.expose();
		handler.startBlocking();
	}

	/**
	 * Testet die Richtigkeit der Ausgabe.
	 */
	@Test
	public final void testOutputCompleteness() throws IOException {
		LOG.debug("Starte GAMSOutputConsistencyTest.");

		final Map<Integer, List<String>> checkedYears = new HashMap<>();
		final Stream<Path> output = Files.list(workspace.resolve("output" + File.separator + "results")).sorted();
		output.filter(f -> f.toString().endsWith(".csv")).forEach(file -> {
			final String fileName = file.getFileName().toString();
			final int time = Integer.parseInt(fileName.substring(fileName.indexOf("ii") + 2, fileName.indexOf(".", fileName.indexOf("ii"))));
			final List<String> checkedElements = getOrNewCheckedElements(checkedYears, time);
			LOG.trace("Checked Elements: {}", checkedElements);
			final List<String> lines = new ArrayList<>();
			try (Stream<String> linesStream = Files.lines(file)) {
				linesStream.filter(line -> line.startsWith("# set_a")).forEach(line -> lines.add(line));
			} catch (final IOException e) {
				e.printStackTrace();
			}
			final List<String> elements = Arrays.asList(Constants.SEMICOLONPATTERN.split(lines.get(0)));
			LOG.trace("Elements: {}", elements);
			elements.stream().skip(5).forEachOrdered(element -> {
				checkElement(element, checkedElements);
			});
		});

		// // Tested, ob alle in den outputDependencies gegebenen Werte auch von
		// GAMS ausgegeben werden.
		// outputDependencies.forEach((parameter, dependencies) -> {
		// collector.checkThat(checkedYears.get(0),
		// Matchers.hasItem(parameter));
		// });

		LOG.trace("Test GamsOutputCompleteness durchgelaufen.");
	}
	
	public void checkElement(String element, List<String> checkedElements) {
	   final String[] names = element.split(Pattern.quote("("));
      collector.checkThat("Parameter doppelt in den csv Ergebnsissen vorhanden: " + element, checkedElements, Matchers.not(Matchers.hasItem(element)));
      checkedElements.add(element);
      collector.checkThat("Element nicht in den Abhängigkeiten vorgesehen: " + element, outputDependencies.keySet(), Matchers.hasItem(names[0].trim()));
      final List<String> depends = Arrays
            .asList(names[1].replaceAll("[ ')]", "").replaceAll("t[0-9]+", "set_t").replaceAll("ii[0-9]+", "set_ii").split(Pattern.quote(",")));
      try {
         collector.checkThat(outputDependencies.get(names[0].trim()).size(), Matchers.is(depends.size()));
         collector.checkThat(outputDependencies.get(names[0].trim()).get(0), Matchers.is(depends.get(0)));
      } catch (final NullPointerException n) {
         collector.addError(n);
      }
	}

	/**
	 * @param checkedYears Die bereits überprüften Jahre
	 * @param time das zu prüfende Jahr
	 * @return Die Liste mit bereits überprüften Elementen. Nötig, damit geprüft werden kann, ob ein Element mehrfach ausgegeben wird
	 */
	private List<String> getOrNewCheckedElements(final Map<Integer, List<String>> checkedYears, final int time) {
		List<String> checkedElements = checkedYears.get(time);
		if (checkedElements == null) {
			checkedElements = new ArrayList<>();
		}
		checkedYears.put(time, checkedElements);
		return checkedElements;
	}
}
