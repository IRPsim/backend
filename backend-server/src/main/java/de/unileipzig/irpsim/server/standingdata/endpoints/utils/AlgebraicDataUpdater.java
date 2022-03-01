package de.unileipzig.irpsim.server.standingdata.endpoints.utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.persistence.EntityTransaction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.standingdata.SzenarioSet;
import de.unileipzig.irpsim.core.standingdata.data.AlgebraicData;
import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;
import de.unileipzig.irpsim.core.standingdata.data.Variable;
import de.unileipzig.irpsim.server.algebraicdata.AlgebraicDataEvaluator;

public class AlgebraicDataUpdater {

	private static final Logger LOG = LogManager.getLogger(AlgebraicDataUpdater.class);

	final AlgebraicDataEvaluator evaluator = new AlgebraicDataEvaluator();

	public List<Integer> insertAlgebraicdata(final AlgebraicData algebraicData, final ClosableEntityManager em, final Stammdatum stammdatum) {
		final List<Integer> ids = new java.util.LinkedList<>();
		for (int jahr = algebraicData.getJahr(); jahr <= stammdatum.getBezugsjahr() + stammdatum.getPrognoseHorizont(); jahr++) {
			final List<AlgebraicData> addedData = addAlgebraicDataForYear(algebraicData, em, stammdatum, jahr);
			addedData.stream().forEach(data -> ids.add(data.getId()));
		}

		return ids;
	}

	public void adjustAlgebraicData(final Session session, final ClosableEntityManager em, final Stammdatum stammdatum) {
		final List<AlgebraicData> algebraicDataList = session.createCriteria(AlgebraicData.class)
				.add(Restrictions.eq("stammdatum", stammdatum))
				.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
		if (algebraicDataList.size() > 0) {
			final AlgebraicData algebraicData = new AlgebraicData();
			algebraicData.setJahr(algebraicDataList.get(0).getJahr());
			algebraicData.setFormel(algebraicDataList.get(0).getFormel());
			algebraicData.setVariablenZuordnung(algebraicDataList.get(0).getVariablenZuordnung());
			updateAlgebraicdata(algebraicData, em, stammdatum, algebraicDataList);
		}
	}

