package de.unileipzig.irpsim.server.optimisation.postprocessing;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.core.simulation.data.AggregatedResult;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersMultiModel;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.PostProcessing;
import de.unileipzig.irpsim.core.simulation.data.TimeseriesTooShortException;
import de.unileipzig.irpsim.core.simulation.data.json.IntermediarySimulationStatus;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.json.YearData;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTestUtils.TestForRun;
import de.unileipzig.irpsim.server.utils.ServerTests;

/**
 * Testet die Funktionalitäten des Basismodells.
 *
 * @author reichelt
 */
public final class BasismodellPostProcessingTest extends ServerTests {

	private static final Logger LOG = LogManager.getLogger(BasismodellPostProcessingTest.class);

	@Rule
	public TestName name = new TestName();

	@Before
	public void printName() {
		LOG.info(name.getMethodName());
	}

	public static final ObjectMapper om = new ObjectMapper();

	/**
	 * @throws InterruptedException
	 *
	 */
	@Test
	public void testOptimisationPostprocessing() throws JsonParseException, JsonMappingException, IOException, InterruptedException {
		final long jobId = ServerTestUtils.startSimulation(DatabaseTestUtils.getParameterText(TestFiles.TEST.make()));

		final TestForRun test = (id, state) -> {
			if (state.getFinishedsteps() > 0) {
				Thread.sleep(1000);
				final String resultstring = RESTCaller.callGet(ServerTestUtils.OPTIMISATION_URI + "/" + jobId + "/results");
				JSONParametersMultimodel currentResult;
				currentResult = om.readValue(resultstring, JSONParametersMultimodel.class);
				Assert.assertEquals(2015, currentResult.getModels().get(0).getYears().get(0).getConfig().getYear());
			}
		};

		ServerTestUtils.waitForSimulationEnd(jobId, test);

		final BackendParametersMultiModel result = ServerTestUtils.fetchResults(jobId);
		final PostProcessing postProcessing = result.getModels()[0].getYeardata()[0].getPostprocessing();

		Assert.assertNotNull(postProcessing);
		final Set<String> keySet = postProcessing.getSets().keySet();
		MatcherAssert.assertThat(keySet, Matchers.hasItem("set_side"));
		final Timeseries timeseries = result.getModels()[0].getYeardata()[0].getTimeseries().get("par_out_IuO_Sector_Cust");
		// final Map<String, AggregatedResult> p1 = postProcessing.getSets().get("set_side").get("p1");
		// Assert.assertEquals(2149, p1.get("par_out_L_DS_E_extcons").getSum(), 0.1);

		timeseries.loadTimeseries(true);
		final List<Number> values = timeseries.getData();

		final double sum = values.stream().reduce((a, b) -> a.doubleValue() + b.doubleValue()).get().doubleValue();

		Assert.assertEquals(sum, postProcessing.getScalars().get("par_out_IuO_Sector_Cust").getSum(), 0.001);

		final Map<String, LinkedHashMap<String, Map<String, AggregatedResult>>> yearPostprocessingSets = postProcessing.getSets();

		for (final Entry<String, LinkedHashMap<String, Map<String, AggregatedResult>>> setEntry : postProcessing.getSets().entrySet()) {
			final Map<String, Map<String, AggregatedResult>> yearSetEntry = yearPostprocessingSets.get(setEntry.getKey());
			Assert.assertNotNull(yearSetEntry);
			for (final Entry<String, Map<String, AggregatedResult>> setElementEntry : setEntry.getValue().entrySet()) {
				final Map<String, AggregatedResult> yearParameterResultMap = yearSetEntry.get(setElementEntry.getKey());
				Assert.assertNotNull(yearParameterResultMap);
				for (final Entry<String, AggregatedResult> parameterResultEntry : setElementEntry.getValue().entrySet()) {
					final AggregatedResult yearResult = yearParameterResultMap.get(parameterResultEntry.getKey());
					LOG.trace("Teste: {} ", parameterResultEntry.getKey());
					Assert.assertEquals("Werte für " + parameterResultEntry.getKey() + " falsch", yearResult, parameterResultEntry.getValue());
				}
			}
		}
	}

