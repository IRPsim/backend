package de.unileipzig.irpsim.server.endpoints;

import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.unileipzig.irpsim.core.data.timeseries.DatabaseConnectionHandler;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationYearPersistent;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.server.utils.MockUtils;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;

/**
 * Testet, ob alte Simulationsjobs mit dem DELETE Endpoint gelöscht werden können.
 *
 * @author krauss
 */
public final class DeleteOldJobTest extends ServerTests {

   private static final Logger LOG = LogManager.getLogger(DeleteOldJobTest.class);
   private long jobId;

   /**
    * Läuft nur, wenn auch irgend ein Job in der Testdatenbank vorhanden ist.
    */
   @Before
   public void checkForJobs() throws SQLException {
      jobId = MockUtils.createSimpleResults();
   }

   /**
    *
    */
   @Test
   public void testDelete() {
      LOG.debug("Starte testDelete, zu löschender Job: {}", jobId);
      final Response callDeleteResponse = RESTCaller.callDeleteResponse(ServerTestUtils.OPTIMISATION_URI + "/" + jobId, true);
      LOG.debug(callDeleteResponse.readEntity(String.class));

      Assert.assertEquals(callDeleteResponse.getStatus(), 200);

      try (final Connection connection = DatabaseConnectionHandler.getInstance().getConnection();
            final Statement st = connection.createStatement()) {
         final ResultSet job = st.executeQuery("SELECT * FROM " + OptimisationJobPersistent.class.getSimpleName() + " WHERE id=" + jobId);
         boolean hasNextJob = job.next();
         Assert.assertFalse("Job vorhanden:" + hasNextJob, hasNextJob);
         
         final ResultSet year = st.executeQuery("SELECT * FROM " + OptimisationYearPersistent.class.getSimpleName() + " WHERE job_id=" + jobId);
         boolean hasNextYear = year.next();
         Assert.assertFalse("Jahr vorhanden:" + hasNextYear, hasNextYear);

      } catch (SQLException e) {
         e.printStackTrace();
      }

      Assert.assertFalse(Files.exists(TestFiles.TEST_PERSISTENCE_FOLDER.toPath().resolve("" + jobId)));
   }
}
