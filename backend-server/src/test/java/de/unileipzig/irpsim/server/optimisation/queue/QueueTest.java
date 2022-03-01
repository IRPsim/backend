package de.unileipzig.irpsim.server.optimisation.queue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.server.optimisation.Job;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;
import net.javacrumbs.jsonunit.JsonMatchers;
import net.javacrumbs.jsonunit.core.Option;
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert;

/**
 * Testet die Warteschlangenfunktionalität für Optimierungsjobs.
 *
 * @author reichelt
 */
public final class QueueTest extends ServerTests {

	private static final int MAX_PARALLEL_JOBS = OptimisationJobHandler.getMaxParallelJobs();
	private static final Logger LOG = LogManager.getLogger(QueueTest.class);

	/**
	 * Beendet alle aktiven und wartenden Jobs.
	 */
	@After
	public void stopAllJobs() {
		final List<Long> ids = new ArrayList<>();
		for (final Job job : OptimisationJobHandler.getInstance().getRunningJobs()) {
			ids.add(job.getId());
		}
		for (final Job job : OptimisationJobHandler.getInstance().getWaitingJobs()) {
			ids.add(job.getId());
		}
		for (final long id : ids) {
			ServerTestUtils.stopJob(id);
		}
		if (!ServerTestUtils.waitForAllJobEnds(5000)) {
			stopAllJobs();
		}
	}

