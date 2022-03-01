package de.unileipzig.irpsim.server.optimisation;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityTransaction;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import de.unileipzig.irpsim.core.data.timeseries.DatabaseConnectionHandler;
import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.TimeseriesTooShortException;
import de.unileipzig.irpsim.core.simulation.data.json.YearData;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.standingdata.DataLoader;
import de.unileipzig.irpsim.core.standingdata.data.AlgebraicData;
import de.unileipzig.irpsim.gams.GAMSHandler;
import de.unileipzig.irpsim.gams.GAMSModelParametrisationUtil;
import de.unileipzig.irpsim.server.algebraicdata.NotEvaluableException;

/**
 * Testet, dass beim Fehlschlag des Ladens von Parametern Fehlermeldungen mit den betroffenen Parametern ausgegeben werden. Da hier der LoadDataProvider (im Gegensatz zum
 * {@link GAMSModelParametrisationUtil}) nicht gemockt werden darf, wird dies in einer separaten Testklasse getan.
 * 
 * @author reichelt
 *
 */
public class GAMSModelParametrisationUtilFailureTest {

	@BeforeClass
	public static void initDb() {	   
		DatabaseConnectionHandler.getInstance().setUrl("jdbc:h2:mem:;MODE=MySQL");
		DataLoader.initializeTimeseriesTables();
	}

	@Test
	public void testShortArray() throws TimeseriesTooShortException {
		final GAMSHandler handler = Mockito.mock(GAMSHandler.class);

		final YearData yd = new YearData();
		yd.getConfig().setSimulationlength(35040);
		yd.getConfig().setSavelength(96);
		final BackendParametersYearData yeardata = new BackendParametersYearData(yd);

		final Timeseries timeseries_stunden = new Timeseries();

		final List<Number> data = new LinkedList<>();
		for (int i = 0; i < 35020; i++) {
			data.add(5d);
		}
		timeseries_stunden.setData(data);

		yeardata.getTimeseries().put("par_test", timeseries_stunden);

		final GAMSModelParametrisationUtil util = new GAMSModelParametrisationUtil(handler, yeardata, 0);
		String errorMessage = "";

		try {
			util.loadParameters();
			util.parameterizeModel();
		} catch (final TimeseriesTooShortException t) {
		   errorMessage = t.getMessage();
		}
		MatcherAssert.assertThat(errorMessage, Matchers.containsString("Zeitreihe für par_test ist 35020 lang, sollte aber 35040 lang sein."));
	}

	@Test
	public void testMissingStaticData() throws TimeseriesTooShortException {

		final GAMSHandler handler = Mockito.mock(GAMSHandler.class);

		final YearData yd = new YearData();
		yd.getConfig().setSimulationlength(35040);
		yd.getConfig().setSavelength(96);
		final BackendParametersYearData yeardata = new BackendParametersYearData(yd);

		final Timeseries timeseries_stunden = new Timeseries();
		timeseries_stunden.setSeriesname(15);

		yeardata.getTimeseries().put("par_test", timeseries_stunden);
		final GAMSModelParametrisationUtil util = new GAMSModelParametrisationUtil(handler, yeardata, 0);
		String errorMessage = "";

		try {
			util.loadParameters();
		} catch (final TimeseriesTooShortException t) {
		   errorMessage = t.getMessage();
		}
		MatcherAssert.assertThat(errorMessage, Matchers.containsString("15(par_test) ist zu kurz, Länge ist: 0, darf aber nur 35040, 8760, 672, 365, 52, 12 oder 1 (für Viertelstundenauflösung) oder 8760 oder 168 (für Stundenauflösung) sein."));
	}

	@Test
	public void testNonEvaluableAlgebraicData() throws TimeseriesTooShortException {
		int algebraicDataId;
		try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final EntityTransaction et = em.getTransaction();
			et.begin();

			final AlgebraicData ad = new AlgebraicData();
			ad.setEvaluable(false);

			em.persist(ad);

			et.commit();

			algebraicDataId = ad.getId();
		}

		final GAMSHandler handler = Mockito.mock(GAMSHandler.class);

		final YearData yd = new YearData();
		yd.getConfig().setSimulationlength(35040);
		yd.getConfig().setSavelength(96);
		final BackendParametersYearData yeardata = new BackendParametersYearData(yd);

		final Timeseries timeseries_stunden = new Timeseries();
		timeseries_stunden.setSeriesname(algebraicDataId);

		yeardata.getTimeseries().put("par_test", timeseries_stunden);
		final GAMSModelParametrisationUtil util = new GAMSModelParametrisationUtil(handler, yeardata, 0);
		String errorMessage = "";

		try {
			util.loadParameters();
		} catch (final NotEvaluableException n) {
			n.printStackTrace();
			errorMessage = n.getMessage();
		}
		MatcherAssert.assertThat(errorMessage, Matchers.containsString("Formel für par_test (" + algebraicDataId + ") konnte nicht ausgewertet werden."));
	}
}
