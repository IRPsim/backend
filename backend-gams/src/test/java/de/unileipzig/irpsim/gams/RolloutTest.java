package de.unileipzig.irpsim.gams;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

import javax.persistence.EntityTransaction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.TimeInterval;
import de.unileipzig.irpsim.core.simulation.data.TimeseriesTooShortException;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.standingdata.AddData;
import de.unileipzig.irpsim.core.standingdata.DataLoader;
import de.unileipzig.irpsim.core.standingdata.StaticDataUtil;
import de.unileipzig.irpsim.core.standingdata.TimeseriesValue;
import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.core.utils.TestFiles;

/**
 * Testet das Ausrollen von Zeitreihen, die kürzer als ein Jahr sind, im GAMSModelParametrisationUtil.
 */
public class RolloutTest {

   private static final Logger LOG = LogManager.getLogger(RolloutTest.class);

   protected final static int[] ids = new int[7];
   private final static int year = 2018;

   @BeforeClass
   public static void initDB() {
      DatabaseTestUtils.setupDbConnectionHandler();
      DataLoader.initializeTimeseriesTables();
      DatabaseTestUtils.cleanUp();

      createExampleTimeseries();
   }

   private static final long start = new DateTime(year, 1, 1, 0, 0).getMillis();

   private static void createExampleTimeseries() {
      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         createExampleDatensatz(em, 35040, TimeInterval.QUARTERHOUR, 0);
         createExampleDatensatz(em, 24 * 365, TimeInterval.HOUR, 1);
         createExampleDatensatz(em, 365, TimeInterval.DAY, 2);
         createExampleDatensatz(em, 52, TimeInterval.WEEK, 3);
         createExampleDatensatz(em, 12, TimeInterval.MONTH, 4);
         createExampleDatensatz(em, 1, TimeInterval.YEAR, 5);
         createExampleDatensatz(em, 11, TimeInterval.MONTH, 6);
      }
   }

   protected static void createExampleDatensatz(final ClosableEntityManager em, final int length, final TimeInterval interval, final int index) {
      final TimeseriesValue timeseriesValues[] = new TimeseriesValue[length];
      for (long i = 0; i < length; i++) {
         timeseriesValues[(int) i] = new TimeseriesValue(start + i * 15l * 1000l, 1.0);
      }
      createStammdatum(em, index, interval, timeseriesValues);
   }

   private static void createStammdatum(final ClosableEntityManager em, final int index, final TimeInterval interval, final TimeseriesValue timeseriesValues[]) {
      final Stammdatum testStammdatum = new Stammdatum();
      testStammdatum.setName("test_" + interval.getLabel());
      testStammdatum.setZeitintervall(interval);

      final EntityTransaction et = em.getTransaction();
      et.begin();
      em.persist(testStammdatum);
      et.commit();

      final AddData ad = new AddData();

      ad.setJahr(year);
      ad.setSzenario(1);
      ad.setValues(timeseriesValues);

      ids[index] = StaticDataUtil.importData(ad, em, testStammdatum).getId();
   }

   /**
    * Testet das herkömmliche Zeitreihen-Ausrollen.
    * 
    * @throws TimeseriesTooShortException
    * @throws IOException
    */
   @Test
   public void testRollout() throws TimeseriesTooShortException, IOException {
      final BackendParametersYearData yeardata = initYear();

      final GAMSHandler handler = new GAMSHandler(Files.createTempDirectory("irpsim").toFile());
      final GAMSModelParametrisationUtil parameterizer = new GAMSModelParametrisationUtil(handler, yeardata, 0);

      parameterizer.loadParameters();

      for (int i = 0; i < 6; i++) {
         final Timeseries ts = parameterizer.getReferencedTimeseries().get(ids[i]);
         LOG.info("i: {} {}", i, ts.getValues().size());
         Assert.assertEquals(35040, ts.getValues().size());
      }

   }

   /**
    * Testet, ob bei einer zu kurzen Monatszeitreihe ein Fehler entsteht.
    * 
    * @throws TimeseriesTooShortException
    * @throws IOException
    */
   //Jetzt überflüssig? Wird ja jetzt implizit in TimeseriesTooShortTest getestet.
   @Test(expected = TimeseriesTooShortException.class)
   public void testRolloutError() throws TimeseriesTooShortException, IOException {
      final BackendParametersYearData yeardata = initYear();
      yeardata.getTimeseries().put("par_F_SMS_E_contract_Sonnentank_discharge", Timeseries.build(ids[6]));

      final GAMSHandler handler = new GAMSHandler(Files.createTempDirectory("irpsim").toFile());
      final GAMSModelParametrisationUtil parameterizer = new GAMSModelParametrisationUtil(handler, yeardata, 0);

      parameterizer.loadParameters();
   }

   private BackendParametersYearData initYear() throws FileNotFoundException {
      final BackendParametersYearData yeardata = createBackendParametersYearData();
      LOG.debug(yeardata.getTimeseries().keySet());

      yeardata.getTimeseries().put("par_F_SMS_E_contract_feedin_PVdirect", Timeseries.build(ids[0]));
      yeardata.getTimeseries().put("par_F_SMS_E_contract_PVdirect", Timeseries.build(ids[1]));
      yeardata.getTimeseries().put("par_F_NS_E_grid_PVdirect", Timeseries.build(ids[2]));
      yeardata.getTimeseries().put("par_F_PS_E_levy_PVdirect", Timeseries.build(ids[3]));
      yeardata.getTimeseries().put("par_F_SMS_E_contract_Sonnentank_charge", Timeseries.build(ids[4]));
      yeardata.getTimeseries().put("par_sometest", Timeseries.build(ids[5]));

      return yeardata;
   }

   protected static BackendParametersYearData createBackendParametersYearData() throws FileNotFoundException {
      final File testFile = TestFiles.FULL_YEAR.make();
      final JSONParametersMultimodel jsonData = DatabaseTestUtils.getParameterObject(testFile);
      final BackendParametersYearData yeardata = new BackendParametersYearData(jsonData.getModels().get(0).getYears().get(0));
      return yeardata;
   }

}
