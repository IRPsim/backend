package de.unileipzig.irpsim.gams;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import javax.persistence.EntityTransaction;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gams.api.GAMSDatabase;
import com.gams.api.GAMSExecutionException;
import com.gams.api.GAMSSymbol;

import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersMultiModel;
import de.unileipzig.irpsim.core.simulation.data.TimeseriesTooShortException;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.standingdata.DataLoader;
import de.unileipzig.irpsim.core.standingdata.data.StaticData;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.core.utils.TestFiles;

/**
 * 
 * Testet die Parametrisierung eines Szenarios mit Referenzen, wobei das Erstellen der Referenzen selbst nicht geprüft wird.
 * 
 * @author reichelt
 */
public final class IRPoptReferencingTest {

	private static final Logger LOG = LogManager.getLogger(IRPoptReferencingTest.class);

	private static int datensatzId;

	@BeforeClass
	public static void initializeTimeseries() throws SQLException {
		DatabaseTestUtils.setupDbConnectionHandler();
		DataLoader.initializeTimeseriesTables();

		final StaticData staticData = new StaticData();
		staticData.setJahr(2015);

		try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final EntityTransaction et = em.getTransaction();
			et.begin();
			em.persist(staticData);
			em.createNativeQuery("INSERT INTO series_data_in(seriesid,unixtimestamp,value) VALUES (" + staticData.getId() + ",0,15)").executeUpdate();
			et.commit();
			datensatzId = staticData.getId();
		}
	}

	@Test
	public void testBasismodell() throws GAMSExecutionException, IOException {
		final File workspaceFolder = new File("src/main/resources/gams/IRPsim/");
		final GAMSHandler gamshandler = new GAMSHandler(workspaceFolder);
		try {
			ParametrisationUtil.parameterizeModel(gamshandler, TestFiles.DAYS_3.make());
		} catch (final TimeseriesTooShortException e) {
			e.printStackTrace();
		}

		gamshandler.startBlocking();

		final GAMSDatabase gdb = gamshandler.getResults();

		for (@SuppressWarnings("rawtypes")
		final GAMSSymbol gs : gdb) {
			LOG.trace("GS: " + gs.getName() + " " + gs.getText());
		}

		Assert.assertNotNull(gdb);
		gdb.dispose();
	}

	@Test
	public void testBasismodellZeroReferences() throws TimeseriesTooShortException, GAMSExecutionException, IOException {
		final File workspaceFolder = new File("src/main/resources/gams/IRPsim/");
		final GAMSHandler gamshandler = new GAMSHandler(workspaceFolder);
		try {
			final JSONParametersMultimodel jsonParametersMultimodel = new ObjectMapper().readValue(TestFiles.DAYS_3.make(), JSONParametersMultimodel.class);
			final BackendParametersMultiModel backendParametersMultiModel = new BackendParametersMultiModel(jsonParametersMultimodel);
			backendParametersMultiModel.getModels()[0].getYeardata()[0].getTimeseries().put("par_L_MS_E_resneg_provision", Timeseries.ZEROTIMESERIES_REFERENCE);
			backendParametersMultiModel.getModels()[0].getYeardata()[0].getScalars().put("sca_X_MS_DE_country", "" + datensatzId);

			final GAMSModelParametrisationUtil gamsModelParametrisationUtil = new GAMSModelParametrisationUtil(gamshandler, backendParametersMultiModel.getModels()[0].getYeardata()[0], 0);
			gamsModelParametrisationUtil.loadParameters();
			gamsModelParametrisationUtil.parameterizeModel();

			gamshandler.expose();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		gamshandler.startBlocking();

		final GAMSDatabase gdb = gamshandler.getResults();

		for (@SuppressWarnings("rawtypes")
		final GAMSSymbol gs : gdb) {
			LOG.trace("GS: " + gs.getName() + " " + gs.getText());
		}

		Assert.assertNotNull(gdb);
		gdb.dispose();
	}

	@AfterClass
	public static void cleanFolder() {
//		DatabaseConnectionHandler.getInstance().closeConnections();
		FileUtils.listFiles(new File("src/main/resources/gams/IRPsim/output/results"), new WildcardFileFilter("*.gdx"), TrueFileFilter.INSTANCE).stream().forEach(f -> f.delete());
		FileUtils.listFiles(new File("src/main/resources/gams/IRPsim/output/results"), new WildcardFileFilter("*.csv"), TrueFileFilter.INSTANCE).stream().forEach(f -> f.delete());
	}

}
