package de.unileipzig.irpsim.server.standingdata.endpoints;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.persistence.EntityTransaction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import de.unileipzig.irpsim.core.standingdata.data.Datensatz;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;
import de.unileipzig.irpsim.core.standingdata.data.StaticData;
import de.unileipzig.irpsim.server.standingdata.endpoints.utils.AlgebraicDataUpdater;

class UpdateStandardszenarioThread implements Runnable {

	private final List<Integer> szenarioStellen;

	public UpdateStandardszenarioThread(final List<Integer> szenarioStellen) {
		this.szenarioStellen = szenarioStellen;
	}

	@Override
	public void run() {

		try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final Session session = em.unwrap(Session.class);

			CriteriaBuilder builder = session.getCriteriaBuilder();
			CriteriaQuery<Stammdatum> criteria = builder.createQuery(Stammdatum.class);
			Root<Stammdatum> queryRoot = criteria.from(Stammdatum.class);

			Predicate likeRestrictions = builder.and(
					builder.equal(queryRoot.get("standardszenario"), true)
			);

			criteria.select(queryRoot).where(likeRestrictions);


			final List<Stammdatum> stammdaten = session.createQuery(criteria).list();

			final EntityTransaction et = em.getTransaction();
			et.begin();

			for (final int szenarioStelle : szenarioStellen) {
				for (final Stammdatum stammdatum : stammdaten) {
					// Datensatz könnte schon vorhanden sein, da alter Datensatz an Stelle vorhanden ist
					CriteriaQuery<StaticData> criteriaSD = builder.createQuery(StaticData.class);
					Root<StaticData> queryRootSD = criteriaSD.from(StaticData.class);

					Predicate likeRestrictionsSD = builder.and(
							builder.equal(queryRootSD.get("stammdatum"), stammdatum),
							builder.equal(queryRootSD.get("szenario"), szenarioStelle)
					);

					criteriaSD.select(queryRootSD).where(likeRestrictionsSD);
					final List<StaticData> staticData = session.createQuery(criteriaSD).list();
					if (staticData.size() == 0) {
						copyOldData(em, session, szenarioStelle, stammdatum);
					}
				}
			}

			et.commit();

			repairDatabase(session, em);
		}

	}

   private void copyOldData(final ClosableEntityManager em, final Session session, final int szenarioStelle, final Stammdatum stammdatum) {
		CriteriaBuilder builder = session.getCriteriaBuilder();
		CriteriaQuery<StaticData> criteria = builder.createQuery(StaticData.class);
		Root<StaticData> queryRoot = criteria.from(StaticData.class);

		Predicate likeRestrictions = builder.and(
				builder.equal(queryRoot.get("stammdatum"), stammdatum)
		);

		criteria.select(queryRoot).where(likeRestrictions);

      final List<StaticData> staticDataOld = session.createQuery(criteria).list();
      if (staticDataOld.size() > 0) {
      	SzenarioSetEndpoint.LOG.info("Kopiere: " + staticDataOld.get(0).getId());
      	final StaticData copiedData = new StaticData();
      	copiedData.setStammdatum(stammdatum);
      	copiedData.setInData(true);
      	copiedData.setJahr(staticDataOld.get(0).getJahr());
      	copiedData.setSzenario(szenarioStelle);
      	em.persist(copiedData);
      	session.doWork(connection -> {
      		connection.createStatement().executeUpdate("INSERT INTO series_data_in(seriesid, unixtimestamp, value) SELECT " + copiedData.getId()
      				+ ", unixtimestamp, value FROM series_data_in WHERE seriesid=" + staticDataOld.get(0).getId());
      	});
      }
   }
   
   /**
    * Repariert die Datensätze nach einem SzenarioSetupdate, indem 1. Die Vollständigkeitswerte verändert werden 2. Algebraische Datensätze hinzugefügt werden, falls notwendig
    * 
    * @param session
    * @param em
    */
   private void repairDatabase(final Session session, final ClosableEntityManager em) {
      final EntityTransaction transaction = session.beginTransaction();

      try (final Statement statement = session.doReturningWork(connection -> connection.createStatement())){
         statement.executeUpdate("UPDATE Stammdatum sd SET vollstaendig = "
               + "(SELECT COUNT(*) FROM Datensatz WHERE stammdatum_id = sd.id AND aktiv=true) * 100 / "
               + "((sd.prognoseHorizont+1) * (SELECT COUNT(*) FROM SzenarioSetElement WHERE set_jahr=sd.bezugsjahr))");
      } catch (HibernateException | SQLException e) {
         e.printStackTrace();
      }

      transaction.commit();

		CriteriaBuilder builder = session.getCriteriaBuilder();
		CriteriaQuery<Stammdatum> criteria = builder.createQuery(Stammdatum.class);
		Root<Stammdatum> queryRoot = criteria.from(Stammdatum.class);

		criteria.select(queryRoot);

      final List<Stammdatum> stammdaten = session.createQuery(criteria).list();
      for (final Stammdatum stammdatum : stammdaten) {
         new AlgebraicDataUpdater().adjustAlgebraicData(session, em, stammdatum);
      }
   }

}