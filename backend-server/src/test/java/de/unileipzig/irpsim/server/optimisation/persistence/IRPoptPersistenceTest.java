package de.unileipzig.irpsim.server.optimisation.persistence;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Scanner;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.ws.rs.core.Response;

import de.unileipzig.irpsim.core.Constants;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsCollectionWithSize;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.unileipzig.irpsim.core.data.timeseries.DatabaseConnectionHandler;
import de.unileipzig.irpsim.core.simulation.data.GenericParameters;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
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
public final class IRPoptPersistenceTest extends ServerTests {

	private static final Logger LOG = LogManager.getLogger(IRPoptPersistenceTest.class);

	private static Job job;

	/**
	 * @throws SQLException
	 *
	 */
   @BeforeClass
   public static void initialize() throws JsonParseException, JsonMappingException, IOException, SQLException {
      try (Connection connection = DatabaseConnectionHandler.getInstance().getConnection()){
         final Statement statement = connection.createStatement();
         statement.executeUpdate("TRUNCATE TABLE " + OptimisationJobPersistent.class.getSimpleName());
      }

      Job.deleteAfterwards = false;
      job = executeSimpleRequest();
      LOG.debug("Job {} wird berechnet!", job.getId());
      ServerTestUtils.waitForSimulationEnd(job.getId());
   }

	/**
	 * @return den gestarteten {@link Job}
	 */
   public static Job executeSimpleRequest() {
      try {
         final String content = DatabaseTestUtils.getParameterText(TestFiles.DAYS_3.make());
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
		final File csvFile2 = job.getCSVFile(0, 0);
		Assert.assertTrue(csvFile2.exists());
		try (final Scanner scanner = new Scanner(csvFile2)) {
			csvfile = scanner.useDelimiter("\\Z").next() + "\n";
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		}

		LOG.debug("Ende Request: {}", StringEscapeUtils.escapeJava(csvRequestResults.substring(Math.max(csvRequestResults.length() - 100, 0))));
		LOG.debug("Ende Echt: {}", StringEscapeUtils.escapeJava(csvfile.substring(Math.max(csvfile.length() - 100, 0))));
		assertEquals(csvfile, csvRequestResults);
	}

	/**
	 *
	 */
	@Test
	public void testResults() throws JsonProcessingException {
		final String testURI = ServerTestUtils.OPTIMISATION_URI + "/" + job.getId() + "/results";
		LOG.info("Results-URI: " + testURI);
		final Response response = RESTCaller.callGetResponse(testURI);
		final String jsonServerResults = response.readEntity(String.class);

		final GenericParameters results = job.getResults();
		final JSONParametersMultimodel realJSONResult = (JSONParametersMultimodel) results.createJSONParameters();
		assertEquals(Constants.MAPPER.writeValueAsString(realJSONResult), jsonServerResults);
	}

	/**
	 *
	 */
	@Test
	public void testLSTFile() {
		final String testURI = ServerTestUtils.OPTIMISATION_URI + "/" + job.getId() + "/0/0/lstfile";
		LOG.info("Results-URI: " + testURI);
		final Response response = RESTCaller.callGetResponse(testURI);
		final String serverLSTReturn = response.readEntity(String.class);
		LOG.info(serverLSTReturn != null ? serverLSTReturn.substring(0, 1000) : "null");
		Assert.assertNotNull(serverLSTReturn);
		assertEquals(HttpStatus.OK_200.getStatusCode(), response.getStatus());
	}

	/**
	 *  @Url https://www.boraji.com/hibernate-5-criteria-query-example
	 *  @url https://teamtreehouse.com/community/the-sessions-method-createcriteria-is-deprecated-how-should-i-proceed-for-establishing-class-criteria
	 */
	@Test
	public void testPersistedSize() {
		try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final Session session = (Session) em.getDelegate();
			@SuppressWarnings("unchecked")
			CriteriaBuilder builder = session.getCriteriaBuilder();
			//Create Criteria
			CriteriaQuery<OptimisationJobPersistent> criteria = builder.createQuery(OptimisationJobPersistent.class);
			Root<OptimisationJobPersistent> optimisationJobPersistentRoot = criteria.from(OptimisationJobPersistent.class);
			criteria.select(optimisationJobPersistentRoot);

			//Use criteria to query with session to fetch all Jobs
			final List listnew = session.createQuery(criteria).getResultList();
			// can be removed after Review
			final List<OptimisationJobPersistent> list = session.createCriteria(OptimisationJobPersistent.class).list();
			assertEquals(list.size(), listnew.size());
			assertEquals(list, listnew);
			MatcherAssert.assertThat(list, IsCollectionWithSize.hasSize(1));
		}
	}

	/**
	 *
	 */
	@Test
	public void testGDXParameter() throws JsonParseException, JsonMappingException, IOException {
		final String testURI = ServerTestUtils.OPTIMISATION_URI + "/" + job.getId() + "/0/0/gdxparameterfile";
		LOG.info("Results-URI: " + testURI);
		final Response response = RESTCaller.callGetResponse(testURI);
		final byte[] serverGDXReturn = IOUtils.toByteArray((InputStream) response.getEntity());
		LOG.debug("Simulationslauf beendet");

		byte[] realgdxparameter = null;
		try (final FileInputStream inputStream = new FileInputStream(job.getGDXParameterFile(0, 0))) {
			realgdxparameter = new byte[(int) job.getGDXParameterFile(0, 0).length()];
			inputStream.read(realgdxparameter);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		Assert.assertArrayEquals(realgdxparameter, serverGDXReturn);
	}

	@Test
	public void testGDXResult() throws IOException {
		final String testURI = ServerTestUtils.OPTIMISATION_URI + "/" + job.getId() + "/0/0/gdxresultfile";
		LOG.info("Results-URI: " + testURI);
		final Response response = RESTCaller.callGetResponse(testURI);
		ServerTestUtils.testGDXEquality(response, job);
	}
}
