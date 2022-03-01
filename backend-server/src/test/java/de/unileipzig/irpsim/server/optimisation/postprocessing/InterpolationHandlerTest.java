package de.unileipzig.irpsim.server.optimisation.postprocessing;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.data.simulationparameters.TimeseriesExistsException;
import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.core.data.timeseries.TimeseriesImportHandler;
import de.unileipzig.irpsim.core.simulation.data.AggregatedResult;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.PostProcessing;
import de.unileipzig.irpsim.core.simulation.data.Set;
import de.unileipzig.irpsim.core.simulation.data.SetElement;
import de.unileipzig.irpsim.core.standingdata.DataLoader;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.core.testutils.UseAlternateDependencies;
import de.unileipzig.irpsim.core.utils.PersistenceFolderUtil;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.server.optimisation.InterpolationHandler;

/**
 * Testet, ob der InterpolationHandler Werte vollständig und richtig interpoliert.
 *
 * @author krauss
 */
public final class InterpolationHandlerTest {

	private static final double AGGREGATED_MAX = 12.0;

	private static final double AGGREGATED_AVG = 14.0;

	private static final double AGGREGATED_SUM = 22.0;

	private static final double AGGREGATED_MIN = 10.0;

	private static final DateTime STARTDATE = new DateTime(2015, 1, 1, 0, 0);

	private static final Logger LOG = LogManager.getLogger(InterpolationHandlerTest.class);

	private static BackendParametersYearData yearBefore, yearAfter;
	private static BackendParametersYearData year = new BackendParametersYearData();

	@ClassRule
	public static UseAlternateDependencies alt = new UseAlternateDependencies(new File("src/test/resources/alternative_dependencies.json"));

	@BeforeClass
	public static void initDatabase() {
	   PersistenceFolderUtil.setPersistenceFolder(TestFiles.TEST_PERSISTENCE_FOLDER);
		DatabaseTestUtils.setupDbConnectionHandler();
	}

	/**
	 * Erstellt zwei Stützjahre und interpoliert.
	 */
	@BeforeClass
	public static void initYears() throws JsonParseException, JsonMappingException, IOException, SQLException, TimeseriesExistsException {
	   PersistenceFolderUtil.setPersistenceFolder(TestFiles.TEST_PERSISTENCE_FOLDER);

		yearBefore = buildYear(1);
		yearAfter = buildYear(6);

		final BackendParametersYearData[] years = new BackendParametersYearData[2];
		years[0] = yearBefore;
		years[1] = yearAfter;
		yearBefore.getConfig().setYear(2015);
		yearBefore.getConfig().setSimulationlength(3);
		yearBefore.getConfig().setModeldefinition(UseAlternateDependencies.ALTERNATIVE_DEPENDENCIES_ID);
		yearAfter.getConfig().setYear(2020);
		yearAfter.getConfig().setSimulationlength(3);
		yearAfter.getConfig().setModeldefinition(UseAlternateDependencies.ALTERNATIVE_DEPENDENCIES_ID);

		System.out.println(new ObjectMapper().writeValueAsString(years[0]));

		System.out.println(new ObjectMapper().writeValueAsString(years[0]));

		addFixedTimeseriesData(years);

		// final BackendParameters years = new BackendParameters(yearsgpj);
		// yearsgpj.setYears(years);

		// final ImportResultHandler importHandler = new ImportResultHandler(year);
		LOG.info("Testdaten importiert, beginne Interpolation.");
		final InterpolationHandler interpolationHandler = new InterpolationHandler(yearBefore, 2, yearAfter, 3);
		interpolationHandler.interpolateCompleteYearData();

		year = interpolationHandler.getYearData();
		// importHandler.executeImports();
	}

	public static void addFixedTimeseriesData(final BackendParametersYearData[] years) throws SQLException, TimeseriesExistsException {
		for (int index = 0; index < years.length; index++) {
			LOG.info("Importiere Index " + index);
			final List<Number> timeseries = new ArrayList<>(Arrays.asList(new Double[] { 5.5 * (index + 1), 11d * (index + 1), 12.5 * (index + 1) }));
			final BackendParametersYearData yearData = years[index];

			for (final Timeseries timeseries2 : yearData.getTimeseries().values()) {
				new TimeseriesImportHandler(timeseries2.getSeriesname()).executeImport(STARTDATE, timeseries, false);
				timeseries2.setData(timeseries);
			}
			for (final Set set : yearData.getSets().values()) {
				for (final SetElement element : set.getElements()) {
					for (final Timeseries timeseries2 : element.getTimeseries().values()) {
						final Integer seriesname = timeseries2.getSeriesname();
						new TimeseriesImportHandler(seriesname).executeImport(STARTDATE, timeseries, false);
						timeseries2.setData(timeseries);
					}
				}
			}

			yearData.getTableTimeseries().values().forEach(firstDependents -> {
				firstDependents.values().forEach(secondDependents -> secondDependents.values().forEach(timeseries2 -> {
					try {
						new TimeseriesImportHandler(timeseries2.getSeriesname()).executeImport(STARTDATE, timeseries, false);
						timeseries2.setData(timeseries);
					} catch (final Exception e) {
						LOG.error(e);
					}
				}));
			});
		}
	}

