package de.unileipzig.irpsim.server.data.stammdaten;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import javax.persistence.EntityTransaction;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.simulation.data.json.IntermediarySimulationStatus;
import de.unileipzig.irpsim.core.simulation.data.json.UserDefinedDescription;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationYearPersistent;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;

public class TestSimpleState extends ServerTests {

	@Before
	public void cleanup() {
		StammdatenTestUtil.cleanUp();
	}

	@Test
	public void testYearState() throws JsonParseException, JsonMappingException, IOException {
		final OptimisationJobPersistent job = new OptimisationJobPersistent();
		final UserDefinedDescription udf = new UserDefinedDescription();
		job.setDescription(udf);
		try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final OptimisationYearPersistent year = new OptimisationYearPersistent();
			final OptimisationYearPersistent year2 = new OptimisationYearPersistent();
			year.setStart(new Date());
			year.setYear(2015);
			year2.setStart(new Date(100000));
			year2.setYear(2020);
			final EntityTransaction et = em.getTransaction();
			et.begin();
			em.persist(udf);
			year.setJob(job);
			year2.setJob(job);
			em.persist(year);
			em.persist(year2);
			job.setYears(Arrays.asList(new OptimisationYearPersistent[] { year, year2 }));
			em.persist(job);
			et.commit();
		}

		final String response = RESTCaller.callGet(ServerTestUtils.OPTIMISATION_URI + "/states");
		final IntermediarySimulationStatus[] iss = new ObjectMapper().readValue(response, IntermediarySimulationStatus[].class);
		System.out.println(response);
		final IntermediarySimulationStatus intermediarySimulationStatus = iss[0];
		Assert.assertNotNull(intermediarySimulationStatus.getYearStates());
		Assert.assertNotNull(intermediarySimulationStatus.getYearStates().get(0).getStart());
	}
}
