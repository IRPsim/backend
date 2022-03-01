package de.unileipzig.irpsim.core.standingdata;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import javax.persistence.Query;

import org.apache.commons.collections4.bag.TreeBag;
import org.apache.commons.lang3.ArrayUtils;
//import org.apache.commons.collections4.bag.TreeBag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.data.timeseries.DatabaseConnectionHandler;
import de.unileipzig.irpsim.core.data.timeseries.LoadElement;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.standingdata.data.AlgebraicData;
import de.unileipzig.irpsim.core.standingdata.data.Datensatz;
import de.unileipzig.irpsim.core.standingdata.data.StaticData;
import de.unileipzig.irpsim.server.algebraicdata.AlgebraicDataEvaluator;
import de.unileipzig.irpsim.server.algebraicdata.NotEvaluableException;

public class DataLoader {

   enum Type {
      IN_DATA, OUT_DATA, ALGEBRAIC_DATA;
   }

   private static final Logger LOG = LogManager.getLogger(DataLoader.class);

   final List<Integer> seriesids;
   final List<Integer> missingids = new LinkedList<>();
   final Map<Integer, List<LoadElement>> results = new HashMap<>();

   public DataLoader(final List<Integer> seriesids, final DateTime start, final DateTime end, final int maxcount) {
      this.seriesids = seriesids;
      loadDataWithDates(maxcount, start, end);
   }

   public DataLoader(final List<Integer> seriesids) {
      this.seriesids = seriesids;
      loadWithoutDates();
   }

