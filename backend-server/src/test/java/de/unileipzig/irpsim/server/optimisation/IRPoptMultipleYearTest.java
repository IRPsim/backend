package de.unileipzig.irpsim.server.optimisation;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.simulation.data.json.JSONParameters;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;

/**
 * Testet die Funktionalitäten des Basismodells.
 *
 * @author reichelt
 */
public final class IRPoptMultipleYearTest extends ServerTests {
	private static final Logger LOG = LogManager.getLogger(IRPoptMultipleYearTest.class);

	@Rule
	public TestName name = new TestName();

	@Test
	public void testBasicRequest() throws JsonParseException, JsonMappingException, IOException {
		LOG.debug("Starte Mehrjahrestest");
		final String content = DatabaseTestUtils.getParameterText(TestFiles.MULTIPLE_YEAR.make());
		LOG.trace("Anfrage: " + content);
		final long jobid = ServerTestUtils.startSimulation(content);

		ServerTestUtils.waitForSimulationEnd(jobid);

		final String resultstring = RESTCaller.callGet(ServerTestUtils.OPTIMISATION_URI + "/" + jobid + "/results");
		LOG.info("Antwort: {}", resultstring);
		final JSONParametersMultimodel gpj = new ObjectMapper().readValue(resultstring, JSONParametersMultimodel.class);
		JSONParameters result = gpj.getModels().get(0);
		
		Assert.assertNotNull(result);
		Assert.assertNotNull(result.getYears().get(0));
		Assert.assertNotNull(result.getYears().get(3));
		Assert.assertNotNull(result.getPostprocessing());

		System.out.println(new ObjectMapper().writeValueAsString(result.getPostprocessing()));

		final String interpolation = System.getenv("IRPSIM_INTERPOLATION");
		if (interpolation != null) {
			if (interpolation.toUpperCase() == "ALL" || interpolation.toUpperCase() == "POSTPROCESSING") {
				Assert.assertNotNull(result.getYears().get(1));
				Assert.assertNotNull(result.getYears().get(2));
			}
		} else {
			Assert.assertNull(result.getYears().get(1));
			Assert.assertNull(result.getYears().get(2));
		}

	}
}
