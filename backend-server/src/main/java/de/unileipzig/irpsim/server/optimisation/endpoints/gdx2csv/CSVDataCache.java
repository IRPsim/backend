package de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv;

import java.io.File;

import org.apache.commons.jcs.JCS;
import org.apache.commons.jcs.access.CacheAccess;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationYearPersistent;
import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading.GDXSQLiteData;
import de.unileipzig.irpsim.server.optimisation.queue.OptimisationJobHandler;

public class CSVDataCache {
   
   private static final Logger LOG = LogManager.getLogger(CSVDataCache.class);
   
   /**
    * Speichert GDX-Daten
    */
   private static CacheAccess<Integer, GDXSQLiteData> savedData = JCS.getInstance("default");

   public static GDXSQLiteData getData(final int simulationid, final int year, int modelindex) {
      final int cacheId = simulationid * 100 + year;
      GDXSQLiteData data = savedData.get(cacheId);
      if (data == null) {
         data = loadData(simulationid, year, modelindex, cacheId);
      }
      return data;
   }

   private static GDXSQLiteData loadData(final int simulationid, final int year, int modelindex, final int cacheId) {
      GDXSQLiteData data;
      LOG.info("Lade GDX-Daten für ID: {}", simulationid);
      final OptimisationJobPersistent persistentJob = OptimisationJobHandler.getInstance().getJob(simulationid);
      if (persistentJob == null) {
         LOG.error("Simulationslauf mit ID " + simulationid + "existiert nicht");
         throw new RuntimeException("Simulationslauf mit ID " + simulationid + "existiert nicht");
      }
      final OptimisationYearPersistent yearData = persistentJob.getYearDataOfSimulatedYear(year, modelindex);
      if (yearData == null) {
         throw new RuntimeException("Stützjahr: " + year + " existiert nicht!");
      }
      final String gdxResultFile = yearData.getSqlite();
      final File sqlDatabaseFile = new File(gdxResultFile);
      data = new GDXSQLiteData(persistentJob.getSimulationsteps(), sqlDatabaseFile);
      savedData.put(cacheId, data);
      return data;
   }
   
}
