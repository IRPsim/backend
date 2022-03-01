package de.unileipzig.irpsim.core.standingdata;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityTransaction;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;

import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.standingdata.data.AlgebraicData;
import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;
import de.unileipzig.irpsim.core.standingdata.data.StaticData;
import de.unileipzig.irpsim.server.algebraicdata.AlgebraicDataEvaluator;

public class StaticDataUtil {

   private static final Logger LOG = LogManager.getLogger(StaticDataUtil.class);

   public static StaticData importDataNoTransaction(final AddData addData, final ClosableEntityManager em, final Stammdatum stammdatum) {
      LOG.info("Importing static data");
      final StaticData newData = new StaticData();
      final List<StaticData> data = getOldData(addData, em, stammdatum);

      if (data.size() > 0) {
         final StaticData oldData = data.get(0);

         em.createNativeQuery("DELETE FROM series_data_in WHERE seriesid=" + oldData.getId()).executeUpdate();
         importSeriesdata(oldData.getId(), em, addData.getValues());
         return oldData;
      } else {

         newData.setStammdatum(stammdatum);
         newData.setSzenario(addData.getSzenario());
         newData.setJahr(addData.getJahr());
         em.persist(newData);

         importSeriesdata(newData.getId(), em, addData.getValues());

         return newData;
      }
   }

   public static StaticData importData(final AddData addData, final ClosableEntityManager em, final Stammdatum stammdatum) {
      LOG.info("Importing static data");
      final StaticData newData = new StaticData();
      final List<StaticData> data = getOldData(addData, em, stammdatum);

      if (data.size() > 0) {
         final StaticData oldData = data.get(0);

         final EntityTransaction et = em.getTransaction();
         et.begin();
         em.createNativeQuery("DELETE FROM series_data_in WHERE seriesid=" + oldData.getId()).executeUpdate();
         importSeriesdata(oldData.getId(), em, addData.getValues());
         et.commit();
         return oldData;
      } else {
         final EntityTransaction et = em.getTransaction();
         et.begin();

         newData.setStammdatum(stammdatum);
         newData.setSzenario(addData.getSzenario());
         newData.setJahr(addData.getJahr());
         em.persist(newData);

         importSeriesdata(newData.getId(), em, addData.getValues());

         et.commit();
         return newData;
      }
   }

   private static List<StaticData> getOldData(final AddData addData, final ClosableEntityManager em, final Stammdatum stammdatum) {

      final CriteriaBuilder cBuilder = em.getCriteriaBuilder();
      final CriteriaQuery<StaticData> staticDataQuery = cBuilder.createQuery(StaticData.class);
      final Root<StaticData> staticDataRoot = staticDataQuery.from(StaticData.class);

      final List<Predicate> restrictionList = addRestrictions(addData, stammdatum, cBuilder, staticDataRoot);

      Predicate[] restrictions = new Predicate[restrictionList.size()];
      restrictions = restrictionList.toArray(restrictions);      ;

      return em.createQuery(staticDataQuery.where(restrictions)).getResultList();
   }

   private static List<Predicate> addRestrictions(final AddData addData, final Stammdatum stammdatum, final CriteriaBuilder cBuilder, final Root<StaticData> staticDataRoot) {

      final List<Predicate> predicateList = new ArrayList<Predicate>();
      predicateList.add(cBuilder.equal(staticDataRoot.get("jahr"), addData.getJahr()));

      if (stammdatum != null) {
         predicateList.add(cBuilder.equal(staticDataRoot.get("stammdatum"), stammdatum));
      }

      if (addData.getSzenario() == 0) {
         predicateList.add(cBuilder.isNull(staticDataRoot.get("szenario")));
      }
      else {
         predicateList.add(cBuilder.equal(staticDataRoot.get("szenario"), addData.getSzenario()));
      }
      return predicateList;
   }

