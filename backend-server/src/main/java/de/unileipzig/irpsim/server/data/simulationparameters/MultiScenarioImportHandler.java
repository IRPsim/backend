package de.unileipzig.irpsim.server.data.simulationparameters;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.data.timeseries.DatabaseConnectionHandler;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersMultiModel;
import de.unileipzig.irpsim.core.simulation.data.ConnectedModelParameter;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;

/**
 * Verwaltet den Import aller Zeitreihen von MultiModelSzenarios, indem die Zeitreihen zuerst in eine .csv-Datei exportiert und anschließend importiert wird; Damit ist eine
 * Benutzung von Infobright-Datenbanken möglich.
 *
 * @author kluge
 */
public class MultiScenarioImportHandler extends ScenarioImportBase {
   private static final Logger LOG = LogManager.getLogger(MultiScenarioImportHandler.class);
   private final BackendParametersMultiModel parameters;
   public static final int MIN_REFERENCING_SIZE = 1000;

   /**
    * Erzeugt eine neue ImportTimerowHandler-Instanz, setzt ID und GAMSParametersJSON.
    *
    * @param parameters Die GAMS-Daten
    */
   public MultiScenarioImportHandler(final JSONParametersMultimodel parameters, final int scenario, final boolean isIn) {
      super(scenario, isIn);
      this.parameters = new BackendParametersMultiModel(parameters);
   }
   
   public MultiScenarioImportHandler(final BackendParametersMultiModel parameters, final int scenario, final boolean isIn) {
      super(scenario, isIn);
      this.parameters = parameters;
   }

   /**
    * Liefert die GAMS-Daten.
    *
    * @return Die GAMS-Daten
    */
   public final JSONParametersMultimodel getParameters() {
      return parameters.createJSONParameters();
   }

   /**
    * Führt den Import von Zeitreihendaten aus.
    *
    * @return die Referenzierten Daten
    * @throws InterruptedException
    */
   public final JSONParametersMultimodel handleTimeseries() throws InterruptedException {
      LOG.info("Beginne Import");

      final File outputFile = new File(DatabaseConnectionHandler.getInstance().getMysqlJavaPath(), "temp.sql");
      outputFile.delete();

      for (ConnectedModelParameter model : parameters.getModels()) {
         handleSimpleTimeseries(model.getYeardata());
      }
      for (ConnectedModelParameter model : parameters.getModels()) {
         handleSetTimeseries(model.getYeardata());
      }
      for (ConnectedModelParameter model : parameters.getModels()) {
         handleTableTimeseries(model.getYeardata());
      }

      pool.shutdown();
      boolean finished = false;
      while (!finished) {
         finished = pool.awaitTermination(10, TimeUnit.SECONDS);
         LOG.debug("Beendet: {}", finished);
      }

      return getParameters();
   }
}
