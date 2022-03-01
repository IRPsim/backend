package de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading;

import java.util.stream.DoubleStream;

import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.ParameterdataWriter;

public class Transformer {

   public static double[] transform(String set_ii, double[] timeseries) {
      double[] result;
      if (set_ii.equals(ParameterdataWriter.VIERTELSTUNDENWERTE)) {
         result = timeseries;
      } else if (set_ii.equals(ParameterdataWriter.SUMME)) {
         double sum = DoubleStream.of(timeseries).sum();
         result = new double[] { sum };
      } else if (set_ii.equals(ParameterdataWriter.DURCHSCHNITT)) {
         double avg = DoubleStream.of(timeseries).average().getAsDouble();
         result = new double[] { avg };
      } else if  (set_ii.equals(ParameterdataWriter.STUNDEN_DURCHSCHNITT)) {
         result = getAggregatedTimeseries(timeseries, 4);
      } else if (set_ii.equals(ParameterdataWriter.DURCHSCHNITTS_JAHRESWOCHE)) {
         result = getSlicedTimeseries(timeseries, 672);
      } else {
         result = new double[0];
      }
      return result;
   }

   private static double[] getAggregatedTimeseries(double[] timeseries, int aggregateIndicies) {
      int aggregateCounter = 0;
      int index = 0;
      double sum = 0;
      double[] resultTimeSeries = new double[35040 / aggregateIndicies];
      for (int i = 0; i < timeseries.length; i++) {
         if (aggregateCounter < aggregateIndicies) {
            aggregateCounter++;
            sum += timeseries[i];
         } else {
            resultTimeSeries[index++] = sum / aggregateIndicies;
            sum = timeseries[i];
            aggregateCounter = 1;
         }
      }
      resultTimeSeries[index] = sum / aggregateIndicies;
      return resultTimeSeries;
   }

   private static double[] getSlicedTimeseries(double[] timeseries, int slices) {
      int index = 0;
      double[] tempTimeSeries = new double[slices];
      double[] tempTimeSeriesCounter = new double[slices];
      for (int i = 0; i < timeseries.length; i++) {
         tempTimeSeries[index] += timeseries[i];
         tempTimeSeriesCounter[index]++;
         if (index < (slices - 1)) {
            index++;
         } else {
            index = 0;
         }
      }

      double[] resultTimeSeries = new double[slices];
      for (int k = 0; k < tempTimeSeries.length; k++) {
         resultTimeSeries[k] = tempTimeSeries[k] / tempTimeSeriesCounter[k];
      }

      return resultTimeSeries;
   }

}