	public static BackendParametersYearData buildYear(final int start) {
		final BackendParametersYearData year = new BackendParametersYearData();
		final Timeseries tableTimeseries = new Timeseries();
		final Timeseries inputTimeseries = tableTimeseries;
		inputTimeseries.setSeriesname(1 + start);
		inputTimeseries.setData(new LinkedList<>());
		year.getTimeseries().put("Par1", inputTimeseries);
		final Timeseries inputTimeseriesSet = tableTimeseries;
		inputTimeseriesSet.setSeriesname(2 + start);
		inputTimeseriesSet.setData(new LinkedList<>());
		final Set set1 = new Set("Set1");
		set1.getElements().add(new SetElement("E1"));
		year.getSets().put("Set1", set1);
		year.getSetWithName("Set1").getElement("E1").getTimeseries().put("Par2", inputTimeseriesSet);

		final HashMap<String, Map<String, Timeseries>> hashMap = new HashMap<>();
		final HashMap<String, Timeseries> hashMap2 = new HashMap<>();
		hashMap.put("A", hashMap2);
		hashMap2.put("B", tableTimeseries);
		tableTimeseries.setSeriesname(3 + start);
		year.getTableTimeseries().put("Par3", hashMap);

		final AggregatedResult aggregatedResult = new AggregatedResult();
		aggregatedResult.setAvg(AGGREGATED_AVG);
		aggregatedResult.setMin(AGGREGATED_MIN);
		aggregatedResult.setMax(AGGREGATED_MAX);
		aggregatedResult.setSum(AGGREGATED_SUM);
		year.getPostprocessing().getScalars().put("XY", aggregatedResult);

		final AggregatedResult aggregatedResult2 = new AggregatedResult();
		aggregatedResult2.setAvg(AGGREGATED_AVG);
		year.getPostprocessing().getScalars().put("XY2", aggregatedResult2);

		final Map<String, AggregatedResult> aggregatedResultMap = new HashMap<>();
		aggregatedResultMap.put("Set1", aggregatedResult);
		year.getPostprocessing().getSets().put("Set_Type1", new LinkedHashMap<>());
		year.getPostprocessing().getSets().get("Set_Type1").put("Set1", aggregatedResultMap);

		return year;
	}

	/**
	 * Testet, ob die Jahreszahl richtig interpoliert wird.
	 */
	@Test
	public void testInterpolateYearDate() {
		Assert.assertEquals(2018, year.getConfig().getYear());
	}

	/**
	 * Testet die Interpolation von Zeitreihen, die als Referenz zur Datenbank übergeben werde.
	 */
	@Test
	public void testInterpolateTimeseriesReferences() {
		int tests = 0;
		for (final String parameter : yearBefore.getTimeseries().keySet()) {
			LOG.debug("Lade: {}", parameter);
			LOG.debug("Zeitreihen: {}", year.getTimeseries());
			final Timeseries timeseries2 = year.getTimeseries().get(parameter);
			timeseries2.loadTimeseries(true);
			final List<Double> timeseries = timeseries2.getValues();
			LOG.debug("parameter: {}", parameter);
			Assert.assertEquals(Arrays.asList(new Double[] { 8.8, 17.6, 20.0 }), timeseries);
			tests++;
		}
		Assert.assertEquals(yearBefore.getTimeseries().size(), tests);
	}

	/**
	 * Testet die Interpolation von Zeitreihen, die als Referenz zur Datenbank übergeben werden.
	 */
	@Test
	public void testInterpolateSetReferences() {
		int tests = 0;
		for (final Set setBefore : yearBefore.getSets().values()) {
			final Set set = year.getSetWithName(setBefore.getName());
			Assert.assertNotNull("Set fehlt: " + setBefore, set);
			for (final SetElement elementBefore : setBefore.getElements()) {
				final SetElement element = set.getElement(elementBefore.getName());
				Assert.assertNotNull("Setelement fehlt: " + elementBefore, element);
				for (final String parameter : elementBefore.getTimeseries().keySet()) {
					final Integer reference = element.getTimeseries().get(parameter).getSeriesname();
					final List<Number> timeseries = DataLoader.getTimeseries(Arrays.asList(reference), false).get(reference);
					Assert.assertEquals(Arrays.asList(new Double[] { 8.8, 17.6, 20.0 }), timeseries);
					tests++;
				}
			}
		}
		int setRefs = 0;
		for (final Set set : yearBefore.getSets().values()) {
			for (final SetElement element : set.getElements()) {
				setRefs += element.getTimeseries().size();
			}
		}
		Assert.assertEquals(setRefs, tests);
	}

