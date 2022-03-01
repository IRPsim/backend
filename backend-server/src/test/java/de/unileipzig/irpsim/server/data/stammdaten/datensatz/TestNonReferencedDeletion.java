package de.unileipzig.irpsim.server.data.stammdaten.datensatz;

import java.sql.SQLException;
import java.util.Arrays;

import javax.persistence.EntityTransaction;

import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.data.simulationparameters.OptimisationScenario;
import de.unileipzig.irpsim.core.data.simulationparameters.TimeseriesExistsException;
import de.unileipzig.irpsim.core.data.timeseries.TimeseriesImportHandler;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParameters;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.json.YearData;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.core.standingdata.DataLoader;
import de.unileipzig.irpsim.core.standingdata.data.Datensatz;
import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;
import de.unileipzig.irpsim.core.standingdata.data.StaticData;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.server.endpoints.Cleaner;

/**
 * Prüft ob das Löschen ungenutzer Objekte beim Serverstart funktioniert.
 */
public class TestNonReferencedDeletion {
	private static final ObjectMapper mapper = new ObjectMapper();

	@Before
	public void cleanDBUp() throws SQLException {
	   DatabaseTestUtils.setupDbConnectionHandler();
		DataLoader.initializeTimeseriesTables();
		DatabaseTestUtils.cleanUp();
	}

	@Test
	public void testDeletion() throws SQLException, TimeseriesExistsException, JsonProcessingException {

		try (final ClosableEntityManager entitymanager = ClosableEntityManagerProxy.newInstance()) {
			importBasicData(entitymanager);
			importStammdatum(entitymanager);
			importJob(entitymanager);
			importScenario(entitymanager);
		}

		new Cleaner().deleteNonReferencedData();

		try (final ClosableEntityManager entitymanager = ClosableEntityManagerProxy.newInstance()) {
			Assert.assertNull(entitymanager.find(Datensatz.class, 1));
			Assert.assertNotNull(entitymanager.find(Datensatz.class, 2));
			Assert.assertNotNull(entitymanager.find(Datensatz.class, 3));
			Assert.assertNotNull(entitymanager.find(Datensatz.class, 4));
			Assert.assertNotNull(entitymanager.find(Datensatz.class, 5));
			Assert.assertNull(entitymanager.find(Datensatz.class, 6));
		}
	}

	private void importBasicData(final ClosableEntityManager entitymanager) throws SQLException, TimeseriesExistsException {
		for (int timeseriesname = 1; timeseriesname <= 6; timeseriesname++) {
			final StaticData datensatz = new StaticData();
			datensatz.setSzenario(1);
			datensatz.setInData(true);
			final EntityTransaction et = entitymanager.getTransaction();
			et.begin();
			entitymanager.persist(datensatz);
			et.commit();

			final TimeseriesImportHandler importHandler = new TimeseriesImportHandler(timeseriesname);
			importHandler.executeImport(new DateTime(2015, 1, 1, 0, 0, 0), Arrays.asList(new Double[] { 1.0, 2.0, 3.0, 4.0, 5.0, 6.0 }), true);
		}
	}

	private void importStammdatum(final ClosableEntityManager entitymanager) {
		final Stammdatum stammdatum = new Stammdatum();
		stammdatum.setBezugsjahr(2015);
		stammdatum.setName("Test");
		final Datensatz datensatz = entitymanager.find(Datensatz.class, 2);
		datensatz.setStammdatum(stammdatum);

		final EntityTransaction et = entitymanager.getTransaction();
		et.begin();
		entitymanager.persist(stammdatum);
		entitymanager.persist(datensatz);
		et.commit();
	}

	private void importJob(final ClosableEntityManager entitymanager) throws JsonProcessingException {
		final OptimisationJobPersistent job = new OptimisationJobPersistent();

		final YearData inputData = new YearData();
		inputData.getTimeseries().put("bla", "3");
		final JSONParametersMultimodel input = setMultimodelYearData(inputData);

		final YearData outputData = new YearData();
		outputData.getTimeseries().put("bla", "4");
		final JSONParametersMultimodel output = setMultimodelYearData(outputData);

		job.setJsonParameter(mapper.writeValueAsString(input));
		job.setJsonResult(mapper.writeValueAsString(output));

		final EntityTransaction jobTransaction = entitymanager.getTransaction();
		jobTransaction.begin();
		entitymanager.persist(job);
		jobTransaction.commit();
	}

   private JSONParametersMultimodel setMultimodelYearData(final YearData yearData) {
      final JSONParameters jsonParameters = new JSONParameters();
		jsonParameters.setYears(Arrays.asList(yearData));
		final JSONParametersMultimodel jsonParametersMultimodel = new JSONParametersMultimodel();
		jsonParametersMultimodel.setModels(Arrays.asList(jsonParameters));
		return jsonParametersMultimodel;
   }

	private void importScenario(final ClosableEntityManager entitymanager) throws JsonProcessingException {
		final OptimisationScenario scenario = new OptimisationScenario();

		final YearData inputData = new YearData();
		inputData.getTimeseries().put("bla", "5");
		final JSONParametersMultimodel input = setMultimodelYearData(inputData);

		scenario.setData(mapper.writeValueAsString(input));

		final EntityTransaction scenarioTransaction = entitymanager.getTransaction();
		scenarioTransaction.begin();
		entitymanager.persist(scenario);
		scenarioTransaction.commit();
	}
}
