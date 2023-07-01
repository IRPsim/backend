package de.unileipzig.irpsim.core.standingdata;

import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import de.unileipzig.irpsim.core.simulation.data.TimeInterval;

public class StammdatenUtil {

   private static final Logger LOG = LogManager.getLogger();

   private static final int WEEK_LENGTH = 7;
   private static final int HOUR_LENGTH = 4;
   private static final int DAY_LENGTH = 24;

   public static List<Double> rolloutTimeseries(final TimeInterval concreteInterval, final List<Double> values, final int resolution) {
      if (resolution == 35040) {
         return rolloutTimeseriesTo35040(concreteInterval, values);
      } else if (resolution == 8760) {
         return rolloutTimeseriesTo8760(concreteInterval, values);
      } else {
         throw new RuntimeException("Can only rollout to 35040 or 8760, rollout to " + resolution + " not possible.");
      }
   }

   public static List<Double> rolloutTimeseriesTo35040(final TimeInterval concreteInterval, final List<Double> currentValues) {
      final List<Double> changedTimeseries = new LinkedList<>();
      switch (concreteInterval) {
      case QUARTERHOUR:
         changedTimeseries.addAll(currentValues);
         break;
      case HOUR:
         for (int hour = 0; hour < 24 * 365; hour++) {
            for (int quarterhour = 0; quarterhour < HOUR_LENGTH; quarterhour++) {
               changedTimeseries.add(currentValues.get(hour));
            }
         }
         break;
      case DAY:
         for (int day = 0; day < 365; day++) {
            for (int quarterhour = 0; quarterhour < HOUR_LENGTH * DAY_LENGTH; quarterhour++) {
               changedTimeseries.add(currentValues.get(day));
            }
         }
         break;
      case MONTH:
         for (int month = 0; month < 12; month++) {
            final DateTime copy = new DateTime().year().setCopy(2015); // zufälliges Nicht-Schaltjahr
            final DateTime copy2 = copy.monthOfYear().setCopy(month + 1);
            final int days = copy2.dayOfMonth().getMaximumValue();
            for (int quarterhour = 0; quarterhour < HOUR_LENGTH * DAY_LENGTH * days; quarterhour++) {
               changedTimeseries.add(currentValues.get(month));
            }
         }
         break;
      case WEEK:
         for (int week = 0; week < 52; week++) {
            for (int quarterhour = 0; quarterhour < HOUR_LENGTH * WEEK_LENGTH * DAY_LENGTH; quarterhour++) {
               changedTimeseries.add(currentValues.get(week));
            }
         }
         // Jahr hat 365 Tage, 7 * 52 = 364
         for (int quarterhour = 0; quarterhour < HOUR_LENGTH * DAY_LENGTH; quarterhour++) {
            changedTimeseries.add(currentValues.get(51));
         }
         LOG.debug("changedTimeseries.size: {}", changedTimeseries.size());
         break;
      case YEAR:
         for (int quarterhour = 0; quarterhour < 35040; quarterhour++) {
            changedTimeseries.add(currentValues.get(0));
         }
      }
      return changedTimeseries;
   }

   public static List<Double> rolloutTimeseriesTo8760(final TimeInterval concreteInterval, final List<Double> currentValues) {
      final List<Double> changedTimeseries = new LinkedList<>();
      switch (concreteInterval) {
      case QUARTERHOUR:
         if (currentValues.size() != 35040) {
            throw new RuntimeException("Wrong length");
         }
         for (int hour = 0; hour < 8760; hour++) {
            int index = hour * 4;
            double average = (currentValues.get(index) + currentValues.get(index + 1) + currentValues.get(index + 2) + currentValues.get(index + 3)) / 4;
            changedTimeseries.add(average);
         }
         break;
      case HOUR:
         changedTimeseries.addAll(currentValues);
         break;
      case DAY:
         for (int day = 0; day < 365; day++) {
            for (int quarterhour = 0; quarterhour < DAY_LENGTH; quarterhour++) {
               changedTimeseries.add(currentValues.get(day));
            }
         }
         break;
      case MONTH:
         for (int month = 0; month < 12; month++) {
            final DateTime copy = new DateTime().year().setCopy(2015); // zufälliges Nicht-Schaltjahr
            final DateTime copy2 = copy.monthOfYear().setCopy(month + 1);
            final int days = copy2.dayOfMonth().getMaximumValue();
            for (int quarterhour = 0; quarterhour < DAY_LENGTH * days; quarterhour++) {
               changedTimeseries.add(currentValues.get(month));
            }
         }
         break;
      case WEEK:
         for (int week = 0; week < 52; week++) {
            for (int quarterhour = 0; quarterhour < WEEK_LENGTH * DAY_LENGTH; quarterhour++) {
               changedTimeseries.add(currentValues.get(week));
            }
         }
         // Jahr hat 365 Tage, 7 * 52 = 364
         for (int quarterhour = 0; quarterhour < DAY_LENGTH; quarterhour++) {
            changedTimeseries.add(currentValues.get(51));
         }
         LOG.debug("changedTimeseries.size: {}", changedTimeseries.size());
         break;
      case YEAR:
         for (int quarterhour = 0; quarterhour < 8760; quarterhour++) {
            changedTimeseries.add(currentValues.get(0));
         }
      }
      return changedTimeseries;
   }

}
