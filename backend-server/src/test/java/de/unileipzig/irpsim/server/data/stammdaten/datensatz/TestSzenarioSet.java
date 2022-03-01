package de.unileipzig.irpsim.server.data.stammdaten.datensatz;

import java.io.IOException;
import java.util.LinkedList;

import javax.persistence.EntityTransaction;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.standingdata.SzenarioSet;
import de.unileipzig.irpsim.core.standingdata.SzenarioSetElement;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;

public class TestSzenarioSet extends ServerTests {

	@Before
	public void clearDatabase() {
		try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final EntityTransaction transaction = em.getTransaction();
			transaction.begin();
			em.createNativeQuery("TRUNCATE TABLE " + SzenarioSet.class.getSimpleName()).executeUpdate();
			em.createNativeQuery("TRUNCATE TABLE " + SzenarioSetElement.class.getSimpleName()).executeUpdate();
		}
	}

	@Test
	public void testPutAndGet() throws IOException {
		final SzenarioSet set = new SzenarioSet();
		set.setJahr(2016);
		final LinkedList<SzenarioSetElement> szenarien = new LinkedList<>();
		final SzenarioSetElement sse = new SzenarioSetElement();
		sse.setStelle(1);
		sse.setName("LEME A");
		set.setSzenarien(szenarien);
		szenarien.add(sse);
		set.setSzenarien(szenarien);
		// set.setSzenarienString(Arrays.asList(new String[] { "LEME A", "LEME B", "LEME C" }));

		final String uri = ServerTestUtils.URI + "szenariosets/";
		final String setJSON = new ObjectMapper().writeValueAsString(set);
		System.out.println(setJSON);
		final Response responsePut = RESTCaller.callPutResponse(uri, setJSON);
		Assert.assertEquals(Status.OK.getStatusCode(), responsePut.getStatus());

		final Response responseGet = RESTCaller.callGetResponse(ServerTestUtils.URI + "szenariosets/");
		Assert.assertEquals(Status.OK.getStatusCode(), responseGet.getStatus());

		final String response = responseGet.readEntity(String.class);
		final LinkedList<?> sets = new ObjectMapper().readValue(response, LinkedList.class);
		Assert.assertEquals(1, sets.size());
	}
}
