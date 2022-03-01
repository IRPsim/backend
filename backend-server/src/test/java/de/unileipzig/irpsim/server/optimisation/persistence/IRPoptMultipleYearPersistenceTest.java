package de.unileipzig.irpsim.server.optimisation.persistence;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;

import javax.ws.rs.core.Response;

import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.simulation.data.Calculation;
import de.unileipzig.irpsim.core.simulation.data.GenericParameters;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.core.utils.ParameterOutputDependenciesUtil;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.server.marker.DBTest;
import de.unileipzig.irpsim.server.marker.GAMSTest;
import de.unileipzig.irpsim.server.marker.RESTTest;
import de.unileipzig.irpsim.server.optimisation.Job;
import de.unileipzig.irpsim.server.optimisation.queue.OptimisationJobHandler;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;

/**
 * @author reichelt
 */
@Category({ RESTTest.class, DBTest.class, GAMSTest.class })
public final class IRPoptMultipleYearPersistenceTest extends ServerTests {
   private static final Logger LOG = LogManager.getLogger(IRPoptMultipleYearPersistenceTest.class);

   private static Job job;

   @Rule
   public TestName name = new TestName();

   /**
    * Startet eine Optimierung und wartet bis zu deren Ende.
    */
   @BeforeClass
   public static void initialize() throws JsonParseException, JsonMappingException, IOException {
      Job.deleteAfterwards = false;

      job = executeSimpleRequest();
      ServerTestUtils.waitForSimulationEnd(job.getId());
   }

   /**
    * @return Der gestartete {@link Job}
    */
   public static Job executeSimpleRequest() {
      try {
         final String content = DatabaseTestUtils.getParameterText(TestFiles.MULTI_THREE_DAYS.make());
         LOG.trace("Anfrage: " + content);
         final long id = ServerTestUtils.startSimulation(content);

         final Job simJob = OptimisationJobHandler.getInstance().getRunningJob(id);
         return simJob;
      } catch (final FileNotFoundException e) {
         e.printStackTrace();
         return null;
      }
   }

   /**
    *
    */
   @Test
   public void testGeneralResults() throws JsonParseException, JsonMappingException, IOException {
      ServerTestUtils.isJobCorrectFinished(job.getId());
   }

   /**
    *
    */
   @Test
   public void testCSVResults() {
      final String testURI = ServerTestUtils.OPTIMISATION_URI + "/" + job.getId() + "/0/0/csvfile";
      LOG.info("Results-URI: " + testURI);
      final Response response = RESTCaller.callGetResponse(testURI);
      final String csvRequestResults = response.readEntity(String.class);
      LOG.debug("Simulationslauf beendet");

      String csvfile = null;
      try (Scanner scanner = new Scanner(job.getCSVFile(0, 0))) {
         csvfile = scanner.useDelimiter("\\Z").next() + "\n";
      } catch (final FileNotFoundException e) {
         e.printStackTrace();
      }

      LOG.debug("Ende Request: {}", StringEscapeUtils.escapeJava(csvRequestResults.substring(csvRequestResults.length() - 100)));
      LOG.debug("Ende Echt: {}", StringEscapeUtils.escapeJava(csvfile.substring(csvfile.length() - 100)));
      Assert.assertEquals(csvfile, csvRequestResults);
   }

   public void testCSVSecondYear() {
      final String testURI = ServerTestUtils.OPTIMISATION_URI + "/" + job.getId() + "/1/0/csvfile";
      LOG.info("Results-URI: " + testURI);
      final Response response = RESTCaller.callGetResponse(testURI);
      final String csvRequestResults = response.readEntity(String.class);
      LOG.debug("Simulationslauf beendet");

      String csvfile = null;
      try (Scanner scanner = new Scanner(job.getCSVFile(0, 3))) {
         csvfile = scanner.useDelimiter("\\Z").next() + "\n";
      } catch (final FileNotFoundException e) {
         e.printStackTrace();
      }

      LOG.debug("Ende Request: {}", StringEscapeUtils.escapeJava(csvRequestResults.substring(csvRequestResults.length() - 100)));
      LOG.debug("Ende Echt: {}", StringEscapeUtils.escapeJava(csvfile.substring(csvfile.length() - 100)));
      Assert.assertEquals(csvfile, csvRequestResults);
   }

