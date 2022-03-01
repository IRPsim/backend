package de.unileipzig.irpsim.server.data.simulationparameters;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.core.data.timeseries.TimeseriesImportHandler;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.Set;
import de.unileipzig.irpsim.core.simulation.data.SetElement;
import de.unileipzig.irpsim.core.standingdata.StaticDataUtil;
import de.unileipzig.irpsim.core.standingdata.data.Datensatz;

/**
 * Basisklasse für die gemeinsame Verwendung von ScenarioImportHandel's
 *
 * @author kluge
 */
public class ScenarioImportBase {

   private static final Logger LOG = LogManager.getLogger(ScenarioImportBase.class);

   protected final ExecutorService pool;
   private final int scenario;
   private final boolean isIn;

   public ScenarioImportBase(int scenario, boolean isIn) {
      this.scenario = scenario;
      this.isIn = isIn;
      pool = Executors.newFixedThreadPool(3);
   }

   /**
    * TODO
    *
    * @param yeardataArray Zeitreihe
    */
   void handleTableTimeseries(BackendParametersYearData[] yeardataArray) {
      for (final BackendParametersYearData yeardata : yeardataArray) {
         if (yeardata != null) {
            final int year = yeardata.getConfig().getYear();
            for (final Map.Entry<String, Map<String, Map<String, Timeseries>>> parameter : yeardata.getTableTimeseries().entrySet()) {
               for (final Map.Entry<String, Map<String, Timeseries>> firstDep : parameter.getValue().entrySet()) {
                  for (final Map.Entry<String, Timeseries> secondDep : firstDep.getValue().entrySet()) {
                     final List<Number> data = secondDep.getValue().getData();
                     if (data.size() != 0) {
                        final int referenceName = makeReference(data, year);
                        firstDep.getValue().put(secondDep.getKey(), Timeseries.build(referenceName));
                     }
                  }
               }
            }
         }
      }
   }

   /**
    * Erstellt den Referenznamen zu einem Zeitreihenparameter und führt den Import der Zeitreihe in die Datenbank aus mit {@link TimeseriesImportHandler}. Das imprtieren wird in
    * einem neuen Thread ausgeführt.
    *
    * @param values Zeitreihe
    * @param year Jahreszahl
    * @return Gibt die erzeugte Referenz zurück.
    */
   public final int makeReference(final List<Number> values, final int year) {
      final int referenceName;
      boolean currentTimeseriesIsZeroTimeseries = checkZeroTimeries(values);
      if (currentTimeseriesIsZeroTimeseries) {
         referenceName = Constants.ZERO_TIMESERIES_NAME;
      } else {
         final Datensatz newData = StaticDataUtil.importDatensatz(scenario, year, true);
         referenceName = newData.getId();

         pool.execute(() -> {
            try {
               final DateTime start = new DateTime(year, 1, 1, 0, 0);
               new TimeseriesImportHandler(referenceName).executeImport(start, values, isIn);
            } catch (final Exception e) {
               e.printStackTrace();
               LOG.error(e);
            }
         });
      }
      return referenceName;
   }

   private boolean checkZeroTimeries(final List<Number> values) {
      boolean currentTimeseriesIsZeroTimeseries = false;
      if (values.size() <= Constants.ZERO_TIMESERIES_VALUES.size()) {
         currentTimeseriesIsZeroTimeseries = true;
         for (final Number current : values) {
            if (current.doubleValue() != 0d) {
               currentTimeseriesIsZeroTimeseries = false;
               break;
            }
         }
      }
      return currentTimeseriesIsZeroTimeseries;
   }

   /**
    * Verwaltet die direkten Zeitreihen, erstellt das Benennungsprefix und führt den Import für alle Jahre mit Hilfe der handleGenericTimeseries Methode aus.
    *
    * @param yeardataArray Zeitreihe
    */
   public final void handleSimpleTimeseries(BackendParametersYearData[] yeardataArray) {
      for (final BackendParametersYearData yeardata : yeardataArray) {
         if (yeardata != null) {
            final int year = yeardata.getConfig().getYear();
            handleGenericTimeseries(yeardata.getTimeseries(), year);
         }
      }
   }

   /**
    * Verwaltet die Setzeitreihen, erstellt das Benennungsprefix und führt den Import für alle Sets mit Hilfe der handleGenericTimeseries Methode aus.
    *
    * @param yeardataArray Zeitreihe
    */
   public final void handleSetTimeseries(BackendParametersYearData[] yeardataArray) {
      for (final BackendParametersYearData yeardata : yeardataArray) {
         if (yeardata != null) {
            final int year = yeardata.getConfig().getYear();
            for (final Set set : yeardata.getSets().values()) {
               for (final SetElement element : set.getElements()) {
                  handleGenericTimeseries(element.getTimeseries(), year);
               }
            }
         }
      }
   }

   /**
    * Importiert eine Reihe von Zeitserien, die als direkte Map von Parametern abhängen.
    *
    * @param timeseriesMap die Zeitserien
    */
   public final void handleGenericTimeseries(final Map<String, Timeseries> timeseriesMap, final int year) {
      for (final Map.Entry<String, Timeseries> timeseries : timeseriesMap.entrySet()) {
         LOG.trace("Bearbeite Zeitreihe: {}", timeseries.getKey());
         final List<Number> data = timeseries.getValue().getData();
         if (data.size() != 0) {
            final int referenceName = makeReference(data, year);
            timeseries.setValue(Timeseries.build(referenceName));
         }
      }
   }
}
