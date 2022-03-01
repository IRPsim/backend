package de.unileipzig.irpsim.server.optimisation;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;

@Ignore
//@RunWith(PerformanceTestRunnerJUnit.class)
public class TestDuration extends ServerTests {

	private static final Logger LOG = LogManager.getLogger(TestDuration.class);

//	@PerformanceTest(warmupExecutions = 1, executionTimes = 2)
	@Test
	public void testResults() throws InterruptedException, JsonParseException, JsonMappingException, IOException {
		final String content = DatabaseTestUtils.getParameterText(TestFiles.TEST.make());
		ServerTestUtils.getInstance();
		final long jobid = ServerTestUtils.startSimulation(content);

		ServerTestUtils.waitForSimulationEnd(jobid);
		final String testURI = ServerTestUtils.OPTIMISATION_URI + "/" + jobid + "/";
		final String resultstring = RESTCaller.callGet(testURI);
		LOG.info("GET: " + resultstring + " " + resultstring.getClass());
		final JSONObject jso = new JSONObject(resultstring);

		final int steps = (int) jso.get("finishedsteps");
		final int allSteps = (int) jso.get("simulationsteps");

		Assert.assertEquals(allSteps, steps);
	}
}
