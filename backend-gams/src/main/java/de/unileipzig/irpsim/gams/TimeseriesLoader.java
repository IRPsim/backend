package de.unileipzig.irpsim.gams;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.TimeInterval;
import de.unileipzig.irpsim.core.simulation.data.TimeseriesTooShortException;
import de.unileipzig.irpsim.core.standingdata.DataLoader;
import de.unileipzig.irpsim.core.standingdata.StammdatenUtil;
import de.unileipzig.irpsim.server.algebraicdata.NotEvaluableException;

public class TimeseriesLoader {

   private static final Logger LOG = LogManager.getLogger(TimeseriesLoader.class);

   private final BackendParametersYearData yeardata;
   final Map<Integer, String> timeseriesReferences;
   final Map<Integer, String> scalarReferences;
   final List<Integer> nonZeroTimeseries = new ArrayList<>();
   final List<Integer> nonZeroScalars = new ArrayList<>();

   public TimeseriesLoader(BackendParametersYearData yeardata) {
      this.yeardata = yeardata;

      timeseriesReferences = yeardata.collectTimeseriesReferences(true);
      scalarReferences = yeardata.collectScalarReferences();
      timeseriesReferences.keySet().stream().filter(r -> !r.equals(Constants.ZERO_TIMESERIES_NAME)).forEach(reference -> nonZeroTimeseries.add(reference));
      scalarReferences.keySet().stream().filter(r -> !r.equals(Constants.ZERO_TIMESERIES_NAME)).forEach(reference -> nonZeroScalars.add(reference));
   }

   public void load(Map<Integer, Timeseries> referencedTimeseries, Map<Integer, Number> referencedScalars) throws TimeseriesTooShortException {
      if (Thread.currentThread().isInterrupted()) {
         return;
      }

      try {
         loadTimeseries(referencedTimeseries);
         loadScalars(referencedScalars);
         referencedTimeseries.put(Constants.ZERO_TIMESERIES_NAME, Timeseries.ZEROTIMESERIES_DATA);
      } catch (final NotEvaluableException e) {
         for (final Integer id : e.getMissingIds()) {
            e.setParameterForId(id, timeseriesReferences.get(id));
         }
         throw e;
      }

      findTooShortTimeseries(referencedTimeseries);
   }

   private String scalarErrors = "";

   private void loadScalars(Map<Integer, Number> referencedScalars) {
      final Map<Integer, List<Number>> timeseries = DataLoader.getTimeseries(nonZeroScalars, true);
      // each timeseries should have length 1, so convert the map to having only the first entry in each array as value
      final Map<Integer, Number> res = new HashMap<>(timeseries.size());
      for (final Integer id : timeseries.keySet()) {
         final List<Number> values = timeseries.get(id);
         if (values.size() != 1) {
            scalarErrors += "Skalarer Wert id=" + id + " aus der DB ist nicht skalar sondern eine Zeitreihe der Länge " + values.size();
         } else {
            LOG.debug("Putting: " + id + " " + values.size());
            res.put(id, values.get(0));
         }
      }
      referencedScalars.putAll(res);
   }
   
   public String getScalarErrors() {
      return scalarErrors;
   }

   private void loadTimeseries(Map<Integer, Timeseries> referencedTimeseries) {
      final Map<Integer, List<Number>> referenceTimeseriesMap = DataLoader.getTimeseries(nonZeroTimeseries, true);
      referenceTimeseriesMap.forEach((reference, seriesdata) -> {
         LOG.trace("Lade {} Daten: {}", reference, seriesdata);
         if (Thread.currentThread().isInterrupted()) {
            return;
         }
         final Timeseries timeseries = Timeseries.build(seriesdata);
         timeseries.setSeriesname(reference);
         referencedTimeseries.put(reference, timeseries);
      });
   }

   private void findTooShortTimeseries(Map<Integer, Timeseries> referencedTimeseries) throws TimeseriesTooShortException {
      String errorMessage = "";
      for (final Integer referenceName : timeseriesReferences.keySet()) {
         final Timeseries timeseries = referencedTimeseries.get(referenceName);
         if (timeseries.size() != 672 &&
               timeseries.size() != 288 &&
               timeseries.size() != 168 &&
               timeseries.size() != yeardata.getConfig().getResolution()) {
            LOG.trace("Rollout {}", timeseries.getSeriesname());
            errorMessage = rolloutTimeseries(timeseriesReferences, errorMessage, timeseries);
         }
      }
      if (errorMessage.length() > 0) {
         throw new TimeseriesTooShortException(errorMessage);
      }
   }

   private String rolloutTimeseries(final Map<Integer, String> allReferences, String errorMessage, final Timeseries timeseries) {
      final TimeInterval concreteInterval = TimeInterval.getInterval(timeseries.getData().size());
      if (concreteInterval == null) {
         errorMessage += timeseries.getSeriesname() + "(" + allReferences.get(timeseries.getSeriesname()) + ") ist zu kurz, Länge ist: " + timeseries.size()
               + ", darf aber nur 35040, 8760, 672, 365, 52, 12 oder 1 (für Viertelstundenauflösung) oder 8760 oder 168 (für Stundenauflösung) sein.";
         return errorMessage;
      }
      final int resolution = yeardata.getConfig().getResolution();

      final List<Double> changedTimeseries = StammdatenUtil.rolloutTimeseries(concreteInterval, timeseries.getValues(), resolution);
      timeseries.setValues(changedTimeseries);
      return errorMessage;
   }
}
