package de.unileipzig.irpsim.server.data.stammdaten;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityTransaction;
import javax.ws.rs.core.Response;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.unileipzig.irpsim.core.data.simulationparameters.TimeseriesExistsException;
import de.unileipzig.irpsim.core.data.timeseries.TimeseriesImportHandler;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.standingdata.data.AlgebraicData;
import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;
import de.unileipzig.irpsim.core.standingdata.data.StaticData;
import de.unileipzig.irpsim.core.standingdata.data.Variable;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;

public class TestStammdatumDelete extends ServerTests {

	private int id;
	private int idForeign;
	private int algebraicid;

	@Before
	public void cleanDBUp() throws JsonProcessingException, SQLException, TimeseriesExistsException {
		StammdatenTestUtil.cleanUp();
		StammdatenTestUtil.createPrognoseszenarien();

		id = StammdatenTestUtil.addStammdatum(StammdatenTestExamples.getElectricLoadExample());
		idForeign = StammdatenTestUtil.addStammdatum(StammdatenTestExamples.getThermalLoadExample());

		final StaticData datensatz;
		try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final Stammdatum sd = em.find(Stammdatum.class, id);
			final Stammdatum sdForeign = em.find(Stammdatum.class, idForeign);

			EntityTransaction et = em.getTransaction();
			et.begin();

			datensatz = new StaticData();
			datensatz.setStammdatum(sd);
			datensatz.setJahr(2016);
			em.persist(datensatz);

			et.commit();

			et = em.getTransaction();
			et.begin();

			final AlgebraicData algebraicData_SelfReference = new AlgebraicData();
			algebraicData_SelfReference.setStammdatum(sd);
			algebraicData_SelfReference.setJahr(2017);
			algebraicData_SelfReference.setFormel("result = A .* 3");
			final Map<String, Variable> variablenZuordnung = new HashMap<>();
			final Variable variableSelf = new Variable();
			variableSelf.setStammdatum(sd);
			variableSelf.setJahr(StammdatenTestExamples.getElectricLoadExample().getBezugsjahr());
			variablenZuordnung.put("A", variableSelf);
			algebraicData_SelfReference.setVariablenZuordnung(variablenZuordnung);
			em.persist(variableSelf);
			em.persist(algebraicData_SelfReference);

			et.commit();

			et = em.getTransaction();
			et.begin();

			final AlgebraicData algebraicData_foreignReference = new AlgebraicData();
			algebraicData_foreignReference.setStammdatum(sdForeign);
			algebraicData_foreignReference.setFormel("A*3");
			final Map<String, Variable> variablenZuordnungForeign = new HashMap<>();
			final Variable variableForeign = new Variable();
			variableForeign.setStammdatum(sd);
			variableForeign.setJahr(StammdatenTestExamples.getElectricLoadExample().getBezugsjahr());
			variablenZuordnungForeign.put("A", variableForeign);
			algebraicData_foreignReference.setVariablenZuordnung(variablenZuordnungForeign);
			em.persist(variableForeign);
			em.persist(algebraicData_foreignReference);

			et.commit();

			algebraicid = algebraicData_foreignReference.getId();
		}

		final TimeseriesImportHandler importHandler = new TimeseriesImportHandler(datensatz.getId());
		importHandler.executeImport(new DateTime(2015, 1, 1, 0, 0, 0), Arrays.asList(new Double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }), true);

	}

	@Test
	public void testDeletion() {
		final Response deleteWithAlgebraicReference = RESTCaller.callDeleteResponse(ServerTestUtils.URI + "stammdaten/" + id);
		Assert.assertEquals(deleteWithAlgebraicReference.getStatus(), Response.Status.BAD_REQUEST.getStatusCode());

		final Response deleteAlgebraic = RESTCaller.callDeleteResponse(ServerTestUtils.URI + "stammdaten/" + idForeign + "/algebraicdata/" + algebraicid);
		Assert.assertEquals(deleteAlgebraic.readEntity(String.class), deleteAlgebraic.getStatus(), Response.Status.OK.getStatusCode());

		final Response deleteSuccessfull = RESTCaller.callDeleteResponse(ServerTestUtils.URI + "stammdaten/" + id);
		Assert.assertEquals(deleteSuccessfull.readEntity(String.class), deleteSuccessfull.getStatus(), Response.Status.OK.getStatusCode());

	}
}