   /**
    *
    */
   @Test
   public void testResults() throws JsonProcessingException {
      final GenericParameters realBPResults = job.getResults();
      final JSONParametersMultimodel realGamsResult = (JSONParametersMultimodel) realBPResults.createJSONParameters();
      String realResults = null;
      try {
         realResults = Constants.MAPPER.writeValueAsString(realGamsResult);
      } catch (final IOException e) {
         e.printStackTrace();
      }

      final String testURI = ServerTestUtils.OPTIMISATION_URI + "/" + job.getId() + "/results";
      LOG.info("Results-URI: " + testURI);
      final Response response = RESTCaller.callGetResponse(testURI);
      final String jsonServerResults = response.readEntity(String.class);

      Assert.assertEquals(realResults, jsonServerResults);
   }

   /**
    * TODO: unfertig.
    */
   @Ignore
   @Test
   public void testPostProcessing() {
      final String results = RESTCaller.callGet(ServerTestUtils.OPTIMISATION_URI + "/" + job.getId() + "/results");
      try {
         final JSONParametersMultimodel gamsResult = Constants.MAPPER.readValue(results, JSONParametersMultimodel.class);
         Assert.assertNotNull(gamsResult.getModels().get(0).getPostprocessing());
         final Map<String, List<Calculation>> postProcessings = ParameterOutputDependenciesUtil.getInstance().getPostprocessings(1);
         for (final Map.Entry<String, List<Calculation>> processings : postProcessings.entrySet()) {
            Assert.fail(processings.toString());
         }

      } catch (final IOException e) {
         e.printStackTrace();
      }
   }

   // FIXME!
   @Ignore
   @Test
   public void testLSTFile() throws IOException {
      final String testURI = ServerTestUtils.OPTIMISATION_URI + "/" + job.getId() + "/0/0/lstfile";
      LOG.info("Results-URI: " + testURI);
      final Response response = RESTCaller.callGetResponse(testURI);
      final byte[] serverLSTReturn = response.readEntity(byte[].class);
      LOG.debug("Auslese-Aufruf beendet, lese {}", job.getListingFile(0, 0));

      String lstfile = "";
      try {
         final byte[] encoded = Files.readAllBytes(job.getListingFile(0, 0).toPath());
         lstfile = new String(encoded, StandardCharsets.UTF_8);
      } catch (final IOException e) {
         e.printStackTrace();
         Assert.fail();
      }
      final GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(serverLSTReturn));
      final BufferedReader bf = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
      String outStr = "";
      String line;
      while ((line = bf.readLine()) != null) {
         outStr += line;
      }
      Assert.assertEquals(lstfile, outStr);
   }

   @Test
   public void testGDXEqualityRequest() throws JsonParseException, JsonMappingException, IOException {
      final String testURI = ServerTestUtils.OPTIMISATION_URI + "/" + job.getId() + "/0/0/gdxparameterfile";
      LOG.info("Results-URI: " + testURI);
      final Response response = RESTCaller.callGetResponse(testURI);
      final byte[] serverGDXReturn = IOUtils.toByteArray((InputStream) response.getEntity());
      LOG.debug("Simulationslauf beendet");

      byte[] realgdxparameter = null;
      final File gdxFile = job.getGDXParameterFile(0, 0);
      try (FileInputStream inputStream = new FileInputStream(gdxFile)) {
         realgdxparameter = new byte[(int) gdxFile.length()];
         inputStream.read(realgdxparameter);
      } catch (final FileNotFoundException e) {
         e.printStackTrace();
      } catch (final IOException e) {
         e.printStackTrace();
      }

      Assert.assertArrayEquals(realgdxparameter, serverGDXReturn);
   }

   /**
    *
    */
   @Test
   public void testGDXResultRequest() throws JsonParseException, JsonMappingException, IOException {
      final String testURI = ServerTestUtils.OPTIMISATION_URI + "/" + job.getId() + "/0/0/gdxresultfile";
      LOG.info("Results-URI: " + testURI);
      final Response response = RESTCaller.callGetResponse(testURI);
      ServerTestUtils.testGDXEquality(response, job);
   }

}