	/**
	 * Aktualisiert den algebraischen Datensatz. Dabei können folgende Änderungen eingetreten sein: - Datensatz gilt eher oder später - Prognosehorizont hat sich verschoben, deshalb gilt der Datensatz
	 * länger oder kürzer - Innerhalb des Datensatz hat sich die Formel oder die Variablenzuordnung geändert
	 * 
	 * @param algebraicData
	 * @param em
	 * @param stammdatum
	 * @param oldAlgebraicDataList
	 * @return
	 */
	public List<Integer> updateAlgebraicdata(final AlgebraicData algebraicData, final ClosableEntityManager em, final Stammdatum stammdatum, final List<AlgebraicData> oldAlgebraicDataList) {
		final AlgebraicData minData = oldAlgebraicDataList.stream()
				.min((first, second) -> first.getJahr() - second.getJahr()).get();
		final AlgebraicData maxData = oldAlgebraicDataList.stream()
				.max((first, second) -> first.getJahr() - second.getJahr()).get();

		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		if (minData.getJahr() < algebraicData.getJahr()) {
			final CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
			final CriteriaDelete<AlgebraicData> delete = criteriaBuilder.createCriteriaDelete(AlgebraicData.class);
			final Root<AlgebraicData> root = delete.from(AlgebraicData.class);
			delete.where(criteriaBuilder.and(criteriaBuilder.lessThan(root.get("jahr"), algebraicData.getJahr()),
					criteriaBuilder.equal(root.get("stammdatum"), stammdatum)));

			em.createQuery(delete).executeUpdate();

			oldAlgebraicDataList.removeIf(data -> data.getJahr() < algebraicData.getJahr());
		}

		if (maxData.getJahr() > stammdatum.getBezugsjahr() + stammdatum.getPrognoseHorizont()) {
			final CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
			final CriteriaDelete<AlgebraicData> delete = criteriaBuilder.createCriteriaDelete(AlgebraicData.class);
			final Root<AlgebraicData> root = delete.from(AlgebraicData.class);
			delete.where(criteriaBuilder.and(
					criteriaBuilder.greaterThan(root.get("jahr"),
							stammdatum.getBezugsjahr() + stammdatum.getPrognoseHorizont()),
					criteriaBuilder.equal(root.get("stammdatum"), stammdatum)));

			em.createQuery(delete).executeUpdate();

			oldAlgebraicDataList
					.removeIf(data -> data.getJahr() > stammdatum.getBezugsjahr() + stammdatum.getPrognoseHorizont());
		}
		transaction.commit();

		final SzenarioSet szenarien = em.find(SzenarioSet.class, stammdatum.getBezugsjahr());
		for (int jahr = algebraicData.getJahr(); jahr <= stammdatum.getBezugsjahr() + stammdatum.getPrognoseHorizont(); jahr++) {
			final int jahr2 = jahr;
			for (final int szenarioindex : szenarien.getIds()) {
				final List<AlgebraicData> filteredData = oldAlgebraicDataList.stream()
						.filter(data -> data.getSzenario() == szenarioindex && data.getJahr() == jahr2)
						.collect(Collectors.toList());
				AlgebraicData data;
				if (filteredData.size() == 0) {
					final EntityTransaction et = em.getTransaction();
					et.begin();
					final AlgebraicData newData = new AlgebraicData();
					newData.setFormel(algebraicData.getFormel());
					newData.setVariablenZuordnung(new HashMap<>());
					newData.setJahr(jahr);
					newData.setSzenario(szenarioindex);
					newData.setStammdatum(stammdatum);
					for (final Entry<String, Variable> variable : algebraicData.getVariablenZuordnung().entrySet()) {
						final Variable copy = new Variable();
						final Stammdatum referencedStammdatum = variable.getValue().getStammdatum();
						copy.setStammdatum(referencedStammdatum);
						copy.setJahr(variable.getValue().getJahr());
						LOG.debug("Stammdatum: {} Id: {}", referencedStammdatum, referencedStammdatum.getId());
						em.persist(copy);
						newData.getVariablenZuordnung().put(variable.getKey(), copy);
					}
					final String validationMessage = evaluator.validateDependencies(newData);
					if (validationMessage != null) {
						LOG.warn("Formel für Stammdatum {}, Jahr {} und Szenario {} nicht evaluierbar, Grund: {}", stammdatum.getId(), jahr, szenarioindex, validationMessage);
						newData.setEvaluable(false);
					}
					em.persist(newData);
					et.commit();
				} else {
					data = filteredData.get(0);
					data.setFormel(algebraicData.getFormel());
					for (final Variable variable : data.getVariablenZuordnung().values()) {
						em.remove(variable);
					}
					data.setVariablenZuordnung(new HashMap<>());
					copyData(algebraicData, em, data);
					final String validationMessage = evaluator.validateDependencies(data);
					if (validationMessage != null) {
						LOG.warn("Formel für Stammdatum {}, Jahr {} und Szenario {} nicht evaluierbar, Grund: {}", stammdatum.getId(), jahr, szenarioindex, validationMessage);
						data.setEvaluable(false);
					} else {
						data.setEvaluable(true);
					}
					transaction = em.getTransaction();
					transaction.begin();
					em.persist(data);
					transaction.commit();
				}
			}
		}

		return oldAlgebraicDataList.stream().mapToInt(data -> data.getId()).boxed().collect(Collectors.toList());
	}

	private List<AlgebraicData> addAlgebraicDataForYear(final AlgebraicData algebraicData, final ClosableEntityManager em, final Stammdatum stammdatum, final int jahr) {
		final SzenarioSet szenarien = em.find(SzenarioSet.class, stammdatum.getBezugsjahr());
		final List<AlgebraicData> addedData = new LinkedList<>();
		final EntityTransaction et = em.getTransaction();
		et.begin();
		for (final int szenario : szenarien.getIds()) {
			final AlgebraicData newData = new AlgebraicData();
			newData.setFormel(algebraicData.getFormel());
			newData.setVariablenZuordnung(new HashMap<>());
			newData.setJahr(jahr);
			newData.setSzenario(szenario);
			newData.setStammdatum(stammdatum);
			copyData(algebraicData, em, newData);
			final String validationMessage = evaluator.validateDependencies(newData);
			if (validationMessage != null) {
				LOG.warn("Formel für Stammdatum {}, Jahr {} und Szenario {} nicht evaluierbar, Grund: {}", stammdatum.getId(), jahr, szenario, validationMessage);
				newData.setEvaluable(false);
			}

			em.persist(newData);
			addedData.add(newData);
		}
		et.commit();
		return addedData;
	}

	public void copyData(final AlgebraicData algebraicData, final ClosableEntityManager em, final AlgebraicData newData) {
		for (final Entry<String, Variable> variable : algebraicData.getVariablenZuordnung().entrySet()) {
			final Variable copy = new Variable();
			final Stammdatum referencedStammdatum = em.find(Stammdatum.class, variable.getValue().getStammdatum().getId());
			copy.setStammdatum(referencedStammdatum);
			copy.setJahr(variable.getValue().getJahr());
			LOG.debug("Stammdatum: {} Id: {}", referencedStammdatum, referencedStammdatum.getId());
			em.persist(copy);
			newData.getVariablenZuordnung().put(variable.getKey(), copy);
		}
	}
}