	/**
	 * Testet, ob die Warteschlange nach dem Hinzufügen von 4 Jobs auch diese 4 Jobs enthält.
	 */
	@Test
	public void testQueueContainment() throws JsonParseException, JsonMappingException, IOException {
		final String content = DatabaseTestUtils.getParameterText(TestFiles.TEST.make());
		final long[] ids = new long[4];
		for (int i = 0; i < 4; i++) {
			ids[i] = ServerTestUtils.startSimulation(content);
		}
		try {
			Thread.sleep(1000);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
		final String queue = RESTCaller.callGet(ServerTestUtils.OPTIMISATION_URI + "/queue");

		LOG.debug(queue);

		final JSONArray array = new JSONArray(queue);
		final JSONObject jsonObject = array.getJSONObject(0);

		Assert.assertEquals(ids[MAX_PARALLEL_JOBS], jsonObject.getInt("id"));
		Assert.assertNotNull(jsonObject);
		MatcherAssert.assertThat(jsonObject.getBoolean("running"), Matchers.equalTo(false));
		final JSONObject jsonObject2 = array.getJSONObject(1);
		Assert.assertNotNull(jsonObject2);
		MatcherAssert.assertThat(jsonObject2.getBoolean("running"), Matchers.equalTo(false));
		Assert.assertEquals(ids[MAX_PARALLEL_JOBS + 1], jsonObject2.getInt("id"));
		JsonFluentAssert.assertThatJson(queue).isArray().ofLength(4 - MAX_PARALLEL_JOBS);
	}

	/**
	 * Testet, ob die Warteschlange nach dem Hinzufügen von 4 Jobs und dem Abarbeiten von 2 Jobs die restlichen zwei Jobs bearbeitet.
	 *
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	@Test
	public void testQueueProcessing() throws JsonParseException, JsonMappingException, IOException {
		final String content = DatabaseTestUtils.getParameterText(TestFiles.TEST.make());
		final long[] ids = new long[4];
		for (int i = 0; i < 4; i++) {
			ids[i] = ServerTestUtils.startSimulation(content);
		}

		ServerTestUtils.waitForSimulationEnd(ids[0]);

		final String queue = RESTCaller.callGet(ServerTestUtils.OPTIMISATION_URI + "/queue");

		MatcherAssert.assertThat(queue, Matchers.not(JsonMatchers.jsonPartEquals("[0]." + ids[0], "{}").when(Option.IGNORING_EXTRA_FIELDS)));
		MatcherAssert.assertThat(queue, Matchers.not(JsonMatchers.jsonPartEquals("[0]." + ids[1], "{}").when(Option.IGNORING_EXTRA_FIELDS)));
		MatcherAssert.assertThat(queue, Matchers.not(JsonMatchers.jsonPartEquals("[0]." + ids[MAX_PARALLEL_JOBS], "{}").when(Option.IGNORING_EXTRA_FIELDS)));

		final String runningJobs = RESTCaller.callGet(ServerTestUtils.OPTIMISATION_URI + "?running=true");
		LOG.debug("Laufende Jobs: {}", runningJobs);
		JsonFluentAssert.assertThatJson(runningJobs).isArray().ofLength(MAX_PARALLEL_JOBS);

	}

	/**
	 * Testet, ob sich Jobs aus der Warteschlange löschen lassen.
	 *
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	@Test
	public void testQueueKilling() throws JsonParseException, JsonMappingException, IOException {
		final String content = DatabaseTestUtils.getParameterText(TestFiles.TEST.make());
		final long[] ids = new long[4];
		for (int i = 0; i < 4; i++) {
			ids[i] = ServerTestUtils.startSimulation(content);
		}

		RESTCaller.callDeleteResponse(ServerTestUtils.OPTIMISATION_URI + "/" + ids[MAX_PARALLEL_JOBS]);

		ServerTestUtils.waitForSimulationEnd(ids[0]);

		final String queue = RESTCaller.callGet(ServerTestUtils.OPTIMISATION_URI + "/queue");

		MatcherAssert.assertThat(queue, Matchers.not(JsonMatchers.jsonPartEquals("[0]." + ids[0], "{}").when(Option.IGNORING_EXTRA_FIELDS)));
		MatcherAssert.assertThat(queue, Matchers.not(JsonMatchers.jsonPartEquals("[0]." + ids[MAX_PARALLEL_JOBS], "{}").when(Option.IGNORING_EXTRA_FIELDS)));
		MatcherAssert.assertThat(queue, Matchers.not(JsonMatchers.jsonPartEquals("[0]." + ids[MAX_PARALLEL_JOBS + 1], "{}").when(Option.IGNORING_EXTRA_FIELDS)));

		final String runningJobs = RESTCaller.callGet(ServerTestUtils.OPTIMISATION_URI + "?running=true");
		LOG.debug("Laufende Jobs: " + runningJobs);
		JsonFluentAssert.assertThatJson(runningJobs).isArray();
		MatcherAssert.assertThat(runningJobs, Matchers.containsString("" + ids[MAX_PARALLEL_JOBS + 1]));
		MatcherAssert.assertThat(runningJobs, Matchers.not(Matchers.containsString("" + ids[MAX_PARALLEL_JOBS])));

	}

	/**
	 * Testet, ob sich die Reihenfolge von Warteschlangenjobs ändern lässt.
	 *
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	@Test
	public void testQueueRescheduling() throws JsonParseException, JsonMappingException, IOException {
		final String content = DatabaseTestUtils.getParameterText(TestFiles.TEST.make());
		final List<Long> ids = new ArrayList<>(6);
		for (int i = 0; i < 6; i++) {
			ids.add(ServerTestUtils.startSimulation(content));
		}
		final List<Long> queueIds = ids.subList(MAX_PARALLEL_JOBS, ids.size());
		Collections.shuffle(queueIds);
		final JSONArray json = new JSONArray();
		for (final long qid : queueIds) {
			json.put(qid);
		}

		final Response r = RESTCaller.callPutResponse(ServerTestUtils.OPTIMISATION_URI + "/queue", json.toString());
		final String responseString = r.readEntity(String.class);
		LOG.debug("{}", responseString);
		final JSONArray response = new JSONArray(responseString);
		JsonFluentAssert.assertThatJson(responseString).isArray().ofLength(6 - MAX_PARALLEL_JOBS);
		for (int i = 0; i < queueIds.size(); i++) {
		   MatcherAssert.assertThat(response.getJSONObject(i), Matchers.notNullValue());
		}
	}
}
