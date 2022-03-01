package de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading.GDXSQLiteData;
import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading.Transformer;

/**
 * Writes the data for all combinations of one parameter
 * 
 * @author reichelt
 *
 */
public class ParameterdataWriter {

   public static final String DURCHSCHNITT = "Durchschnitt";
   public static final String SUMME = "Summe";
   public static final String VIERTELSTUNDENWERTE = "Viertelstundenwerte";
   public static final String STUNDEN_DURCHSCHNITT = "Stunden-Durchschnitt";
   public static final String DURCHSCHNITTS_JAHRESWOCHE = "Durchschnitts-Jahreswoche";

   private final Map<String, Map<List<String>, double[]>> values = new HashMap<>();
   private final RequestedParametersYear requestedParameters;

   public ParameterdataWriter(final RequestedParametersYear requestedParameters, final GDXSQLiteData gdxdata, final int steps)
         throws IOException {
      this.requestedParameters = requestedParameters;
      for (final Map.Entry<String, Combination> entry : requestedParameters.getParameters().entrySet()) {
         final Map<List<String>, double[]> parameterValues = new HashMap<>();
         for (final List<String> dependents : entry.getValue().getCombinations()) {
            double[] timeseries = gdxdata.getValues(entry.getKey(), dependents);
            timeseries = Transformer.transform(entry.getValue().getSet_ii(), timeseries);
            parameterValues.put(dependents, timeseries);
         }
         values.put(entry.getKey(), parameterValues);
      }
   }

   public void write(final Writer writer, final int timeIndex) throws IOException {
      for (final Map.Entry<String, Combination> entry : requestedParameters.getParameters().entrySet()) {
         final Map<List<String>, double[]> allParameterValues = values.get(entry.getKey());
         for (final List<String> dependents : entry.getValue().getCombinations()) {
            final double[] currentValues = allParameterValues.get(dependents);
            writeLine(writer, timeIndex, entry, currentValues);
         }
      }
   }

   private void writeLine(final Writer writer, final int timeIndex, final Map.Entry<String, Combination> entry, final double[] currentValues) throws IOException {
      if (currentValues != null) {
         if (currentValues.length > timeIndex) {
            writer.write("" + currentValues[timeIndex]);
         }
         writer.write(";");
      } else {
         writer.write("NA;");
      }
   }

}