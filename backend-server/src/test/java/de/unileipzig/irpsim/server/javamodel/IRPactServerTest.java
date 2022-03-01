package de.unileipzig.irpsim.server.javamodel;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.simulation.data.json.IntermediarySimulationStatus;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;

/**
 * Testet, ob Anfragen an das Basismodell über den Server korrekt funktionieren.
 *
 * @author reichelt
 */
public final class IRPactServerTest extends ServerTests {
	private static final Logger LOG = LogManager.getLogger(IRPactServerTest.class);

	@Rule
	public TestName name = new TestName();

	/**
	 * Gibt den Namen des Tests aus, um besser debuggen zu können.
	 */
	@Before
	public void echoName() {
		LOG.info(name.getMethodName());
	}

	/**
	 *
	 */
	@Test
	public void testSimulationStatus() throws JsonParseException, JsonMappingException, IOException {
		final String content = DatabaseTestUtils.getParameterText(TestFiles.IRPACT.make());
		ServerTestUtils.getInstance();
		final long jobID = ServerTestUtils.startSimulation(content);
		final String resultstring = ServerTestUtils.waitForSimulationEnd(jobID);
		LOG.info("GET: " + resultstring + " " + resultstring.getClass());

		final IntermediarySimulationStatus iss = Constants.MAPPER.readValue(resultstring, IntermediarySimulationStatus.class);

		MatcherAssert.assertThat(iss.getFinishedsteps(), Matchers.equalTo(iss.getSimulationsteps()));
	}
}
