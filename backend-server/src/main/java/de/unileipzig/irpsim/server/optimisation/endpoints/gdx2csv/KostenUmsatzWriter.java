package de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;

import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading.DataMapReader;
import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading.GDXSQLiteData;
import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading.Transformer;

public class KostenUmsatzWriter {

   private final Map<String, KostenUmsatz> response = new TreeMap<>();

   static class KostenUmsatzHeader {
      private final int index;
      private final String parameter;
      private final List<List<String>> dependents;
      private final int simulationid;

      public KostenUmsatzHeader(int index, String parameter, List<List<String>> dependents, int simulationid) {
         super();
         this.index = index;
         this.parameter = parameter;
         this.dependents = dependents;
         this.simulationid = simulationid;
      }

      @Override
      public boolean equals(Object o) {
         if (this == o)
            return true;
         if (o == null || getClass() != o.getClass())
            return false;
         KostenUmsatzHeader that = (KostenUmsatzHeader) o;
         return index == that.index &&
               simulationid == that.simulationid &&
               Objects.equals(parameter, that.parameter) &&
               Objects.equals(dependents, that.dependents);
      }

      @Override
      public int hashCode() {
         return Objects.hash(index, parameter, dependents, simulationid);
      }

      @Override
      public String toString() {
         return "KostenUmsatzHeader{" +
               "index=" + index +
               ", parameter='" + parameter + '\'' +
               ", dependents=" + dependents +
               ", simulationid=" + simulationid +
               '}';
      }
   }

   private final List<RequestedParametersYear> requestedParameters;
   private final DataMapReader data;

   private final StringWriter stringWriter = new StringWriter();
   private final Writer writer = new BufferedWriter(stringWriter);

   public KostenUmsatzWriter(DataMapReader dataMapReader, List<RequestedParametersYear> requestedParameters) {
      data = dataMapReader;
      this.requestedParameters = requestedParameters;
   }

   public KostenUmsatzWriter(final List<RequestedParametersYear> requestedParameters) {
      data = new DataMapReader(requestedParameters);
      this.requestedParameters = requestedParameters;
   }

   public String write() throws IOException {
      List<KostenUmsatzHeader> params = getParams();
      writeData(params);
      writer.flush();
      return stringWriter.toString();
   }

   private void writeData(List<KostenUmsatzHeader> params) throws IOException {
      for (Entry<Integer, Map<Integer, GDXSQLiteData>> run : data.getGdxDataMap().entrySet()) {
         for (Entry<Integer, GDXSQLiteData> yeardata : run.getValue().entrySet()) {
            for (KostenUmsatzHeader param : params) {
               if (param.simulationid == run.getKey()) {
                  for (List<String> dependent : param.dependents) {
                     double[] values = yeardata.getValue().getValues(param.parameter, dependent);
                     if (values.length > 0) {
                        setResponse(param, dependent, Transformer.transform(ParameterdataWriter.SUMME, values));
                     } else {
                        setResponse(param, dependent, new double[] { 0 });
                     }
                  }
               }
            }
         }
      }

      int color = 0;
      int index = 0;
      for (Map.Entry<String, KostenUmsatz> entry : response.entrySet()) {
         if (index % 2 == 0) {
            color++;
         }
         index++;
         String key = entry.getKey();
         KostenUmsatz kostenUmsatz = entry.getValue();
         writer.write(kostenUmsatz.getUmsatz() + ";" + Math.abs(kostenUmsatz.getKosten()) + ";15;" + color + ";" + key);
         writer.write("\n");

      }
   }

   private void setResponse(KostenUmsatzHeader header, List<String> dependent, double[] valuesTransformed) {
      String key = "[ID:" + header.simulationid + "]" + dependent.toString();
      if (response.containsKey(key)) {
         KostenUmsatz kostenUmsatz = response.get(key);
         setKostenUmsatz(header, valuesTransformed, kostenUmsatz, dependent);
      } else {
         KostenUmsatz kostenUmsatz = new KostenUmsatz(0, 0);
         setKostenUmsatz(header, valuesTransformed, kostenUmsatz, dependent);
      }
   }

   private void setKostenUmsatz(KostenUmsatzHeader header, double[] valuesTransformed, KostenUmsatz kostenUmsatz, List<String> dependent) {
      if ((header.index % 2) == 0) {
         kostenUmsatz.setKosten(valuesTransformed[0]);
      } else {
         kostenUmsatz.setUmsatz(valuesTransformed[0]);
      }
      response.put("[ID:" + header.simulationid + "]" + dependent.toString(), kostenUmsatz);
   }

   private List<KostenUmsatzHeader> getParams() {
      List<KostenUmsatzHeader> params = new ArrayList<>();
      for (RequestedParametersYear parameters : requestedParameters) {
         int index = 0;
         for (Entry<String, Combination> parameter : parameters.getParameters().entrySet()) {
            KostenUmsatzHeader header = new KostenUmsatzHeader(index++, parameter.getKey(), parameter.getValue().getCombinations(), parameters.getSimulationid());
            params.add(header);
         }
      }
      return params;
   }

}
