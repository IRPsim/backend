package de.unileipzig.irpsim.server.data.stammdaten;

import java.io.File;
import java.io.FileNotFoundException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;
import de.unileipzig.irpsim.core.utils.ParameterBaseDependenciesUtil;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;

public class TestStammdatenParent extends ServerTests {

	private static final Logger LOG = LogManager.getLogger(TestStammdatenParent.class);

	private static final ObjectMapper mapper = new ObjectMapper();

	@Before
	public void cleanDBUp() {
		StammdatenTestUtil.cleanUp();
		StammdatenTestUtil.createPrognoseszenarien();
	}

	private int id_therm, id_last;

	/**
	 * Testet, dass das hinzufügen eines Stammdatums mit Referenz-Stammdatum funktioniert.
	 * 
	 * @throws JsonProcessingException
	 */
	@Test
	public void testGetReference() throws JsonProcessingException {
		initStammdaten();
		final Response getConcreteResponse = RESTCaller.callGetResponse(ServerTestUtils.URI + "stammdaten/");
		final String concreteResponse = getConcreteResponse.readEntity(String.class);
		MatcherAssert.assertThat(concreteResponse, Matchers.containsString("" + id_last));
		MatcherAssert.assertThat(concreteResponse, Matchers.containsString("" + id_therm));
	}

	/**
	 * Testet, dass das Löschen eines Stammdatum, auf das referenziert wird, einen Fehler zurückgibt.
	 * 
	 * @throws JsonProcessingException
	 */
	@Test
	public void testDeleteWithParent() throws JsonProcessingException {
		initStammdaten();

		final Response deleteResponse = RESTCaller.callDeleteResponse(ServerTestUtils.URI + "stammdaten/" + id_last);
		final String deleteResponseString = deleteResponse.readEntity(String.class);
		MatcherAssert.assertThat(deleteResponseString, Matchers.containsString("Es zeigen noch"));
		MatcherAssert.assertThat(deleteResponseString, Matchers.containsString("" + id_therm));
	}

	public void initStammdaten() throws JsonProcessingException {
		final Stammdatum lastStammdatum = StammdatenTestExamples.getLoadExample();
		final Stammdatum thermStammdatum = StammdatenTestExamples.getThermalLoadExample();
		final String jsonInput = mapper.writeValueAsString(lastStammdatum);
		LOG.debug(jsonInput);
		final Response putResponse = RESTCaller.callPutResponse(ServerTestUtils.URI + "stammdaten/", jsonInput);
		id_last = StammdatenTestUtil.getId(putResponse);

		lastStammdatum.setId(id_last);
		thermStammdatum.setReferenz(lastStammdatum);
		final String jsonInputTherm = mapper.writeValueAsString(thermStammdatum);
		LOG.debug(jsonInputTherm);
		final Response putResponseTherm = RESTCaller.callPutResponse(ServerTestUtils.URI + "stammdaten/", jsonInputTherm);
		id_therm = StammdatenTestUtil.getId(putResponseTherm);
	}

	@Test
	public void testAddAndGetWithoutId() throws FileNotFoundException, JsonProcessingException {
		ParameterBaseDependenciesUtil.getInstance().loadDependencies(1);

		final Stammdatum electricLoadExample = StammdatenTestExamples.getElectricLoadExample();
		electricLoadExample.setTyp("Testparent");
		electricLoadExample.setAbstrakt(true);
		final int idParent = StammdatenTestUtil.addStammdatum(electricLoadExample);
		electricLoadExample.setId(idParent);

		final Stammdatum electricLoadExampleChild = StammdatenTestExamples.getElectricLoadExample();
		electricLoadExampleChild.setName(null);
		electricLoadExampleChild.setBezugsjahr(null);
		electricLoadExampleChild.setPrognoseHorizont(null);
		electricLoadExampleChild.setReferenz(electricLoadExample);
		final int idChild = StammdatenTestUtil.addStammdatum(electricLoadExampleChild);

		final Response childResponseOk = RESTCaller.callGetResponse(ServerTestUtils.URI + "stammdaten/" + idChild + "/excel");
		Assert.assertEquals(Status.OK.getStatusCode(), childResponseOk.getStatus());

		final Response clientResp = TestStammdatumImport.sendFile(new File("src/test/resources/templates/template.xlsx"), ServerTestUtils.URI + "stammdaten/excel");
		LOG.debug("clientResp: {}", clientResp.readEntity(String.class));
		Assert.assertEquals(Status.OK.getStatusCode(), clientResp.getStatus());
	}
}
