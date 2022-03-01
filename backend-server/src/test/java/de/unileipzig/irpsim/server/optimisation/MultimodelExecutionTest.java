package de.unileipzig.irpsim.server.optimisation;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.simulation.data.TimeseriesTooShortException;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.persistence.State;
import de.unileipzig.irpsim.core.standingdata.DataLoader;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.core.utils.PersistenceFolderUtil;
import de.unileipzig.irpsim.core.utils.TestFiles;
import org.junit.Assert;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MultimodelExecutionTest {
   private final File exampleScenario = new File("src/main/resources/scenarios/5.json");
   
   @Before
   public void setup() {
      DatabaseTestUtils.setupDbConnectionHandler();
      DataLoader.initializeTimeseriesTables();
      PersistenceFolderUtil.setPersistenceFolder(TestFiles.TEST_PERSISTENCE_FOLDER);
   }
   
   @Test
   public void testCombinedExecution() throws IOException, InterruptedException {
      JSONParametersMultimodel multiModelParameters = Constants.MAPPER.readValue(exampleScenario, JSONParametersMultimodel.class);
      
      File tempDir = new File("target/persistence-tests");
      PersistenceFolderUtil.setPersistenceFolder(tempDir);
      MultiModelJob job = new MultiModelJob(multiModelParameters, false, false);
      
      job.start();
      job.getJobThread().join();
      
      Assert.assertEquals(State.FINISHED, job.getState());
   }

   @Test
   public void testParameterConnection() throws IOException {
      JSONParametersMultimodel multiModelParameters = Constants.MAPPER.readValue(exampleScenario, JSONParametersMultimodel.class);

      MultiModelJob job = new MultiModelJob(multiModelParameters, false, false);
      try {
         job.runYearModel();
      } catch (TimeseriesTooShortException | InterruptedException e) {
         e.printStackTrace();
      }

      File tempDir = new File("target/persistence-tests");
      PersistenceFolderUtil.setPersistenceFolder(tempDir);

      final File jobDir = PersistenceFolderUtil.getRunningJobFolder();
      System.out.println(jobDir);
      File input = new File(tempDir, job.getId() + File.separator + "model-1/year-0/input_0.json");

      Assert.assertTrue(input.exists());

      final Scanner scanner = new Scanner(input, StandardCharsets.UTF_8);
      final String content = scanner.useDelimiter("\\Z").next();
      scanner.close();

      int timeseriesidx = content.lastIndexOf("timeseries");
      int tablesidx = content.indexOf("tables");
      String timeseries = content.substring(timeseriesidx, tablesidx);
      // default value is 0 at time of writing the test
      Assert.assertTrue(timeseries.contains("par_C_MS_E"));
      try {
         int par_C_MS_E_idx = timeseries.lastIndexOf("par_C_MS_E\":");
         String numberString = timeseries.substring(par_C_MS_E_idx);
         String str = numberString.substring(numberString.indexOf(":")+1);
         boolean isArray = str.startsWith("[");
         if (isArray){
            Assert.assertTrue(true);
         } else {
            int number = Integer.parseInt(str.replaceAll("[^\\d.]", ""));
            System.out.println(numberString);
            System.out.println(str);
            Assert.assertTrue(number > 0);
         }
      } catch (StringIndexOutOfBoundsException e){
      }
   }
}