   private void loadDataWithDates(final int maxcount, DateTime start, DateTime end) {
      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final Session session = (Session) em.getDelegate();

         for (final int seriesid : seriesids) {
            final Datensatz datensatz = session.get(Datensatz.class, seriesid);
            String table = null;
            if (seriesid == 0) {
               table = "series_data_in";
               start = start.year().setCopy(1970);
               end = end.year().setCopy(1970);
            } else {
               if (datensatz == null) {
                  missingids.add(seriesid);
                  continue;
               } else if (datensatz instanceof StaticData) {
                  if (((StaticData) datensatz).isInData()) {
                     table = "series_data_in";
                  } else {
                     table = "series_data_out";
                  }
                  start = start.year().setCopy(datensatz.getJahr());
                  end = end.year().setCopy(datensatz.getJahr());
                  LOG.debug("Datum: {} Jahr: {}", start, datensatz.getJahr());
               }
            }
            if (datensatz == null || datensatz instanceof StaticData) {
               // avoid SQL injection via seriesid
               final Query query = em.createNativeQuery("SELECT COUNT(*) FROM " + table + " WHERE seriesid=:seriesid and unixtimestamp between :start and :end");
               query.setParameter("seriesid", seriesid)
                     .setParameter("start", start.getMillis())
                     .setParameter("end", end.getMillis());

               final int count = ((Number) query.getSingleResult()).intValue();
               String groupBy;
               final String valueQuery;
               if (maxcount >= 1 && count > 1) {
                  final long stepsize = (end.getMillis() - start.getMillis()) * count / ((count - 1) * maxcount);
                  LOG.debug("Diff: {} Stepsize: {} Anzahl in DB: {}", end.getMillis() - start.getMillis(), stepsize, count);
                  groupBy = " group by ((unixtimestamp - " + start.getMillis() + ") div " + stepsize + ")  order by unixtimestamp";

                  valueQuery = "select avg(value) as value, min(value) as min, max(value) as max, count(value) as count, unixtimestamp "
                        + "from " + table + " where seriesid=" + seriesid + " and unixtimestamp between " + start.getMillis() + " and " + end.getMillis() + groupBy;
               } else {
                  groupBy = " order by unixtimestamp";
                  valueQuery = "select value, value as min, value as max, 1 as count, unixtimestamp "
                        + "from " + table + " where seriesid=" + seriesid + " and unixtimestamp between " + start.getMillis() + " and " + end.getMillis() + groupBy;
               }

               LOG.debug("Anfrage für AP {} Start: {} Ende: {} Maximalanzahl: {} ", seriesid, start.getMillis(), end.getMillis(), maxcount);

               session.doWork(connection -> loadData(valueQuery, seriesid, connection));
               // loadData(valueQuery, seriesid, em.unwrap(SessionImplementor.class).connection());
            } else if (datensatz instanceof AlgebraicData) {
               this.results.put(seriesid, evaluateFormula((AlgebraicData) datensatz, start, end, maxcount));
            }
         }

         for (final int seriesname : seriesids) {
            if (!results.containsKey(seriesname)) {
               results.put(seriesname, new LinkedList<>());
            }
         }
         LOG.info("Ergebnisse: {}", results.size());
      } catch (final Exception e) { // FIXME why this catch? Can't inform caller!
         e.printStackTrace();
      }
   }

   private List<LoadElement> evaluateFormula(final AlgebraicData datensatz, final DateTime start, final DateTime end, final int maxcount) {
      final long intervalLength = new Interval(start, end).toDurationMillis() / maxcount;
      DateTime currentDateTime = new DateTime(start.getYear(), 1, 1, 0, 0, start.getZone());
      final TreeBag<Double> currentInterval = new TreeBag<>();
      DateTime startOfInterval = start;
      long currentIndex = 0;

      final double[] values = new AlgebraicDataEvaluator().evaluateFormula(datensatz);
      final List<LoadElement> algRes = new ArrayList<>(values.length);

      System.out.println(values.length);
      for (int i = 0; i < values.length; i++) {
         currentDateTime = currentDateTime.plusMinutes(15);
         if (currentDateTime.getMonthOfYear() == 2 && currentDateTime.getDayOfMonth() == 29) {
            currentDateTime = currentDateTime.plusDays(1); // skip leap day
         }
         if (currentDateTime.equals(start) || currentDateTime.isAfter(start)) {
            final long intervalIndex = (currentDateTime.getMillis() - start.getMillis()) / intervalLength;
            if (intervalIndex > currentIndex) {
               if (currentIndex >= 0) {
                  final OptionalDouble avg = currentInterval.stream().mapToDouble(d -> d.doubleValue()).average();
                  algRes.add(new LoadElement(startOfInterval.getMillis(), currentInterval.first(), avg.getAsDouble(), currentInterval.last(), currentInterval.size()));
                  if (currentInterval.size() > 1) {
                     System.out.println(currentInterval.size());
                  }
               }
               startOfInterval = currentDateTime;
               currentInterval.clear();
               currentIndex = intervalIndex;
            }
            currentInterval.add(values[i]);
         }
         if (currentDateTime.isAfter(end)) {
            break;
         }
      }
      return algRes;
   }

   private void loadWithoutDates() {
      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final Session session = (Session) em.getDelegate();

         for (final int seriesid : seriesids) {
            final Type type = getType(session, seriesid);

            if (type == Type.IN_DATA || type == Type.OUT_DATA) {
               final String groupBy = " order by unixtimestamp";
               final String valueQuery = "select value, value as min, value as max, 1 as count, unixtimestamp "
                     + "from " + (type == Type.IN_DATA ? "series_data_in" : "series_data_out") + " where seriesid=" + seriesid + groupBy;

               LOG.debug("Anfrage für AP {} Maximalanzahl: {} ", seriesid);
               session.doWork(connection -> loadData(valueQuery, seriesid, connection));
            } else if (type == Type.ALGEBRAIC_DATA) {
               final AlgebraicData data = em.find(AlgebraicData.class, seriesid);
               final double[] values = new AlgebraicDataEvaluator().evaluateFormula(data);
               final List<LoadElement> algRes = new ArrayList<>(values.length);
               DateTime dt = new DateTime(data.getJahr(), 1, 1, 0, 0);
               for (int i = 0; i < values.length; i++) {
                  final double value = values[i];
                  dt = dt.plusMinutes(15);
                  if (dt.getMonthOfYear() == 2 && dt.getDayOfMonth() == 29) {
                     dt = dt.plusDays(1); // skip leap day
                  }
                  algRes.add(new LoadElement(dt.getMillis(), value, value, value, 1));
               }
               results.put(seriesid, algRes);
            }

         }

         for (final int seriesname : seriesids) {
            if (!results.containsKey(seriesname)) {
               results.put(seriesname, new LinkedList<>());
            }
         }
         LOG.info("Ergebnisse: {}", results.size());
      } catch (final Exception e) {
         e.printStackTrace();
      }
   }

   private Type getType(final Session session, final int seriesid) {
      final Type table;
      if (seriesid == 0) {
         table = Type.IN_DATA;
      } else {
         final StaticData datensatz = session.get(StaticData.class, seriesid);
         if (datensatz == null) {
            table = Type.ALGEBRAIC_DATA;
         } else {
            if (datensatz.isInData()) {
               table = Type.IN_DATA;
            } else {
               table = Type.OUT_DATA;
            }
         }
      }
      return table;
   }

   private void loadData(final String valueQuery, final int seriesid, final Connection con) throws SQLException {
      LOG.debug(valueQuery);

      try (final Statement st = con.createStatement(); final ResultSet rs = st.executeQuery(valueQuery)) {
         List<LoadElement> resultList = results.get(seriesid);
         if (resultList == null) {
            resultList = new LinkedList<>();
            results.put(seriesid, resultList);
         }
         while (rs.next()) {
            final LoadElement le = new LoadElement(rs.getLong("unixtimestamp"), rs.getDouble("min"), rs.getDouble("value"), rs.getDouble("max"), rs.getLong("count"));
            resultList.add(le);
         }
      }
   }

   public Map<Integer, List<LoadElement>> getResultData() {
      return results;
   }

   public List<Integer> getMissingids() {
      return missingids;
   }

   /**
    * Frage Zeitreihenwerte nach ihrem Namen ab und liefert diese zurück.
    *
    * @param timeseriesNames Die Namen der Zeitreihen
    * @return Die Namen abgebildet auf ihre Werte als Map<String, List<Number>>
    */
   public static Map<Integer, List<Number>> getTimeseries(final List<Integer> timeseriesNames, final boolean in) {
      if (timeseriesNames.size() == 0) {
         return new HashMap<>();
      }

      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final Map<Integer, List<Number>> result = new LinkedHashMap<>();
         final List<Integer> notEvaluable = new LinkedList<>();

         for (final Integer timeseriesName : timeseriesNames) {
            // LOG.debug("Lade {}", timeseriesName);
            if (Thread.currentThread().isInterrupted()) {
               return new HashMap<>();
            }

            if (Constants.ZERO_TIMESERIES_NAME == timeseriesName) {
               if (!result.containsKey(Constants.ZERO_TIMESERIES_NAME)) {
                  result.put(timeseriesName, Constants.ZERO_TIMESERIES_VALUES);
               }
            } else {
               final Datensatz datensatz = em.find(Datensatz.class, timeseriesName);
               if (datensatz instanceof AlgebraicData) {
                  try {
                     final AlgebraicDataEvaluator algebraicDataEvaluator = new AlgebraicDataEvaluator();
                     final double[] values = algebraicDataEvaluator.evaluateFormula((AlgebraicData) datensatz);
                     result.put(timeseriesName, Arrays.asList(ArrayUtils.toObject(values)));
                  } catch (final RuntimeException re) {
                     notEvaluable.add(timeseriesName);
                  }
               } else {
                  result.put(timeseriesName, new ArrayList<>());
                  final String table = in ? "series_data_in" : "series_data_out";
                  final String valueQuery = "select value from " + table + " where seriesid=" + timeseriesName + " order by unixtimestamp";
                  final Query query = em.createNativeQuery(valueQuery);
                  if (Thread.currentThread().isInterrupted()) {
                     return new HashMap<>();
                  }
                  final List<Object> resultList = query.getResultList();
                  LOG.trace("valueQuery liefert {} Ergebnisse.", resultList.size());
                  for (final Object element : resultList) {
                     final List<Number> values = result.get(timeseriesName);
                     values.add((Double) element);
                  }
               }
            }
         }

         if (notEvaluable.size() > 0) {
            throw new NotEvaluableException(notEvaluable);
         }

         LOG.trace("{} Länge: {}", result.get(timeseriesNames.get(0)), result.get(timeseriesNames.get(0)).size());
         return result;
      }
   }

   // public static Map<Integer, Number> getScalars(final List<Integer> nonZeroScalars) {
   // final Map<Integer, List<Number>> timeseries = getTimeseries(nonZeroScalars, true);
   // // each timeseries should have length 1, so convert the map to having only the first entry in each array as value
   // final Map<Integer, Number> res = new HashMap<>(timeseries.size());
   // for (final Integer id : timeseries.keySet()) {
   // final List<Number> values = timeseries.get(id);
   // if (values.size() != 1) {
   // LOG.warn("Skalarer Wert id={} aus der DB ist nicht skalar sondern eine Zeitreihe der Länge {}", id, values.size());
   // }
   // LOG.debug("Putting: " + id + " " + values.size());
   // res.put(id, values.get(0));
   // }
   // return res;
   // }

   public static void initializeTimeseriesTables() {
      try (final Connection connection = DatabaseConnectionHandler.getInstance().getConnection(); final Statement statement = connection.createStatement()) {

         final long startTime = System.nanoTime();
         final DatabaseMetaData meta = connection.getMetaData();

         final String values = getValues();

         try (final ResultSet resIn = meta.getTables(null, null, "series_data_in", new String[] { "TABLE" })) {
            if (!resIn.next()) {
               statement.executeUpdate("CREATE TABLE series_data_in(seriesid int, unixtimestamp bigint, value DOUBLE);");
               statement.executeUpdate("CREATE INDEX series_in_index ON series_data_in(seriesid)");
               statement.executeUpdate("CREATE INDEX series_in_index_timestamp ON series_data_in(seriesid, unixtimestamp)");
               statement.executeUpdate("INSERT INTO series_data_in VALUES " + values);
            }
         }

         try (final ResultSet resOut = meta.getTables(null, null, "series_data_out", new String[] { "TABLE" })) {
            if (!resOut.next()) {
               statement.executeUpdate("CREATE TABLE series_data_out(seriesid int, unixtimestamp bigint, value DOUBLE);");
               statement.executeUpdate("CREATE INDEX series_data_out_index ON series_data_out(seriesid)");
               statement.executeUpdate("CREATE INDEX series_data_out_index_timestamp ON series_data_out(seriesid, unixtimestamp)");
               statement.executeUpdate("INSERT INTO series_data_out VALUES " + values);
            }
         }
         LOG.debug("DB-Initialisierung: {} ms", (System.nanoTime() - startTime) / 10E6);
      } catch (final SQLException e) {
         e.printStackTrace();
      }
   }

   private static String getValues() {
      final StringBuilder builder = new StringBuilder();

      final String start = "(" + Constants.ZERO_TIMESERIES_NAME + ",";
      final String end = ",0),";
      for (long i = 0; i < 35040; i++) {
         final long timestamp = i * 15 * 60 * 1000;
         builder.append(start);
         builder.append(timestamp);
         builder.append(end);
      }
      String values = builder.toString();
      values = values.substring(0, values.length() - 1);
      return values;
   }
}
