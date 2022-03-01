package de.unileipzig.irpsim.server.optimisation.queue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.simulation.data.json.IntermediarySimulationStatus;
import de.unileipzig.irpsim.core.simulation.data.persistence.State;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.server.ServerStarter;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;

/**
 * Testet, ob nach einem Neustart der alte Job auf Interrupted gesetzt ist.
 * 
 * @author reichelt
 *
 */
public class ServerRestartTest extends ServerTests {

	private static final Logger LOG = LogManager.getLogger(ServerRestartTest.class);

	@Test
	public void testRestart() throws IOException, InterruptedException, SQLException {

		final long parameterId = ServerTestUtils.putScenarioWithREST(TestFiles.FULL_YEAR.make());
		final String parameterset = RESTCaller.callGet(ServerTestUtils.SZENARIEN_URI + "/" + parameterId);
		final long jobId = startSimulation(parameterset);

		Thread.sleep(2000);

		ServerTestUtils.getInstance().stopServerDontKillJobs();

		Thread.sleep(2000);

		ServerTestUtils.getInstance().startServer();
		ServerStarter.synchronizeJobsFromDatabase();

		Thread.sleep(2000);

		final String nonRunningIds = RESTCaller.callGet(ServerTestUtils.OPTIMISATION_URI);
		final ObjectMapper om = new ObjectMapper();
		final List<Long> ids = om.readValue(nonRunningIds, new TypeReference<List<Long>>() {
		});
		MatcherAssert.assertThat(ids, Matchers.hasItem(jobId));

		final String statusInterrupted = RESTCaller.callGet(ServerTestUtils.OPTIMISATION_URI + "/" + jobId);
		final IntermediarySimulationStatus status = om.readValue(statusInterrupted, IntermediarySimulationStatus.class);
		Assert.assertEquals(State.INTERRUPTED, status.getState());

		final String runningGet = RESTCaller.callGet(ServerTestUtils.OPTIMISATION_URI + "?running=true");
		final List<Long> runningIds = om.readValue(runningGet, new TypeReference<List<Long>>() {
		});
		Assert.assertFalse("Kein neuer Job begonnen!", runningIds.isEmpty());
		MatcherAssert.assertThat(runningIds, Matchers.hasSize(1));

		Thread.sleep(2000);

		RESTCaller.callDeleteResponse(ServerTestUtils.OPTIMISATION_URI + "/" + runningIds.get(0) + "?delete=true");

		LOG.info("Test beendet");
	}

}
