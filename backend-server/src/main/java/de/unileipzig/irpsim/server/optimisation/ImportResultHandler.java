package de.unileipzig.irpsim.server.optimisation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.persistence.EntityTransaction;
import javax.persistence.RollbackException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;

import de.unileipzig.irpsim.core.data.timeseries.DatabaseConnectionHandler;
import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.Set;
import de.unileipzig.irpsim.core.simulation.data.SetElement;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.standingdata.StaticDataUtil;
import de.unileipzig.irpsim.core.standingdata.data.StaticData;
import de.unileipzig.irpsim.core.utils.DataUtils;
import de.unileipzig.irpsim.core.utils.ParameterOutputDependenciesUtil;

/**
 * Verwaltet Datenbankimports, wobei die Zeitreihen - im Gegensatz zum TimeseriesImportHandler - Schritt für Schritt und nicht auf einmal befüllt werden.
 *
 * @author reichelt
 */
public class ImportResultHandler {
   private static final Logger LOG = LogManager.getLogger(ImportResultHandler.class);
   private static final double MIKRO = 10E6;

   private final Map<Integer, ImportMetadata> referenceFileMap;
   private final BackendParametersYearData intermediaryResult;
   private final int year;
   private final long interval;

   public ImportResultHandler(final BackendParametersYearData intermediaryResult) {
      referenceFileMap = new HashMap<>();
      this.intermediaryResult = intermediaryResult;
      this.year = intermediaryResult.getConfig().getYear();
      if (intermediaryResult.getConfig().getResolution() == 8760) {
         interval = 60;
      } else if (intermediaryResult.getConfig().getResolution() == 35040) {
         interval = 15;
      } else {
         LOG.error("Unknown resolution: {}", intermediaryResult.getConfig().getResolution());
         interval = 15;
      }
   }

   /**
    * Liefert Map mit String-Referenzen zu der Importmetadaten zurück.
    *
    * @return Map mit String-Referenzen zu der Importmetadaten.
    */
   public final Map<Integer, ImportMetadata> getReferenceFileMap() {
      return referenceFileMap;
   }

   /**
    * Fügt den Wert zu den Metadaten hinzu, die später in die Datenbank geladen werden.
    *
    * @param name Parametername mit Abhängigen
    * @param value Der Wert
    */
   public final void manageResult(final String name, final double value) {
      if (name.contains("(") && !name.contains("par_out_Modelstat") && !name.contains("par_out_Solvestat")) {
         final int indexOfParathesis = name.indexOf("(");
         final String parName = name.substring(0, indexOfParathesis);
         final String parameterLine = name.substring(indexOfParathesis + 1, name.length() - 1);
         final String[] dependentNames = OptimisationJobUtils.COMMAPATTERN.split(parameterLine);
         switch (dependentNames.length) {
         case 1:
            handleTimeseries(value, parName);
            break;
         case 2:
            manageSetTimeseriesResult(value, parName, dependentNames[1]);
            break;
         case 3:
            manageTableTimeseriesResult(value, parName, dependentNames);
         }
      }
   }

   /**
    * Fügt den Wert zu einer Tabellenzeitreihe hinzu.
    *
    * @param value Der Wert
    * @param parName Name des Parameter
    * @param dependentNames Name der Abhängigen Sets
    */
   public final void manageTableTimeseriesResult(final double value, final String parameterName, final String[] dependentNames) {

      // final String reference = "temp_" + parName + "_" + dependentNames[1]
      // + "_" + dependentNames[2] + nameSuffix;
      final Map<String, Map<String, Timeseries>> firstDependents = DataUtils
            .getOrNewMap(intermediaryResult.getTableTimeseries(), parameterName);
      final Map<String, Timeseries> secondDependents = DataUtils.getOrNewMap(firstDependents, dependentNames[1]);
      final ImportMetadata metadata;
      if (secondDependents.containsKey(dependentNames[2]) && secondDependents.get(dependentNames[2]).hasReference()) {
         metadata = referenceFileMap.get(secondDependents.get(dependentNames[2]).getSeriesname());
      } else {
         final StaticData ds = StaticDataUtil.importDatensatz(0, year, false);
         metadata = new ImportMetadata(ds, parameterName);
         referenceFileMap.put(ds.getId(), metadata);
         secondDependents.put(dependentNames[2], Timeseries.build(ds.getId()));
      }
      writeValueToData(value, metadata);
   }

   /**
    * Fügt den Wert zu einer Setzeitreihe hinzu.
    *
    * @param value Der Wert
    * @param parameterName Name des Parameters
    * @param setElementName Name des SetElementes
    */
   public final void manageSetTimeseriesResult(final double value, final String parameterName, final String setElementName) {
      final String setName = ParameterOutputDependenciesUtil.getInstance().getOutputSetName(parameterName, intermediaryResult.getConfig().getModeldefinition());
      LOG.trace("Par: " + parameterName + " Set: " + setName + " " + setElementName);
      Set destSet = findSet(setName);
      SetElement destElement = findDestElement(setElementName, destSet);

      final ImportMetadata metadata;
      if (destElement.getTimeseries().containsKey(parameterName)
            && destElement.getTimeseries().get(parameterName).hasReference()) {
         metadata = referenceFileMap.get(destElement.getTimeseries().get(parameterName).getSeriesname());
      } else {
         final StaticData ds = StaticDataUtil.importDatensatz(0, year, false);
         metadata = new ImportMetadata(ds, parameterName);
         referenceFileMap.put(ds.getId(), metadata);
         destElement.getTimeseries().put(parameterName, Timeseries.build(ds.getId()));
      }
      writeValueToData(value, metadata);
   }