	/**
	 * Testet die Interpolation von Zeitreihen, die als Referenz zur Datenbank übergeben werden.
	 */
	@Test
	public void testInterpolateTableReferences() {
		int tableRefs = 0;
		int tests = 0;
		for (final Map.Entry<String, Map<String, Map<String, Timeseries>>> firstDependents : yearBefore.getTableTimeseries().entrySet()) {
			final Map<String, Map<String, Timeseries>> yearFirstDependents = year.getTableTimeseries().get(firstDependents.getKey());
			for (final Map.Entry<String, Map<String, Timeseries>> secondDependents : firstDependents.getValue().entrySet()) {
				final String secondDependent = secondDependents.getKey();
				final Map<String, Timeseries> yearSecondDependents = yearFirstDependents.get(secondDependent);
				for (final Map.Entry<String, Timeseries> timeseries2 : secondDependents.getValue().entrySet()) {
					final Integer reference = yearSecondDependents.get(timeseries2.getKey()).getSeriesname();
					final List<Number> timeseries = DataLoader.getTimeseries(Arrays.asList(reference), false).get(reference);
					Assert.assertEquals(Arrays.asList(new Double[] { 8.8, 17.6, 20.0 }), timeseries);
					tests++;
				}
				tableRefs += secondDependents.getValue().size();
			}
		}
		Assert.assertEquals(tableRefs, tests);
	}

	/**
	 * Testet, ob die Werte des PostProcessing richtig und vollständig interpoliert werden.
	 */
	@Test
	public void testInterpolatePostProcessing() {
		int tests = 0;
		final PostProcessing yearPost = year.getPostprocessing();
		tests += assertAggregatedResultMap(yearPost.getScalars(), yearBefore.getPostprocessing().getScalars());
		tests += assertAggResMapMapMap(yearPost.getSets(), yearBefore.getPostprocessing().getSets());
		tests += assertAggResMapMapMap(yearPost.getTables(), yearBefore.getPostprocessing().getTables());
		final int checkCount = 9;
		Assert.assertEquals(checkCount, tests);
	}

	/**
	 * @param yearAggResMapMapMap
	 *            Die interpolierten Daten
	 * @param compareMappp
	 *            Die Vergleichsdaten
	 * @return Die Summe der bisherigen Tests zum mitzählen
	 */
	private int assertAggResMapMapMap(final Map<String, LinkedHashMap<String, Map<String, AggregatedResult>>> yearAggResMapMapMap,
			final Map<String, LinkedHashMap<String, Map<String, AggregatedResult>>> compareMappp) {
		int tests = 0;
		for (final Map.Entry<String, LinkedHashMap<String, Map<String, AggregatedResult>>> aggResMapMapBef : compareMappp.entrySet()) {
			final LinkedHashMap<String, Map<String, AggregatedResult>> yearAggResMapMap = yearAggResMapMapMap.get(aggResMapMapBef.getKey());

			for (final Map.Entry<String, Map<String, AggregatedResult>> aggResMapBef : aggResMapMapBef.getValue().entrySet()) {
				final Map<String, AggregatedResult> yearAggResMap = yearAggResMapMap.get(aggResMapBef.getKey());

				tests += assertAggregatedResultMap(yearAggResMap, aggResMapBef.getValue());
			}
		}
		return tests;
	}

	/**
	 * @param yearAggResMap
	 *            Die interpolierten Daten
	 * @param compareMap
	 *            Die Vergleichsdaten
	 * @return Die Summe der gemachten Tests
	 */
	private int assertAggregatedResultMap(final Map<String, AggregatedResult> yearAggResMap, final Map<String, AggregatedResult> compareMap) {
		int tests = 0;
		for (final Map.Entry<String, AggregatedResult> aggResBef : compareMap.entrySet()) {
			final AggregatedResult yearAggRes = yearAggResMap.get(aggResBef.getKey());
			tests += assertAggregatedResults(yearAggRes);
		}
		return tests;
	}

	/**
	 * @param yearAggRes
	 *            Die interpolierten Daten
	 * @return die Anzahl der gemachten Tests
	 */
	private int assertAggregatedResults(final AggregatedResult yearAggRes) {
		int tests = 0;
		if (yearAggRes.getMax() != null) {
			Assert.assertEquals(AGGREGATED_MAX, yearAggRes.getMax(), 0.01);
			tests++;
		}
		if (yearAggRes.getMin() != null) {
			Assert.assertEquals(AGGREGATED_MIN, yearAggRes.getMin(), 0.01);
			tests++;
		}
		if (yearAggRes.getSum() != null) {
			Assert.assertEquals(AGGREGATED_SUM, yearAggRes.getSum(), 0.01);
			tests++;
		}
		if (yearAggRes.getAvg() != null) {
			tests++;
			Assert.assertEquals(AGGREGATED_AVG, yearAggRes.getAvg(), 0.01);
		}
		return tests;
	}
}
