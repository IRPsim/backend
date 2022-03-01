package de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.LinkedMap;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading.DataMapReader;
import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading.GDXSQLiteData;


public class CSVWriter {
   
   private final StringWriter stringWriter = new StringWriter();
   private final Writer writer = new BufferedWriter(stringWriter);
   private final List<RequestedParametersYear> requestedParameters;
   
   private final DataMapReader data;
   
   public CSVWriter(DataMapReader dataMapReader, final List<RequestedParametersYear> requestedParameters) {
      data = dataMapReader;
      this.requestedParameters = requestedParameters;
   }
   
   public String write() throws IOException {
      writeHeader();

      final Map<RequestedParametersYear, ParameterdataWriter> dataMap = buildWriters();
      writeData(dataMap);

      writer.flush();

      String result = stringWriter.toString();
      return result;
   }

   private void writeData(Map<RequestedParametersYear, ParameterdataWriter> dataMap) throws IOException {
      final DateTimeFormatter formatter = DateTimeFormat.forPattern("dd.MM. HH:mm");
      DateTime time = new DateTime(2013, 1, 1, 0, 0);
      for (int timeIndex = 0; timeIndex < (data.isScalar() ? 1 : data.getSteps()); timeIndex++) {
         if (!data.isScalar()) {
            writer.write(formatter.print(time) + ";");
            writer.write("ii" + (timeIndex + 1) + ";");
         }
         for (final Map.Entry<RequestedParametersYear, ParameterdataWriter> entry : dataMap.entrySet()) {
            ParameterdataWriter parameterdata = entry.getValue();
            parameterdata.write(writer, timeIndex);
         }
         writer.write("\n");
         time = time.plusSeconds(900);
      }
   }

   private Map<RequestedParametersYear, ParameterdataWriter> buildWriters() throws IOException {
      final Map<RequestedParametersYear, ParameterdataWriter> result = new LinkedMap<>();
      for (final RequestedParametersYear year : requestedParameters) {
         Map<Integer, GDXSQLiteData> simulation = data.getGdxDataMap().get(year.getSimulationid());
         final GDXSQLiteData gdxdata = simulation.get(year.getYear());
         final ParameterdataWriter parameterdata = new ParameterdataWriter(year, gdxdata, gdxdata.getSteps());
         result.put(year, parameterdata);
      }
      return result;
   }

   private void writeHeader() throws IOException {
      if (!data.isScalar()) {
         writer.write("Datum/Zeit;set_ii;");
      }

      for (final RequestedParametersYear year : requestedParameters) {
         final Map<Integer, GDXSQLiteData> simulation = data.getGdxDataMap().get(year.getSimulationid());
         final GDXSQLiteData gdxdata = simulation.get(year.getYear());
         writeHeaderEntry(year, gdxdata, writer);
      }

      writer.write("\n");
   }

   private void writeHeaderEntry(final RequestedParametersYear requestedParameters, final GDXSQLiteData gdxdata, final Writer writer) throws IOException {
      for (final Map.Entry<String, Combination> entry : requestedParameters.getParameters().entrySet()) {
         for (final List<String> dependents : entry.getValue().getCombinations()) {
            String parameterHead = requestedParameters.getSimulationid() + "-" + requestedParameters.getYear() + "-" + entry.getKey();
            for (final String value : dependents) {
               parameterHead += "-" + value;
            }
            writer.write(parameterHead + ";");

         }
      }
   }

}
