package de.unileipzig.irpsim.server.data.stammdaten;

import java.io.IOException;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;
import net.javacrumbs.jsonunit.JsonMatchers;

public class TestWrongJSON extends ServerTests {

	private static final Logger LOG = LogManager.getLogger(TestWrongJSON.class);

	@Before
	public void cleanDBUp() {
		DatabaseTestUtils.cleanUp();
	}

	@Test
	public void testWrongJSON() throws IOException {
		final Stammdatum stammdatum = StammdatenTestExamples.getThermalLoadExample();
		final String wrongJson = new ObjectMapper().writeValueAsString(stammdatum).substring(0, 100);
		System.out.println(wrongJson);
		final Response putResponse = RESTCaller.callPutResponse(ServerTestUtils.URI + "stammdaten/", wrongJson);

		final String responseText = putResponse.readEntity(String.class);
		LOG.debug(responseText);
		MatcherAssert.assertThat(responseText, JsonMatchers.jsonNodePresent("messages"));
	}
}
