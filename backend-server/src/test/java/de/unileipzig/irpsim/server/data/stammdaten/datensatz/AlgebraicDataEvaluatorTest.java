package de.unileipzig.irpsim.server.data.stammdaten.datensatz;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.standingdata.data.AlgebraicData;
import de.unileipzig.irpsim.core.standingdata.data.Datensatz;
import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;
import de.unileipzig.irpsim.core.standingdata.data.StaticData;
import de.unileipzig.irpsim.core.standingdata.data.Variable;
import de.unileipzig.irpsim.server.algebraicdata.AlgebraicDataEvaluator;

import org.hibernate.query.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

@PowerMockIgnore({"javax.management.*", "javax.script.*", "javax.xml.*", "com.sun.org.apache.xerces.*", "org.xml.*", "org.w3c.dom.*"})
@RunWith(PowerMockRunner.class)
@PrepareForTest({ ClosableEntityManagerProxy.class, Timeseries.class })
public class AlgebraicDataEvaluatorTest {

	private static final Logger LOG = LogManager.getLogger(AlgebraicDataEvaluatorTest.class);

	private final static Stammdatum stammdatum = new Stammdatum();

	private static final ClosableEntityManager manager = PowerMockito.mock(ClosableEntityManager.class);
	private static final Timeseries oneTimeseries = new Timeseries();

	static {
		stammdatum.setId(1);
		stammdatum.setTyp("asd");
		stammdatum.setBezugsjahr(2015);
		stammdatum.setName("asd");
	}

	@Before
	public void initMocks() {
		PowerMockito.mockStatic(ClosableEntityManagerProxy.class);
		Mockito.when(ClosableEntityManagerProxy.newInstance()).thenReturn(manager);

		PowerMockito.mockStatic(Timeseries.class);
		PowerMockito.when(Timeseries.build(1)).thenReturn(oneTimeseries);

		final Session session = Mockito.mock(Session.class);
		Mockito.when(manager.getDelegate()).thenReturn(session);
		final CriteriaBuilder builder = Mockito.mock(CriteriaBuilder.class);
		Mockito.when(session.getCriteriaBuilder()).thenReturn(builder);
		final CriteriaQuery criteria = Mockito.mock(CriteriaQuery.class);
		Mockito.when(builder.createQuery(Datensatz.class)).thenReturn(criteria);
		final Root<Datensatz> queryRoot = Mockito.mock(Root.class);
		Mockito.when(criteria.from(Datensatz.class)).thenReturn(queryRoot);
		Mockito.when(criteria.select(Mockito.any())).thenReturn(criteria);
		final Query<Datensatz> query = Mockito.mock(Query.class);
		Mockito.when(session.createQuery(criteria)).thenReturn(query);

		final StaticData ds = new StaticData();
		ds.setId(1);
		ds.setStammdatum(stammdatum);

		Mockito.when(query.uniqueResult()).thenReturn(ds);
	}

	public void mockDatabase(final List<Number> values) {
		oneTimeseries.setData(values);
	}

	@Test
	public void testEvaluation() {
		LOG.debug("Test 1");
		final List<Number> values = new LinkedList<>();
		for (int i = 0; i < 35040; i++) {
			values.add((double) i);
		}
		mockDatabase(values);

		final AlgebraicData data = new AlgebraicData();
		data.setFormel("result = A .* 2");
		final Variable v = new Variable();
		v.setJahr(2015);
		v.setStammdatum(stammdatum);
		data.getVariablenZuordnung().put("A", v);

		final AlgebraicDataEvaluator evaluator = new AlgebraicDataEvaluator();
		final double[] vals = evaluator.evaluateFormula(data);

		Assert.assertEquals(35040, vals.length);
	}

	@Test
	public void testEvaluationWithRollout() {
		LOG.debug("Test 2");
		final List<Number> values = new LinkedList<>();
		for (int i = 0; i < 8760; i++) {
			values.add((double) i);
		}
		mockDatabase(values);

		final AlgebraicData data = new AlgebraicData();
		data.setFormel("result = A .* 2");
		final Variable v = new Variable();
		v.setJahr(2015);
		v.setStammdatum(stammdatum);
		data.getVariablenZuordnung().put("A", v);

		final AlgebraicDataEvaluator evaluator = new AlgebraicDataEvaluator();
		final double[] vals = evaluator.evaluateFormula(data);

		Assert.assertEquals(35040, vals.length);
	}
}
