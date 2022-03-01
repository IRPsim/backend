package de.unileipzig.irpsim.server.endpoints;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.FileNotFoundException;

public class GraphvizEndpointTest extends ServerTests {

   private static final Logger LOG = LogManager.getLogger(GraphvizEndpointTest.class);

   @Test
   public void getGraph() throws FileNotFoundException, JsonProcessingException {

      LOG.debug("uri {}", ServerTestUtils.URI + "graphviz/initGraphPNG");

      final String parameters = DatabaseTestUtils.getParameterText(TestFiles.IRPACT.make());

      JSONParametersMultimodel multiModel = Constants.MAPPER.readValue(parameters, JSONParametersMultimodel.class);

      String scenario = Constants.MAPPER.writeValueAsString(multiModel.getModels().get(0));
      Response response = RESTCaller.callPost(ServerTestUtils.URI + "graphviz/initGraphPNG", scenario);
      LOG.debug("response: {}",response);

      Assert.assertEquals(200, response.getStatus());
      Assert.assertEquals(MediaType.valueOf("image/png") , response.getMediaType());

   }
}