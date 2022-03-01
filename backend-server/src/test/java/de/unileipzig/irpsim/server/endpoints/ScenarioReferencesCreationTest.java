package de.unileipzig.irpsim.server.endpoints;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.Set;
import de.unileipzig.irpsim.core.simulation.data.SetElement;
import de.unileipzig.irpsim.core.simulation.data.TimeseriesTooShortException;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.json.YearData;
import de.unileipzig.irpsim.core.standingdata.DataLoader;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.server.data.stammdaten.StammdatenTestUtil;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;

/**
 * Testet das korrekte Hinzufügen eines neuen Parametersets.
 *
 * @author krauss
 */
public class ScenarioReferencesCreationTest extends ServerTests {

	private static final Logger LOG = LogManager.getLogger(ScenarioReferencesCreationTest.class);
	private static final BackendParametersYearData EXPECTEDYEARDATA = getExpected();

	@BeforeClass
	public static void initServerTest() {
		StammdatenTestUtil.cleanUp();
	}

	/**
	 * Liefert das erste Jahr des gegebenen Parametersatzes zum Vergleich.
	 *
	 * @return Das ursprüngliche Jahr
	 */
	private static BackendParametersYearData getExpected() {
	   JSONParametersMultimodel expectedParameters = null;
		try {
			final File testFile = TestFiles.TEST.make();
			expectedParameters = new ObjectMapper().readValue(testFile, JSONParametersMultimodel.class);
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		YearData yeardata = expectedParameters.getModels().get(0).getYears().get(0);
      final BackendParametersYearData expectedYearData = new BackendParametersYearData(yeardata);
		return expectedYearData;
	}

	/**
	 * Testet das korrekte Hinzufügen eines neuen Parametersets auf die Referenzierung der Zeitreiehen.
	 */
	@Test
	public final void testTimeseriesReferences() throws JsonParseException, JsonMappingException, IOException {
		LOG.debug("Starte testTimeseriesReferences");

		final long id = ServerTestUtils.putScenarioWithREST(TestFiles.TEST.make());

		final String entity = RESTCaller.callGet(ServerTestUtils.SZENARIEN_URI + "/" + id);
		Assert.assertNotNull(entity);
		final BackendParametersYearData firstYearData = getFirstYearData(entity);
		final Collection<Timeseries> values = firstYearData.getTimeseries().values();
		values.forEach(timeseries -> {
			if (timeseries.getSeriesname() != 0) {
				LOG.debug(timeseries.getSeriesname() + " " + timeseries.size());
				Assert.assertEquals("Prüfe Zeitreihe " + timeseries.getSeriesname(), 0, timeseries.size());
			}
		});
		LOG.debug("Direkte Zeitreihen sind in Referenzen umgewandelt.");

		final List<Integer> references = new ArrayList<>();
		values.forEach(t -> references.add(t.getSeriesname()));
		final Map<Integer, List<Number>> resultReferencedTimeseries = DataLoader.getTimeseries(references, true);

		EXPECTEDYEARDATA.getTimeseries().forEach((parameter, expectedTimeseries) -> {
			final Integer seriesname = firstYearData.getTimeseries().get(parameter).getSeriesname();
			LOG.debug("Zeitreihe: " + seriesname);
			final boolean zeroTimeseries = seriesname.equals(Constants.ZERO_TIMESERIES_NAME);
			final List<Number> resultTimeseries = resultReferencedTimeseries.get(seriesname);
			assertListEquality(expectedTimeseries.getData(), resultTimeseries, zeroTimeseries);
		});
	}

	/**
	 * Testet das korrekte Hinzufügen eines neuen Parametersets auf die Referenzierung der Setzeitreihen.
	 */
	@Test
	public final void testSetTimeseriesReferences() throws JsonParseException, JsonMappingException, IOException {
		LOG.debug("Starte testSetTimeseriesReferences");

		final long id = ServerTestUtils.putScenarioWithREST(TestFiles.TEST.make());

		final String entity = RESTCaller.callGet(ServerTestUtils.SZENARIEN_URI + "/" + id);
		Assert.assertNotNull(entity);
		final BackendParametersYearData firstYearData = getFirstYearData(entity);
		firstYearData.getSets().values().forEach(set -> set.getElements().forEach(setElement -> setElement.getTimeseries().values().forEach(timeseries -> {
			if (timeseries.getSeriesname() != 0) {
				Assert.assertEquals("Prüfe Zeitreihe " + timeseries.getSeriesname(), 0, timeseries.size());
				Assert.assertEquals(new ArrayList<>(), timeseries.getData());
			}
		})));
		LOG.debug("Direkte Zeitreihen sind gelöscht.");

		final List<Integer> references = getSetReferences(firstYearData);
		final Map<Integer, List<Number>> resultTimeseriesMap = DataLoader.getTimeseries(references, true);

		EXPECTEDYEARDATA.getSets().values().forEach(set -> {
			final Set resultSet = firstYearData.getSetWithName(set.getName());
			set.getElements().forEach(element -> {
				final SetElement resultElement = resultSet.getElement(element.getName());
				element.getTimeseries().forEach((parameter, timeseries) -> {
					final Integer resultReference = resultElement.getTimeseries().get(parameter).getSeriesname();
					final boolean zeroTimeseries = resultReference.equals(Constants.ZERO_TIMESERIES_NAME);
					// System.out.println(resultTimeseriesMap.keySet());
					// resultTimeseriesMap.forEach((id1, value) -> System.out.println(id1 + " " + value.subList(0, 10)));
					final List<Number> resultTimeseries = resultTimeseriesMap.get(resultReference);
					assertListEquality(timeseries.getData(), resultTimeseries, zeroTimeseries);
				});
			});
		});
	}

	/**
	 * Testet das korrekte Hinzufügen eines neuen Parametersets auf die Referenzierung der Tabellenzeitreihen.
	 * 
	 * @throws TimeseriesTooShortException
	 */
	@Test
	public final void testTableTimeseriesReferences() throws JsonParseException, JsonMappingException, IOException, TimeseriesTooShortException {
		LOG.debug("Starte testTableTimeseriesReferences");

		final long id = ServerTestUtils.putScenarioWithREST(TestFiles.TEST.make());

		final String entity = RESTCaller.callGet(ServerTestUtils.SZENARIEN_URI + "/" + id);
		Assert.assertNotNull(entity);
		final BackendParametersYearData firstYearData = getFirstYearData(entity);
		firstYearData.executeOnTableTimeseries((parameter, first, second, timeseries) -> {
			if (timeseries.getSeriesname() != 0) {
				Assert.assertEquals(0, timeseries.size());
			}
		});
		LOG.debug("Direkte Zeitreihen sind gelöscht.");

		final List<Integer> references = getTableReferences(firstYearData);
		final Map<Integer, List<Number>> resultReferencedTimeseries = DataLoader.getTimeseries(references, true);

		EXPECTEDYEARDATA.getTableTimeseries().forEach((parameter, expectedFirstDependents) -> {
			final Map<String, Map<String, Timeseries>> resultFirstDependents = firstYearData.getTableTimeseries().get(parameter);
			Assert.assertNotNull(resultFirstDependents);
			expectedFirstDependents.forEach((firstDependent, expectedSecondDependents) -> {
				final Map<String, Timeseries> resultSecondDependents = resultFirstDependents.get(firstDependent);
				Assert.assertNotNull(resultSecondDependents);
				expectedSecondDependents.forEach((secondDependent, expectedTimeseries) -> {
					final boolean zeroTimeseries = resultSecondDependents.get(secondDependent).getSeriesname().equals(Constants.ZERO_TIMESERIES_NAME);
					final List<Number> resultTimeseries = resultReferencedTimeseries.get(resultSecondDependents.get(secondDependent).getSeriesname());
					assertListEquality(expectedTimeseries.getData(), resultTimeseries, zeroTimeseries);
				});
			});
		});
	}

	/**
	 * Überprüft die Gleichheit beider Listen mit erweiterter Gleichheitsprüfung der double Werte.
	 *
	 * @param expectedTimesValues
	 *            Die erwarteten Werte
	 * @param resultTimesValues
	 *            Die tatsächlichen Werte
	 * @param zeroTimeseries
	 *            Ob es eine 0 Zeitreihe ist
	 */
	private void assertListEquality(final List<Number> expectedTimesValues, final List<Number> resultTimesValues, final boolean zeroTimeseries) {
		Assert.assertNotNull(resultTimesValues);
		LOG.trace("Zeitreihenlänge: {} {}", expectedTimesValues.size(), resultTimesValues.size());
		if (zeroTimeseries) {
			assertZeroTimeseries(expectedTimesValues, resultTimesValues);
		} else {
			Assert.assertEquals(expectedTimesValues.size(), resultTimesValues.size());
			final Iterator<Number> resultIterator = resultTimesValues.iterator();
			for (final Number expectedNumber : expectedTimesValues) {
				Assert.assertEquals(expectedNumber.doubleValue(), resultIterator.next().doubleValue(), Constants.DOUBLE_DELTA);
			}
		}
	}

	/**
	 * Testet die Annahme, dass beide Zeitreihen Nullzeitreihen sind.
	 *
	 * @param expectedTimesValues
	 *            Die in der Ausgabe erwartete Nummer
	 * @param resultTimesValues
	 *            Die tatsächliche Ausgabe
	 */
	private void assertZeroTimeseries(final List<Number> expectedTimesValues, final List<Number> resultTimesValues) {
		LOG.trace("Zeitreihenlänge: {} {}", expectedTimesValues.size(), resultTimesValues.size());
		if (expectedTimesValues.size() > resultTimesValues.size()) {
			Assert.fail("expected at least:<" + expectedTimesValues.size() + "> but was:<" + resultTimesValues.size() + ">");
		}
		for (final Number expectedNumber : expectedTimesValues) {
			Assert.assertEquals(expectedNumber.doubleValue(), 0, Constants.DOUBLE_DELTA);
		}
		for (final Number resultNumber : resultTimesValues) {
			Assert.assertEquals(resultNumber.doubleValue(), 0, Constants.DOUBLE_DELTA);
		}
	}

	/**
	 * Liefert alle Referenzen der Tabellenzeitreihen.
	 *
	 * @param firstYearData
	 *            Die Jahresdaten
	 * @return Liste aller Tabellenreferenzen
	 * @throws TimeseriesTooShortException
	 */
	private List<Integer> getTableReferences(final BackendParametersYearData firstYearData) throws TimeseriesTooShortException {
		final List<Integer> references = new ArrayList<>();
		firstYearData.executeOnTableTimeseries((parameter, firstDepedent, SecondSependent, timeseries) -> {
			references.add(timeseries.getSeriesname());
		});
		return references;
	}

	/**
	 * Liefert alle Referenzen der Setzeitreihen.
	 *
	 * @param firstYearData
	 *            Die Jahresdaten
	 * @return Liste aller Setreferenzen
	 */
	private List<Integer> getSetReferences(final BackendParametersYearData firstYearData) {
		final List<Integer> references = new ArrayList<>();
		firstYearData.getSets().values().forEach(set -> {
			set.getElements().forEach(element -> {
				element.getTimeseries().values().forEach(timeseries -> {
					references.add(timeseries.getSeriesname());
				});
			});
		});
		return references;
	}

	/**
	 * Liefert das erste Jahr des Strings, der ein JSON Objekt enthält, als {@link BackendParametersYearData}.
	 *
	 * @param entity
	 *            Der die Daten enthaltene String
	 * @return Die Daten des ersten Jahres
	 */
	private BackendParametersYearData getFirstYearData(final String entity) throws IOException, JsonParseException, JsonMappingException {
		final ObjectMapper objectMapper = new ObjectMapper();
		final JSONParametersMultimodel parameters = objectMapper.readValue(entity, JSONParametersMultimodel.class);
		final BackendParametersYearData firstYearData = new BackendParametersYearData(parameters.getModels().get(0).getYears().get(0));
		LOG.debug(new ObjectMapper().writeValueAsString(firstYearData.getTimeseries()).substring(0, 200));
		return firstYearData;
	}
}
