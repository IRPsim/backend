package de.unileipzig.irpsim.server.javamodel;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.json.UserDefinedDescription;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.core.utils.PersistenceFolderUtil;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.server.modelstart.JavaModelStarter;
import de.unileipzig.irpsim.server.optimisation.persistence.OptimisationJobPersistenceManager;

@Ignore
public class TestModelStart {
   
   private static final int IRPACT_ID = 3;
   private static final File inputFile = new File("src/main/resources/scenarios/3.json");

   private OptimisationJobPersistent job = new OptimisationJobPersistent();
   
   private JSONParametersMultimodel parameters;
   
   @Before
   public void initDB() throws SQLException, JsonParseException, JsonMappingException, IOException {
      DatabaseTestUtils.setupDbConnectionHandler();
      
      TestFiles.TEST_PERSISTENCE_FOLDER.mkdirs();
      PersistenceFolderUtil.setPersistenceFolder(TestFiles.TEST_PERSISTENCE_FOLDER);
      
      parameters = Constants.MAPPER.readValue(inputFile, JSONParametersMultimodel.class);
   }
   
   @Test
   public void testBasicModelExecution() throws IOException, InterruptedException {
      
      createPersistentJob(inputFile);
      
      BackendParametersYearData year = new BackendParametersYearData(parameters.getModels().get(0).getYears().get(0));
      JavaModelStarter starter = new JavaModelStarter(job.getId(), 0, IRPACT_ID, year);
      
      starter.parameterize();
      
      starter.startOptimisation();
      
      Assert.assertNotNull(starter.getOptimizeLog());
   }

   private void createPersistentJob(File inputFile) throws IOException {
      job.setDescription(new UserDefinedDescription());
      try {
         
         job.setJsonParameter(Constants.MAPPER.writeValueAsString(parameters));
      } catch (final JsonProcessingException e1) {
         e1.printStackTrace();
      }
      @SuppressWarnings("unused")
      OptimisationJobPersistenceManager manager = new OptimisationJobPersistenceManager(job);
   }
}
