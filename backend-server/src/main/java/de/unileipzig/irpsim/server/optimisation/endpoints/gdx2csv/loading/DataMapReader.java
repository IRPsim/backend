package de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.CSVDataCache;
import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.Combination;
import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.ParameterdataWriter;
import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.ParametermetaData;
import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.RequestedParametersYear;

public class DataMapReader {
   private boolean isScalar = true;
   private int steps = 0;
   private final Map<Integer, Map<Integer, GDXSQLiteData>> gdxDataMap = new HashMap<>();
   
   public DataMapReader(List<RequestedParametersYear> requestedParameters) {
      for (final RequestedParametersYear year : requestedParameters) {
         Map<Integer, GDXSQLiteData> jobMap = gdxDataMap.get(year.getSimulationid());
         if (jobMap == null){
            jobMap = new HashMap<>();
            gdxDataMap.put(year.getSimulationid(), jobMap);
         }
         GDXSQLiteData gdxdata = jobMap.get(year.getYear());
         if (gdxdata == null) {
            gdxdata = CSVDataCache.getData(year.getSimulationid(), year.getYear(), year.getModelindex());
            jobMap.put(year.getYear(), gdxdata);
         }

         if (!isScalarRequest(year, gdxdata)) {
            isScalar = false;
            steps = Math.max(steps, gdxdata.getSteps());
         }
      }
   }

   public boolean isScalar() {
      return isScalar;
   }
   
   public int getSteps() {
      return steps;
   }
   
   public Map<Integer, Map<Integer, GDXSQLiteData>> getGdxDataMap() {
      return gdxDataMap;
   }
   
   
   private boolean isScalarRequest(final RequestedParametersYear requestedParameters, final GDXSQLiteData gdxdata) {
      boolean isScalar = true;
      for (final Map.Entry<String, Combination> entry : requestedParameters.getParameters().entrySet()) {
         final ParametermetaData data = gdxdata.getData().get(entry.getKey());
         String set_ii = entry.getValue().getSet_ii();
         if (!data.isScalar() || set_ii.equals(ParameterdataWriter.SUMME) 
               || set_ii.equals(ParameterdataWriter.DURCHSCHNITT)) {
            isScalar = false;
         }
      }
      return isScalar;
   }
}