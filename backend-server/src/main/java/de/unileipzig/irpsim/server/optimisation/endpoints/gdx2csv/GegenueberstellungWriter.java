package de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading.DataMapReader;
import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading.GDXSQLiteData;
import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading.Transformer;

public class GegenueberstellungWriter {

   static class GegenueberstellungHeader {
      private final String parameter;
      private final List<String> dependents;

      public GegenueberstellungHeader(String parameter, List<String> dependents) {
         super();
         this.parameter = parameter;
         this.dependents = dependents;
      }

      @Override
      public boolean equals(Object obj) {
         if (obj instanceof GegenueberstellungHeader) {
            GegenueberstellungHeader other = (GegenueberstellungHeader) obj;
            return parameter.equals(other.parameter) && dependents.equals(other.dependents);
         }
         return false;
      }
      
      @Override
      public int hashCode() {
         return parameter.hashCode() + dependents.hashCode();
      }
   }

   private final List<RequestedParametersYear> requestedParameters;
   private final DataMapReader data;

   private final StringWriter stringWriter = new StringWriter();
   private final Writer writer = new BufferedWriter(stringWriter);

   public GegenueberstellungWriter(final List<RequestedParametersYear> requestedParameters) {
      data = new DataMapReader(requestedParameters);
      this.requestedParameters = requestedParameters;
   }

   public String write() throws IOException {
      Set<GegenueberstellungHeader> headers = getHeaders();
      writeHeader(headers);
      writeData(headers);
      writer.flush();
      return stringWriter.toString();
   }

   private void writeData(Set<GegenueberstellungHeader> headers) throws IOException {
      for (Entry<Integer, Map<Integer, GDXSQLiteData>> run : data.getGdxDataMap().entrySet()) {
         for (Entry<Integer, GDXSQLiteData> yeardata : run.getValue().entrySet()) {
            writer.write("Lauf " + run.getKey() + " Jahr " + yeardata.getKey() + ";");
            for (GegenueberstellungHeader header : headers) {
               double[] values = yeardata.getValue().getValues(header.parameter, header.dependents);
               if (values.length > 0) {
                  double[] valuesTransformed = Transformer.transform(ParameterdataWriter.SUMME, values);
                  writer.write(valuesTransformed[0] + ";");
               } else {
                  writer.write("0;");
               }
            }
            writer.write("\n");
         }
      }
   }

   private void writeHeader(Set<GegenueberstellungHeader> headers) throws IOException {
      writer.write("Lauf;");
      for (GegenueberstellungHeader headerEntry : headers) {
         writer.write(headerEntry.parameter + "-" + headerEntry.dependents + ";");
      }
      writer.write("\n");
   }

   private Set<GegenueberstellungHeader> getHeaders() {
      Set<GegenueberstellungHeader> headers = new HashSet<>();
      for (RequestedParametersYear parameters : requestedParameters) {
         for (Entry<String, Combination> parameter : parameters.getParameters().entrySet()){
            for (List<String> dependents : parameter.getValue().getCombinations()) {
               GegenueberstellungHeader header = new GegenueberstellungHeader(parameter.getKey(), dependents);
               headers.add(header);
            }
         }
      }
      return headers;
   }

}