   public static StaticData importDatensatz(final int szenario, final int jahr, final boolean isIn) {
      final StaticData newData = new StaticData();
      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final EntityTransaction et = em.getTransaction();
         et.begin();

         newData.setStammdatum(null);
         newData.setSzenario(szenario);
         newData.setJahr(jahr);
         newData.setInData(isIn);

         em.persist(newData);

         et.commit();
      }
      return newData;
   }

   static class EvaluabilityUpdater {

      void updateEvalueability(final ClosableEntityManager em, final Stammdatum stammdatum) {

         final AlgebraicDataEvaluator evaluator = new AlgebraicDataEvaluator();

         for (final AlgebraicData result : getResults(em, stammdatum)) {
            final String validationMessage = evaluator.validateDependencies(result);
            final boolean evaluable = validationMessage == null;
            if (evaluable != result.isEvaluable()) {
               result.setEvaluable(evaluable);
               /**
                * TODO Hier würde es reichen, für den jeweiligen algebraischen Datensatz diejenigen Datensätze auszuwerten, die sich genau auf den Datensatz (im richtigen Jahr und
                * Szenario) beziehen. Würde allerdings Implementierungsaufwand erfordern, deshalb wird hier der Einfachheit halber das gesamte Stammdatum neu ausgewertet (da dann
                * nur weitergemacht wird, wenn die Evaluierbarkeit wirklich ungleich der ursprünglichen Evaluierbarkeit ist, sollte der Aufwand nur um einen konstanten Faktor (bis
                * zum Prognosehorizont) wachsen.
                */
               updateEvalueability(em, result.getStammdatum());
            }
         }
      }

      private List<AlgebraicData> getResults(final ClosableEntityManager em, final Stammdatum stammdatum) {
         final String queryString = "SELECT ad FROM AlgebraicData ad JOIN ad.variablenZuordnung zuordnung JOIN zuordnung.stammdatum sd WHERE sd = :sd AND ad.evaluable = false";
         final TypedQuery<AlgebraicData> adQuery = em.createQuery(queryString, AlgebraicData.class);
         final List<AlgebraicData> results = adQuery.setParameter("sd", stammdatum).getResultList();
         return results;
      }
   }

   public static void updateCompleteness(final ClosableEntityManager em, final Stammdatum stammdatum) {
      final Session session = em.unwrap(Session.class);
      final EvaluabilityUpdater updater = new EvaluabilityUpdater();
      updater.updateEvalueability(em, stammdatum);

      final Query createNativeQuery = em.createNativeQuery(
            "SELECT COUNT(*) FROM Datensatz d WHERE stammdatum_id=:id AND aktiv=true AND (evaluable = true OR evaluable IS NULL)")
            .setParameter("id", stammdatum.getId());
      final SzenarioSet set = em.find(SzenarioSet.class, stammdatum.getBezugsjahr());

      final double countComplete = ((BigInteger) createNativeQuery.getSingleResult()).doubleValue();
      final double maxDatensatzCount = set.getSzenarien().size() * (stammdatum.getPrognoseHorizont() + 1);
      final double vollstaendig = 100d * countComplete / maxDatensatzCount;
      LOG.info("Vollständig nach Upload: {} Datensätze: {} Für Vollständigkeit notwendige Datensätze: {}",
            vollstaendig, countComplete, maxDatensatzCount);

      final EntityTransaction et = em.getTransaction();
      et.begin();
      stammdatum.setVollstaendig(vollstaendig);
      session.update(stammdatum);
      et.commit();
   }

   private static void importSeriesdata(final int seriesid, final ClosableEntityManager em,
         final TimeseriesValue[] timeseriesValues) {
      Session session = em.unwrap(Session.class);
      session.doWork(new Work() {
         @Override
         public void execute(final Connection connection) throws SQLException {

            try (final PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO series_data_in(seriesid, unixtimestamp, value) VALUES (?,?,?)")) {
               for (final TimeseriesValue value : timeseriesValues) {
                  preparedStatement.setInt(1, seriesid);
                  preparedStatement.setLong(2, value.getUnixtimestamp());
                  preparedStatement.setDouble(3, value.getValue());
                  preparedStatement.executeUpdate();
               }
            }
         }
      });
      session.flush();
   }

   public static Stammdatum findEqualStammdatum(final Stammdatum stammdatum, final ClosableEntityManager em) {

      final CriteriaBuilder cBuilder = em.getCriteriaBuilder();
      final CriteriaQuery<Stammdatum> stammdatumQuery = cBuilder.createQuery(Stammdatum.class);
      final Root<Stammdatum> stammdatumRoot = stammdatumQuery.from(Stammdatum.class);

      final Predicate[] restrictions = getRestrictions(stammdatum, cBuilder, stammdatumRoot);
      final List<Stammdatum> candidates = em.createQuery(stammdatumQuery.where(restrictions)).getResultList();

      Stammdatum equal = null;
      for (final Stammdatum candidate : candidates) {
         StaticDataUtil.fillStammdatum(candidate);
         // TODO beim Vergleich mit NULL-Werten umgehen
         if (equalStringAllowBothNull(candidate.getName(), stammdatum.getName())
               && equalStringAllowBothNull(candidate.getTyp(), stammdatum.getTyp())
               && candidate.getBezugsjahr().intValue() == stammdatum.getBezugsjahr().intValue()) {
            equal = candidate;
            break;
         }
      }
      return equal;
   }

   private static Predicate[] getRestrictions(final Stammdatum stammdatum, final CriteriaBuilder cBuilder, final Root<Stammdatum> stammdatumRoot) {
      final Predicate equalName = cBuilder.equal(stammdatumRoot.get("name"), stammdatum.getName());
      final Predicate equalTyp = cBuilder.equal(stammdatumRoot.get("typ"), stammdatum.getTyp());
      final Predicate equalBezugsjahr = cBuilder.equal(stammdatumRoot.get("bezugsjahr"), stammdatum.getBezugsjahr());

      final Predicate equalNameOrTypOrBezugsjahr = cBuilder.or(equalName, equalTyp, equalBezugsjahr);
      final Predicate notEqualId = cBuilder.notEqual(stammdatumRoot.get("id"), stammdatum.getId());

      final Predicate[] restrictions = new Predicate[] {
            equalNameOrTypOrBezugsjahr,
            notEqualId
      };
      return restrictions;
   }

   public static boolean equalStringAllowBothNull(final String first, final String second) {
      return (first == null ? second == null : first.equals(second));
   }

   /**
    * Befüllt das Stammdatum mit den Werten der Elternelemente, falls die Werte null sind. Gibt, falls ein Wert endgültig nicht
    * 
    * @param stammdatum Stammdatum, das befüllt werden soll
    * @return Die Fehlermeldung, falls ein Fehler auftritt
    */
   public static String fillStammdatum(final Stammdatum stammdatum) {
      boolean goToFather = true;
      Stammdatum current = stammdatum;
      while (goToFather) {
         final Stammdatum father = current.getReferenz();
         LOG.info("Befülle mit: {}", (father != null ? father.getId() : "nichts"));
         boolean hasNull = false;
         for (final StammdatumEntity entity : StammdatumEntity.values()) {
            final String oldValue = entity.getValue(stammdatum);
            LOG.debug("Prüfe: {} Wert: {} Ist null: {}", entity.getName(), oldValue, oldValue == null);
            if (oldValue == null) {
               LOG.debug("null-Wert: {}", entity.getName());
               if (father != null) {
                  if (entity.getValue(father) == null) {
                     hasNull = true;
                  } else {
                     LOG.info("Ersetze Wert: {} Von Id: {}", entity.getValue(father), father.getId());
                     entity.setValue(stammdatum, entity.getValue(father));
                  }
               } else {
                  return "Wert " + entity.getName() + " für " + stammdatum.getId() + " nicht definiert.";
               }
            }
         }
         current = father;
         goToFather = hasNull;
      }
      return "";
   }
}
