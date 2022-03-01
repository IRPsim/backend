package de.unileipzig.irpsim.server.data.stammdaten.datensatz;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityTransaction;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.data.timeseries.LoadElement;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.standingdata.data.AlgebraicData;
import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;
import de.unileipzig.irpsim.core.standingdata.data.Variable;
import de.unileipzig.irpsim.server.data.stammdaten.StammdatenTestExamples;
import de.unileipzig.irpsim.server.data.stammdaten.StammdatenTestUtil;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;
import net.javacrumbs.jsonunit.JsonMatchers;

/**
 * Testet das Hinzufügen und Abrufen von Datensätzen als statische Daten über REST ohne Nutzung der Excel-Schnittstelle.
 */
public class AlgebraicDataTest extends ServerTests {
	private static final Logger LOG = LogManager.getLogger(AlgebraicDataTest.class);

	private static final ObjectMapper mapper = new ObjectMapper();
	private static int id;
	private static final AlgebraicData example = new AlgebraicData();

	@Before
	public void initializeData() throws JsonProcessingException {
		StammdatenTestUtil.cleanUp();
		StammdatenTestUtil.createPrognoseszenarien();

		example.setSzenario(1);
		example.setJahr(2016);
		example.setFormel("result = A.*5 + B.*100");

		final Stammdatum sdA, sdB;
		try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final EntityTransaction et = em.getTransaction();
			et.begin();

			sdA = new Stammdatum();
			sdA.setName("Name A");
			sdA.setBezugsjahr(2016);
			em.persist(sdA);

			sdB = new Stammdatum();
			sdB.setName("Name B");
			sdB.setBezugsjahr(2016);
			em.persist(sdB);

			et.commit();
		}

		final Map<String, Variable> zuordnung = new HashMap<>();
		final Variable A = new Variable();
		A.setJahr(2016);
		A.setStammdatum(sdA);
		zuordnung.put("A", A);

		final Variable B = new Variable();
		B.setJahr(2016);
		B.setStammdatum(sdB);
		zuordnung.put("B", B);

		example.setVariablenZuordnung(zuordnung);

		id = StammdatenTestUtil.addStammdatum(StammdatenTestExamples.getElectricLoadExample());
	}

	@Test
	public void testChangedYear() throws IOException {

		final Response putResponseData = RESTCaller.callPutResponse(ServerTestUtils.URI + "stammdaten/" + id + "/algebraicdata", mapper.writeValueAsString(example));
		final String responseString = putResponseData.readEntity(String.class);
		LOG.debug(responseString);
		final ObjectMapper objectMapper = new ObjectMapper();
		final List<Integer> ids = objectMapper.readValue(responseString, new TypeReference<List<Integer>>() {
      });

		Assert.assertEquals(ids.size(), 12);

		example.setJahr(2018);
		final Response putResponseData2 = RESTCaller.callPutResponse(ServerTestUtils.URI + "stammdaten/" + id + "/algebraicdata", mapper.writeValueAsString(example));
		final List<Integer> idsRemoved = objectMapper.readValue(putResponseData2.readEntity(String.class), new TypeReference<List<Integer>>() {
      });

		Assert.assertEquals(ids.size() - 4, idsRemoved.size());
	}

	@Test
	public void testDataLoading() throws IOException {
		example.setFormel("result = ones(35040)");
		example.setVariablenZuordnung(new HashMap<>());
		final Response putResponseData = RESTCaller.callPutResponse(ServerTestUtils.URI + "stammdaten/" + id + "/algebraicdata", mapper.writeValueAsString(example));
		final String responseString = putResponseData.readEntity(String.class);
		LOG.debug(responseString);
		final ObjectMapper objectMapper = new ObjectMapper();
		final List<Integer> ids = objectMapper.readValue(responseString, new TypeReference<List<Integer>>() {
      });

		Assert.assertEquals(ids.size(), 12);
		final int firstId = ids.get(0);

		final Response getResponse = RESTCaller.callGetResponse(ServerTestUtils.URI + "stammdaten/concretedata?seriesid=" + firstId);
		final String jsonString = getResponse.readEntity(String.class);
		LOG.debug(jsonString.substring(0, 100));
		final Map<Integer, List<LoadElement>> timeseriesData = new ObjectMapper().readValue(jsonString, new TypeReference<Map<Integer, List<LoadElement>>>() {
      });
		final List<LoadElement> currentData = timeseriesData.get(firstId);
		MatcherAssert.assertThat(currentData, Matchers.hasSize(35040));

	}

	@Test
	public void testChangedPrognosehorizont() {
		// TODO
	}

	@Test
	public void testChangedData() throws JsonProcessingException {
		example.setFormel("result = A .* 5 + C");
		@SuppressWarnings("unused")
		final Response putResponseData = RESTCaller.callPutResponse(ServerTestUtils.URI + "stammdaten/" + id + "/algebraicdata", mapper.writeValueAsString(example));

		final Response getResponse = RESTCaller.callGetResponse(ServerTestUtils.URI + "stammdaten/" + id + "/algebraicdata?jahr=2025");
		final String responseString = getResponse.readEntity(String.class);
		MatcherAssert.assertThat(responseString, JsonMatchers.jsonPartEquals("[0].formel", example.getFormel()));

		example.setFormel("result = A .* 3 + B");
		@SuppressWarnings("unused")
		final Response putResponseData2 = RESTCaller.callPutResponse(ServerTestUtils.URI + "stammdaten/" + id + "/algebraicdata", mapper.writeValueAsString(example));
		// final String responseString2 = putResponseData2.readEntity(String.class);

		final Response getResponse2 = RESTCaller.callGetResponse(ServerTestUtils.URI + "stammdaten/" + id + "/algebraicdata?jahr=2023");
		final String responseString2 = getResponse2.readEntity(String.class);
		MatcherAssert.assertThat(responseString2, JsonMatchers.jsonPartEquals("[0].formel", example.getFormel()));
	}

	@Test
	public void testDelete() throws JsonProcessingException {
		final Response putResponseData = RESTCaller.callPutResponse(ServerTestUtils.URI + "stammdaten/" + id + "/algebraicdata", mapper.writeValueAsString(example));
		final JSONArray ids = new JSONArray(putResponseData.readEntity(String.class));

		final Response getResponse = RESTCaller.callGetResponse(ServerTestUtils.URI + "stammdaten/" + id + "/algebraicdata?jahr=2025");
		final String responseString = getResponse.readEntity(String.class);
		MatcherAssert.assertThat(responseString, Matchers.not(JsonMatchers.jsonEquals("[]")));

		final Response deleteResponse = RESTCaller.callDeleteResponse(ServerTestUtils.URI + "stammdaten/" + id + "/algebraicdata/" + ids.getInt(0));
		Assert.assertEquals(Status.OK.getStatusCode(), deleteResponse.getStatus());

		final Response getResponse2 = RESTCaller.callGetResponse(ServerTestUtils.URI + "stammdaten/" + id + "/algebraicdata?jahr=2025");
		final String responseString2 = getResponse2.readEntity(String.class);
		LOG.debug(responseString2);
		MatcherAssert.assertThat(responseString2, JsonMatchers.jsonEquals("[]"));
	}
}
