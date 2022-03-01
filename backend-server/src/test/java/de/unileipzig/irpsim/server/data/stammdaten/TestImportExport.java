package de.unileipzig.irpsim.server.data.stammdaten;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityTransaction;
import javax.ws.rs.core.Response;

import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.data.simulationparameters.TimeseriesExistsException;
import de.unileipzig.irpsim.core.data.timeseries.DatabaseConnectionHandler;
import de.unileipzig.irpsim.core.data.timeseries.TimeseriesImportHandler;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.standingdata.DataLoader;
import de.unileipzig.irpsim.core.standingdata.data.AlgebraicData;
import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;
import de.unileipzig.irpsim.core.standingdata.data.StaticData;
import de.unileipzig.irpsim.server.standingdata.endpoints.AlgebraicDataEndpoint;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;

//TODO Dieser Test lief nie durch, da Algebraische Daten nie importiert wurden - beheben!
@Ignore
public class TestImportExport extends ServerTests {
	private static final ObjectMapper mapper = new ObjectMapper();

	private List<Integer> id;

	@Before
	public void cleanDBUp() throws SQLException, JsonProcessingException, TimeseriesExistsException {
		DataLoader.initializeTimeseriesTables();
		StammdatenTestUtil.cleanUp();
		StammdatenTestUtil.createPrognoseszenarien();

		id = new ArrayList<>();
		id.add(StammdatenTestUtil.addStammdatum(StammdatenTestExamples.getElectricLoadExample()));
		id.add(StammdatenTestUtil.addStammdatum(StammdatenTestExamples.getThermalLoadExample()));

		final Stammdatum sd;
		final StaticData datensatz;
		try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final EntityTransaction et = em.getTransaction();
			et.begin();
			sd = em.find(Stammdatum.class, id.get(0));
			datensatz = new StaticData();
			datensatz.setStammdatum(sd);
			datensatz.setJahr(2016);
			datensatz.setSzenario(1);
			em.persist(datensatz);

			et.commit();
		}

		final AlgebraicData algebraicData = new AlgebraicData();
		algebraicData.setStammdatum(sd);
		algebraicData.setJahr(2017);
		algebraicData.setFormel("1");

		final Response r = new AlgebraicDataEndpoint().addAlgebraicData(id.get(0), false, algebraicData);
		Assert.assertEquals(200, r.getStatus());

		final TimeseriesImportHandler importHandler = new TimeseriesImportHandler(datensatz.getId());
		importHandler.executeImport(new DateTime(2015, 1, 1, 0, 0, 0), Arrays.asList(new Double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }), true);
	}

	@Test
	public void testImportExport() throws SQLException, TimeseriesExistsException, IOException {
		final String importString = RESTCaller.callGet(ServerTestUtils.URI + "stammdaten/export?ids=" + id.get(0) + "&ids=" + id.get(1));

      try (final Connection connection = DatabaseConnectionHandler.getInstance().getConnection()) {
         Statement statement = connection.createStatement();
         statement.executeUpdate("TRUNCATE TABLE " + Stammdatum.class.getSimpleName());
         statement.executeUpdate("TRUNCATE TABLE Stammdatum_setElemente1");
         statement.executeUpdate("TRUNCATE TABLE Stammdatum_setElemente2");
         statement.executeUpdate("TRUNCATE TABLE Datensatz");
         statement.executeUpdate("TRUNCATE TABLE series_data_in");
      }

		RESTCaller.callPutResponse(ServerTestUtils.URI + "stammdaten/export", importString);

		final String electricEntity = RESTCaller.callGet(ServerTestUtils.URI + "stammdaten?all=true&typ=" + StammdatenTestExamples.getElectricLoadExample().getTyp());

		final Stammdatum[] electricLoadImported = mapper.readValue(electricEntity, Stammdatum[].class);
		final int eletrictLoadId = electricLoadImported[0].getId();
		electricLoadImported[0].setId(0);

		Assert.assertEquals(StammdatenTestExamples.getElectricLoadExample(), electricLoadImported[0]);

		final String electricDatensatzList = RESTCaller.callGet(ServerTestUtils.URI + "stammdaten/" + eletrictLoadId + "/data");
		final JSONArray ids = new JSONArray(electricDatensatzList);
		Assert.assertEquals(1, ids.length());

		final String algebraicList = RESTCaller.callGet(ServerTestUtils.URI + "stammdaten/" + eletrictLoadId + "/algebraicdata");
		final JSONArray idsAlgebraic = new JSONArray(algebraicList);
		Assert.assertEquals(20, idsAlgebraic.length());

		final JSONObject object = idsAlgebraic.getJSONObject(0);
		Assert.assertEquals("1", object.getString("formel"));
	}

}
