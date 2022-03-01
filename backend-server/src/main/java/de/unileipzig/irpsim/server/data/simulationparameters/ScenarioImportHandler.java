package de.unileipzig.irpsim.server.data.simulationparameters;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.data.timeseries.DatabaseConnectionHandler;
import de.unileipzig.irpsim.core.simulation.data.BackendParameters;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersSingleModel;

/**
 * Verwaltet den Import aller Zeitreihen eines Szenarios, indem die Zeitreihe zuerst in eine .csv-Datei exportiert und anschließend importiert wird; Damit ist eine Benutzung von
 * Infobright-Datenbanken möglich.
 *
 * @author reichelt
 */
public class ScenarioImportHandler extends ScenarioImportBase {

   private static final Logger LOG = LogManager.getLogger(ScenarioImportHandler.class);
   private final BackendParameters parameters;
   public static final int MIN_REFERENCING_SIZE = 1000;

   /**
    * Erzeugt eine neue ImportTimerowHandler-Instanz, setzt ID und GAMSParametersJSON.
    *
    * @param parameters Die GAMS-Daten
    */
   public ScenarioImportHandler(final BackendParameters parameters, final int scenario, final boolean isIn) {
      super(scenario, isIn);
      this.parameters = parameters;
   }

   /**
    * Liefert die GAMS-Daten.
    *
    * @return Die GAMS-Daten
    */
   public final JSONParametersSingleModel getParameters() {
      return parameters.createJSONParameters();
   }

   /**
    * Führt den Import von Zeitreihendaten aus.
    *
    * @return die Referenzierten Daten
    * @throws InterruptedException 
    */
   public final JSONParametersSingleModel handleTimeseries() throws InterruptedException {
      LOG.info("Beginne Import");

      final File outputFile = new File(DatabaseConnectionHandler.getInstance().getMysqlJavaPath(), "temp.sql");
      outputFile.delete();

      handleSimpleTimeseries(parameters.getYeardata());
      handleSetTimeseries(parameters.getYeardata());
      handleTableTimeseries(parameters.getYeardata());

      pool.shutdown();
      boolean finished = false;
      while (!finished) {
         finished = pool.awaitTermination(10, TimeUnit.SECONDS);
         LOG.debug("Beendet: {}", finished);
      }

      return getParameters();
   }
}