	@Test
	public void testMultipleYearPostprocessing() throws JsonParseException, JsonMappingException, IOException, TimeseriesTooShortException {
		final ObjectMapper om = new ObjectMapper();
		final String content = DatabaseTestUtils.getParameterText(TestFiles.MULTI_THREE_DAYS.make());
		final JSONParametersMultimodel input = om.readValue(content, JSONParametersMultimodel.class);
		LOG.info("Eingabelänge: " + input.getModels().get(0).getYears().size());
		final long jobId = startSimulation(content);

		boolean running = true;
		final String issURI = ServerTestUtils.OPTIMISATION_URI + "/" + jobId + "/";
		LOG.trace("URI: " + issURI);
		int lastSteps = 0;
		while (running) {
			final String stateString = RESTCaller.callGet(issURI);
			LOG.debug("GET: " + stateString + " " + stateString.getClass());

			final IntermediarySimulationStatus iss = om.readValue(stateString, IntermediarySimulationStatus.class);
			running = iss.isRunning();
			final int steps = iss.getFinishedsteps();

			MatcherAssert.assertThat(steps, Matchers.lessThanOrEqualTo(iss.getSimulationsteps()));
			LOG.debug("Running: {} FinishedSteps: {}", running, steps);

			if (running && steps > lastSteps) {
				final String resultString = RESTCaller.callGet(issURI + "results");
				LOG.debug("resultString: {}", resultString);
				LOG.debug(resultString.substring(0, Math.min(resultString.length(), 2000)));
				final BackendParametersMultiModel resultYears = new BackendParametersMultiModel(om.readValue(resultString, JSONParametersMultimodel.class));
				int lastYearIndex = resultYears.getModels()[0].getYeardata().length - 1;
				while (resultYears.getModels()[0].getYeardata()[lastYearIndex] == null) {
					lastYearIndex--;
				}
				LOG.debug("Letztes simuliertes Jahr: {}, von {} Jahren gesamt. Schritte: {}", lastYearIndex, resultYears.getModels()[0].getYeardata().length, steps);
				final BackendParametersYearData lastYear = resultYears.getModels()[0].getYeardata()[lastYearIndex];

				lastYear.getTimeseries().forEach(
						(name, timeseries) -> {
							MatcherAssert.assertThat(name + "(" + timeseries.getSeriesname() + ") expected <= " + steps + ", but was: " + timeseries.size() * 96, timeseries.size() * 96,
									Matchers.lessThanOrEqualTo(steps + 96));
						});

				lastYear.getSets().values().forEach(set -> {
					set.getElements().forEach(element -> {
						element.getTimeseries().forEach((name, timeseries) -> {
							MatcherAssert.assertThat(name + "expected <= " + (steps + 96) + ", but was: " + timeseries.size() * 96, timeseries.size() * 96,
									Matchers.lessThanOrEqualTo(steps + 96));
						});
					});
				});

				lastYear.executeOnTableTimeseries((parameter, first, second, timeseries) -> {
					MatcherAssert.assertThat(parameter + "expected <= " + steps + ", but was: " + timeseries.size() * 96, timeseries.size() * 96, Matchers.lessThanOrEqualTo(steps + 96));
				});
				lastSteps = steps;
			}

			try {
				Thread.sleep(1000);
			} catch (final InterruptedException e1) {
				e1.printStackTrace();
			}

		}

		try {
			Thread.sleep(2000);
		} catch (final InterruptedException e1) {
			e1.printStackTrace();
		}

		final String resultString = RESTCaller.callGet(ServerTestUtils.OPTIMISATION_URI + "/" + jobId + "/results");
		LOG.trace("Antwort: {}", resultString);
		final JSONParametersMultimodel overallResult = om.readValue(resultString, JSONParametersMultimodel.class);

		Assert.assertNotNull(overallResult);
		Assert.assertEquals(4, overallResult.getModels().get(0).getYears().size());

		for (final YearData yd : overallResult.getModels().get(0).getYears()) {
			if (yd != null) {
				LOG.debug("Jahr: {}", om.writeValueAsString(yd.getConfig()));
			} else {
				LOG.debug("Jahr: null");
			}
		}

		final PostProcessing postProcessing = overallResult.getModels().get(0).getPostprocessing();
		LOG.debug("postProcessing.getSets: {}", postProcessing.getSets());
		Assert.assertNotNull(postProcessing.getSets().get("set_side").get("p1").get("par_out_IuO_WSector_CustSide"));
		Assert.assertNotNull(postProcessing.getSets().get("set_side").get("p1").get("par_out_IuO_WSector_CustSide").getMax());
		Assert.assertNotNull(postProcessing.getTables().get("par_out_E_fromPss_toPss"));
		LOG.debug(postProcessing.getTables().get("par_out_E_fromPss_toPss"));
	}
}
