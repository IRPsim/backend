package de.unileipzig.irpsim.server.endpoints;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.simulation.data.json.IntermediarySimulationStatus;
import de.unileipzig.irpsim.core.simulation.data.persistence.State;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.server.data.stammdaten.StammdatenTestUtil;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;

/**
 * Prüft, dass kein Status eines Optimierungsjobs doppelt zurückgegeben wird.
 * 
 * @author reichelt
 *
 */
public class StateEndpointTest extends ServerTests {

	public static final int COUNT = 30;

	final static ObjectMapper MAPPER = new ObjectMapper();

	private static final Logger LOG = LogManager.getLogger(StateEndpointTest.class);

	private final long ids[] = new long[COUNT];

	@Before
	public void startJobs() {
		StammdatenTestUtil.cleanUp();
		for (int i = 0; i < COUNT; i++) {
			try {
				ids[i] = startSimulation(new String(Files.readAllBytes(TestFiles.TEST.make().toPath())));
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Test
	public void testStates() throws JsonParseException, JsonMappingException, IOException {
		final String response = RESTCaller.callGet(ServerTestUtils.OPTIMISATION_URI + "/states");
		LOG.info("Response: {}", response);
		final List<IntermediarySimulationStatus> states = new ArrayList<>();
		final JSONArray jsonArray = new JSONArray(response);
		for (int i = 0; i < jsonArray.length(); i++) {
			final String intermediaryStateString = jsonArray.get(i).toString();
			states.add(MAPPER.readValue(intermediaryStateString, IntermediarySimulationStatus.class));
		}
		final List<IntermediarySimulationStatus> statesTest = new ArrayList<>(states);
		for (final IntermediarySimulationStatus state : states) {
			statesTest.remove(state);
			statesTest.forEach(iss -> Assert.assertNotEquals(state.getId(), iss.getId()));
		}

		for (int i = 0; i < COUNT; i++) {
			ServerTestUtils.stopJob(ids[i]);
		}

		try {
			Thread.sleep(2000);
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

		testAbortedOrFinished();
	}

	/*
	 * Prüft die JobStatus auf Aborted nachdem alle Jobs gestoppt wurden.
	 * Ist ein Job dann schon Finished, wird geprüft, ob es der erste ist oder ob der vorige Job ebenfalls den Status Finished hat.
	 */
	public void testAbortedOrFinished() throws JsonMappingException, JsonProcessingException {
	   final String responseFinished = RESTCaller.callGet(ServerTestUtils.OPTIMISATION_URI + "/states");
	   LOG.debug("responseFinished: {}", responseFinished);
	   final JSONArray arrayFinished = new JSONArray(responseFinished);

	   IntermediarySimulationStatus previousState = null;

	   for (int i = 0; i < arrayFinished.length(); i++) {
	      previousState = testJobState(arrayFinished, previousState, i);
	   }
	}

   private IntermediarySimulationStatus testJobState(final JSONArray arrayFinished, IntermediarySimulationStatus previousState, int i)
         throws JsonProcessingException, JsonMappingException {
      final String currentStateString;
      final IntermediarySimulationStatus currentState;
      currentStateString = arrayFinished.get(i).toString();
      currentState = MAPPER.readValue(currentStateString, IntermediarySimulationStatus.class);

      if (currentState.getState().equals(State.FINISHED)) {
         // Der erste Job darf finished sein.
         if (previousState == null) {
            previousState = currentState;
            return previousState;
         }
         // Wenn finished, muss der vorige Job auch finished sein.
         else {
            Assert.assertEquals("Prüfe Vorgänger von " + currentState.getId(), State.FINISHED, previousState.getState());
            previousState = currentState;
            return previousState;
         }
      }

      Assert.assertEquals("Prüfe Job " + currentState.getId(), State.ABORTED, currentState.getState());
      previousState = currentState;
      return previousState;
   }

	@After
	public void cleanup() {

	}
}