   private Set findSet(final String setName) {
      final Optional<Set> findAny = intermediaryResult.getSets().values().stream()
            .filter(p -> p.getName().equals(setName)).findAny();
      Set destSet = null;
      if (!findAny.isPresent()) {
         destSet = new Set();
         destSet.setName(setName);
         intermediaryResult.getSets().put(destSet.getName(), destSet);
      } else {
         destSet = findAny.get();
      }
      return destSet;
   }

   private SetElement findDestElement(final String setElementName, Set destSet) {
      SetElement destElement = destSet.getElement(setElementName);
      if (destElement == null) {
         destElement = new SetElement(setElementName);
         destSet.getElements().add(destElement);
      }
      return destElement;
   }

   /**
    * Fügt den Wert zu einer direkten Zeitreihe hinzu.
    *
    * @param value Der Wert
    * @param parameterName Name des Parameters
    */
   public final void handleTimeseries(final double value, final String parameterName) {
      final ImportMetadata metadata;
      final Timeseries timeseries = intermediaryResult.getTimeseries().get(parameterName);
      if (timeseries != null && timeseries.hasReference()) {
         metadata = referenceFileMap.get(timeseries.getSeriesname());
      } else {
         final StaticData staticdata = StaticDataUtil.importDatensatz(0, year, false);
         metadata = new ImportMetadata(staticdata, parameterName);
         referenceFileMap.put(staticdata.getId(), metadata);
         intermediaryResult.getTimeseries().put(parameterName, Timeseries.build(staticdata.getId()));
      }
      writeValueToData(value, metadata);
   }

   /**
    * Fügt einen Wert zu Metadaten hinzu.
    *
    * @param value Der hinzuzufügende Wert
    * @param metadata Die Metadaten an welche Informationen angefügt werden sollen.
    */
   public final void writeValueToData(final double value, final ImportMetadata metadata) {
      try {
         if (metadata.isZeroTimeseries() && value != 0) {
            metadata.setZeroTimeseries(false);
         }
         final long time = new DateTime(year, 1, 1, 0, 0).getMillis() + metadata.getRowCount() * interval * 60 * 1000;
         metadata.getWriter().write(metadata.getReference() + ";" + time + ";" + value + "\n");
         metadata.addRow();
         if (metadata.getRowCount() % 200 == 0) {
            metadata.getWriter().flush();
         }
      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   /**
    * Schreibt Serien-Metadaten in die Datenbank.
    */
   public final void executeImports() {
      final long start = System.nanoTime();

      final int timeserieslength = intermediaryResult.getConfig().getSimulationlength();
      String errorString = "";
      for (final Entry<Integer, ImportMetadata> metadataEntry : referenceFileMap.entrySet()) {
         String tempErrorString = importMetadata(metadataEntry.getKey(), metadataEntry.getValue(), timeserieslength);
         errorString += tempErrorString;
         if (Thread.currentThread().isInterrupted()) {
            return;
         }

      }
      if (errorString.length() > 0) {
         throw new RuntimeException(errorString);
      }

      for (final ImportMetadata metadata : referenceFileMap.values()) {
         metadata.getFile().delete();
      }

      final long duration = System.nanoTime() - start;
      LOG.debug("Importzeit: " + duration / MIKRO);
   }

   public String importMetadata(int id, ImportMetadata metadata, int timeserieslength) {
      String tempErrorString = "";
      // Zu lange/kurze Zeitreihen sind nur relevant, wenn 35040 Werte vorhanden sind; bei einer anderen Wertanzahl sind immer einige Zeitreihen länger, da die 0-Zeitreihe 35040
      // Werte groß ist
      if (metadata.getRowCount() != timeserieslength && timeserieslength == 35040) {
         final String message = "Achtung, Zeitreihe " + metadata.getReference() + " (" + metadata.getParametername() + ") ist " + metadata.getRowCount() + " groß, sollte aber "
               + timeserieslength
               + " groß sein.";
         LOG.error(message);
         tempErrorString = message + " ";
      }
      try {
         metadata.getWriter().close();
      } catch (final IOException e1) {
         e1.printStackTrace();
      }
      if (metadata.isZeroTimeseries()) {
         // TODO: lookupMapWithReference ist evtl. ein Performanceengpass.
         final Map.Entry<String, Timeseries> zeroTimeseriesElement = intermediaryResult.lookupMapWithReference(id);
         zeroTimeseriesElement.setValue(Timeseries.ZEROTIMESERIES_REFERENCE);
         return "";
      } else {
         final String parameter = intermediaryResult.lookupMapWithReference(id).getKey();
         LOG.trace("Ungleich 0: {} Parameter: {} {}", id, metadata, parameter);
      }
      LOG.debug("Füge import hinzu: {} Länge: {}", metadata.getReference(), metadata.getRowCount());

      final String persistSeriesData = "LOAD DATA infile '"
            + DatabaseConnectionHandler.getInstance().getMysqlSQLPath() + metadata.getFilename()
            + "' INTO TABLE series_data_out fields terminated by ';' lines terminated by '\n'";

      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final EntityTransaction transaction = em.getTransaction();
         transaction.begin();
         em.createNativeQuery(persistSeriesData).executeUpdate();
         transaction.commit();
      } catch (final RollbackException e) {
         e.printStackTrace();
      }
      return tempErrorString;
   }
}
