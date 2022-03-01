package de.unileipzig.irpsim.server.optimisation;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParameters;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.server.modelstart.JavaModelStarter;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Testet die Funktionalitäten des Basismodells.
 *
 * @author kluge
 */
public final class IRPactMultipleYearTest extends ServerTests {
	private static final Logger LOG = LogManager.getLogger(IRPactMultipleYearTest.class);

	@Rule
	public TestName name = new TestName();

	@Test
	public void testIRPACTIsAvailable(){
		Assert.assertTrue(JavaModelStarter.getModelPath().exists());
	}

	@Test
	public void testBasicRequest() throws IOException {
		LOG.debug("Starte Mehrjahrestest");
		final String content = DatabaseTestUtils.getParameterText(TestFiles.IRPACT_MULTIPLE_YEAR.make());
		LOG.trace("Anfrage: " + content);
		final long jobid = ServerTestUtils.startSimulation(content);

		ServerTestUtils.waitForSimulationEnd(jobid);

		final String resultstring = RESTCaller.callGet(ServerTestUtils.OPTIMISATION_URI + "/" + jobid + "/results");
		LOG.info("Antwort: {}", resultstring);
		final JSONParametersMultimodel gpj = new ObjectMapper().readValue(resultstring, JSONParametersMultimodel.class);
		JSONParameters result = gpj.getModels().get(0);
		
		Assert.assertNotNull(result);
		Assert.assertNotNull(result.getYears().get(0));
		Assert.assertNotNull(result.getYears().get(2));

		// for testing propose is the par_out_S_DS incremented by 1 for every successful year as of 17.03.2021
		Map<String, LinkedHashMap<String, LinkedHashMap<String, Object>>> sets = result.getYears().get(2).getSets();
		if (sets != null && sets.isEmpty()) {
			Assert.assertTrue(sets.containsKey("set_side_cust"));
			LinkedHashMap<String, LinkedHashMap<String, Object>> side_cust = sets.get("set_side_cust");
			LinkedHashMap<String, Object> set_BUM = side_cust.get("BUM");
			Object actualO = set_BUM.get("par_out_S_DS");
			if (actualO instanceof Number) {
				int actual = (Integer) actualO;
				Assert.assertEquals(4, actual);
			} else {
				Assert.fail();
			}
			LinkedHashMap<String, Object> set_TRA = side_cust.get("TRA");
			Object actualA = set_TRA.get("par_out_S_DS");
			if (actualA instanceof Number) {
				int actual = (Integer) actualA;
				Assert.assertEquals(4, actual);
			} else  {
				Assert.fail();
			}
		}
	}
}
