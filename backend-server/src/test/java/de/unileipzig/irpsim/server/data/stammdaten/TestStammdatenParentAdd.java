package de.unileipzig.irpsim.server.data.stammdaten;

import java.io.File;
import java.io.FileNotFoundException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;

public class TestStammdatenParentAdd extends ServerTests {

   private static final Logger LOG = LogManager.getLogger();

   @Before
	public void cleanDBUp() {
		StammdatenTestUtil.cleanUp();
		StammdatenTestUtil.createPrognoseszenarien();
	}

	@Test
	public void testAddAndGetWithoutId() throws FileNotFoundException, JsonProcessingException {
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

		final Response childResponse = RESTCaller.callGetResponse(ServerTestUtils.URI + "stammdaten/" + idChild + "/excel");
		LOG.debug("response: {}", childResponse.readEntity(String.class));
		Assert.assertEquals(Status.OK.getStatusCode(), childResponse.getStatus());

		final Response clientResp = TestStammdatumImport.sendFile(new File("src/test/resources/templates/template.xlsx"), ServerTestUtils.URI + "stammdaten/excel");
		LOG.debug("clientResp: {}", clientResp.readEntity(String.class));
		Assert.assertEquals(Status.OK.getStatusCode(), clientResp.getStatus());
		//
		// try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
		// final Query createNativeQuery = em.createNativeQuery("SELECT COUNT(*) FROM series_data_in series, Datensatz d WHERE series.seriesid=d.id AND d.stammdatum_id=" + id.get(0));
		// final BigInteger count = (BigInteger) createNativeQuery.getResultList().get(0);
		// Assert.assertEquals(35136, count.intValue());
		// }
	}
}
