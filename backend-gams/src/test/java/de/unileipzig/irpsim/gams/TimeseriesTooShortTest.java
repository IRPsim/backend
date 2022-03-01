package de.unileipzig.irpsim.gams;

import static de.unileipzig.irpsim.gams.RolloutTest.createBackendParametersYearData;
import static de.unileipzig.irpsim.gams.RolloutTest.createExampleDatensatz;
import static de.unileipzig.irpsim.gams.RolloutTest.ids;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Test;

import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.TimeInterval;
import de.unileipzig.irpsim.core.simulation.data.TimeseriesTooShortException;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.standingdata.DataLoader;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;

public class TimeseriesTooShortTest {

   @BeforeClass
   public static void initDB() {
      DatabaseTestUtils.setupDbConnectionHandler();
      DataLoader.initializeTimeseriesTables();
      DatabaseTestUtils.cleanUp();

      createFailureTimeseries();
   }

   private static void createFailureTimeseries() {
      try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         createExampleDatensatz(em, 35000, TimeInterval.QUARTERHOUR, 0);
         createExampleDatensatz(em, 20 * 365, TimeInterval.HOUR, 1);
         createExampleDatensatz(em, 360, TimeInterval.DAY, 2);
      }
   }

   /**
    * Testet, ob bei mehreren fehlerhaften Zeitreihen alle gefundenen Fehler in der errorMessage enthalten sind.
    *
    * @throws IOException
    */
   @Test
   public void testSeveralRolloutErrors() throws IOException {
      final BackendParametersYearData yeardata = initYear();

      final GAMSHandler handler = new GAMSHandler(Files.createTempDirectory("irpsim").toFile());
      final GAMSModelParametrisationUtil parameterizer = new GAMSModelParametrisationUtil(handler, yeardata, 0);
      String errorMessage = "";

      try {
         parameterizer.loadParameters();
      } catch (TimeseriesTooShortException e) {
         errorMessage = e.getMessage();
      }

      MatcherAssert.assertThat(errorMessage,
            Matchers.containsString(ids[0] + "(par_F_SMS_E_contract_feedin_PVdirect) ist zu kurz, Länge ist: 35000, darf aber nur 35040, 8760, 672, 365, 52, 12 oder 1 (für Viertelstundenauflösung) oder 8760 oder 168 (für Stundenauflösung) sein."));
      MatcherAssert.assertThat(errorMessage,
            Matchers.containsString(ids[1] + "(par_F_SMS_E_contract_PVdirect) ist zu kurz, Länge ist: 7300, darf aber nur 35040, 8760, 672, 365, 52, 12 oder 1 (für Viertelstundenauflösung) oder 8760 oder 168 (für Stundenauflösung) sein."));
      MatcherAssert.assertThat(errorMessage,
            Matchers.containsString(ids[2] + "(par_F_NS_E_grid_PVdirect) ist zu kurz, Länge ist: 360, darf aber nur 35040, 8760, 672, 365, 52, 12 oder 1 (für Viertelstundenauflösung) oder 8760 oder 168 (für Stundenauflösung) sein."));

   }

   private BackendParametersYearData initYear() throws FileNotFoundException {

      BackendParametersYearData yeardata = createBackendParametersYearData();
      yeardata.getTimeseries().put("par_F_SMS_E_contract_feedin_PVdirect", Timeseries.build(ids[0]));
      yeardata.getTimeseries().put("par_F_SMS_E_contract_PVdirect", Timeseries.build(ids[1]));
      yeardata.getTimeseries().put("par_F_NS_E_grid_PVdirect", Timeseries.build(ids[2]));

      return yeardata;
   }

}
