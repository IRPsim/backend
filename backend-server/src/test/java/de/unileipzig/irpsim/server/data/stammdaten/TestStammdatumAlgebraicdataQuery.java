package de.unileipzig.irpsim.server.data.stammdaten;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.commons.collections4.map.HashedMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.standingdata.data.AlgebraicData;
import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;
import de.unileipzig.irpsim.core.standingdata.data.Variable;
import de.unileipzig.irpsim.server.utils.ServerTests;

/**
 * Prüft, ob der Query, um aus einem geänderten Stammdatum die betroffenen AlgebraicData-Instanzen zu schlussfolgern, funktioniert.
 * 
 * @author reichelt
 *
 */
public class TestStammdatumAlgebraicdataQuery extends ServerTests {

	private List<Integer> id;

	@Before
	public void cleanDBUp() throws JsonProcessingException {
		StammdatenTestUtil.cleanUp();
		StammdatenTestUtil.createPrognoseszenarien();

		id = new ArrayList<>();
		id.add(StammdatenTestUtil.addStammdatum(StammdatenTestExamples.getElectricLoadExample()));
		id.add(StammdatenTestUtil.addStammdatum(StammdatenTestExamples.getThermalLoadExample()));

		try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final EntityTransaction et = em.getTransaction();
			final Stammdatum sdA = em.find(Stammdatum.class, id.get(0));
			final Stammdatum sdB = em.find(Stammdatum.class, id.get(1));

			et.begin();
			initAlgebraicdata1(em, sdA);
			initAlgebraicdata2(em, sdA, sdB);
			et.commit();
		}
	}

	private void initAlgebraicdata1(final ClosableEntityManager em, final Stammdatum sdA) {
		final Variable A = new Variable();
		A.setJahr(2015);
		A.setStammdatum(sdA);
		final AlgebraicData data1 = new AlgebraicData();
		data1.setFormel("A * 2 + 3");
		final Map<String, Variable> variablenZuordnung1 = new HashedMap<>();

		variablenZuordnung1.put("A", A);
		data1.setVariablenZuordnung(variablenZuordnung1);

		data1.setStammdatum(sdA);
		data1.setJahr(2015);

		em.persist(A);
		em.persist(data1);

	}

	private void initAlgebraicdata2(final ClosableEntityManager em, final Stammdatum sdA, final Stammdatum sdB) {
		final Variable A = new Variable();
		A.setJahr(2015);
		A.setStammdatum(sdA);
		final Variable B = new Variable();
		B.setJahr(2015);
		B.setStammdatum(sdB);
		final AlgebraicData data2 = new AlgebraicData();
		data2.setFormel("A * 2 + B * 3");
		final Map<String, Variable> variablenZuordnung2 = new HashedMap<>();

		variablenZuordnung2.put("A", A);
		variablenZuordnung2.put("B", B);
		data2.setVariablenZuordnung(variablenZuordnung2);
		data2.setStammdatum(sdB);
		data2.setJahr(2015);

		em.persist(A);
		em.persist(B);
		em.persist(data2);
	}

	@Test
	public void testQuery() {
		try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final Query createQuery = em.createQuery("SELECT ad FROM AlgebraicData ad JOIN ad.variablenZuordnung var WHERE var.stammdatum.id = :id");

			createQuery.setParameter("id", id.get(0));
			final List<Object> data = createQuery.getResultList();
			Assert.assertEquals(2, data.size());
			final AlgebraicData ad = (AlgebraicData) data.get(0);
			Assert.assertEquals("A * 2 + 3", ad.getFormel());

			createQuery.setParameter("id", id.get(1));
			final List<Object> data2 = createQuery.getResultList();
			Assert.assertEquals(1, data2.size());
		}
	}
}
