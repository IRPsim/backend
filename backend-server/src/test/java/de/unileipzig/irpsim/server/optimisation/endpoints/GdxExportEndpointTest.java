package de.unileipzig.irpsim.server.optimisation.endpoints;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.unileipzig.irpsim.core.data.simulationparameters.GdxConfiguration;
import de.unileipzig.irpsim.core.data.timeseries.DatabaseConnectionHandler;
import de.unileipzig.irpsim.core.testutils.DatabaseTestUtils;
import de.unileipzig.irpsim.server.marker.RESTTest;
import de.unileipzig.irpsim.server.utils.MockUtils;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;

/**
 * Testet die GDX-Konfiguration Export-Schnittstelle.
 *
 * @author kluge
 */
@Category(RESTTest.class)
public final class GdxExportEndpointTest extends ServerTests {

   private static final ObjectMapper MAPPER = new ObjectMapper();
   private static final Logger LOG = LogManager.getLogger(GdxExportEndpointTest.class);

   private static final String EXAMPLEDATA = "{\"years\": [{\"config\":{\"simulationlength\":8736,\"savelength\":0,\"timestep\":0.0,\"modell\":\"Basismodell\",\"year\":null},"
         + "\"scalars\":{\"sca_VarDemandMax\":300.0,\"sca_FixDemand\":300.0,\"sca_StatCost\":0.0,\"sca_Cop_stat\":3.0},\"sets\":{},"
         + "\"timeseries\":{" + "\"par_VarDemand\":[1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20],"
         + "\"par_SpotForecast\":[3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22]},\"tables\":{}" + "} ]}";
   private static final String EXAMPLE_REQUEST = "{\"name\":\"TEest\",\"creator\":\"DGR\","
         + "\"data\":"
         + "{\"selParameters\":[{\"id\":471,\"label\":\"var_energyFlow\",\"year\":\"0\",\"tagLabel\":\"var_energyFlow [2015]\"}],"
         + "\"selYears\":[{\"label\":\"2015\",\"id\":0}],\"parameter\":{\"type\":\"timeseries\"},"
         + "\"exportConfiguration\":{\"0\":"
         + "{\"var_energyFlow\":[{\"label\":\"var_energyFlow\",\"isEditing\":false,\"isValid\":true,\"hasOnlyOneValue\":false,"
         + "\"sets\":{\"set_sector\":[{\"disabled\":true,\"value\":\"E\",\"items\":[\"E\"]}],"
         + "\"set_fromPss\":[{\"disabled\":true,\"value\":\"EGrid\",\"items\":[\"EGrid\"]}],"
         + "\"set_toPss\":[{\"disabled\":true,\"value\":\"load_E1\",\"items\":[\"load_E1\"]}]},"
         + "\"combinationCount\":1,\"autoFillIndex\":0,\"setIi\":\"Viertelstundenwerte\"}]}}}}";
   private static final String EXAMPLEPARAMETERSET = "{\"name\": \"testConfig\", \"creator\": \"testCreator\", \"data\": " + EXAMPLEDATA + " }";

   private static final String FAILREQUEST = "{\"name\": \"testConfig\",\"dummy\":\"bar\"}";

   private long queryID;

   @After
   public void tearDown() {
      DatabaseTestUtils.cleanUp();
   }

   @Before
   public void setUp() throws SQLException {
      queryID = MockUtils.createSimpleResults();
   }
   
   @Test
   public void testEqualOrder() throws JsonProcessingException {
      Response response = RESTCaller.callPutResponse(ServerTestUtils.GDX_URI, EXAMPLE_REQUEST);
      Assert.assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
      String idString = response.readEntity(String.class);
      final int id = Integer.parseInt(idString);
      Assert.assertTrue(id > 0);
      
      String configurations = RESTCaller.callGet(ServerTestUtils.GDX_URI);
      System.out.println("get RESULT: " + configurations);
      JsonObject sets = getSets(configurations);
      
      LOG.debug("Sets: {}", sets);
      
      int set_sector = sets.toString().indexOf("set_sector");
      int set_fromPss = sets.toString().indexOf("set_fromPss");
      int set_toPss = sets.toString().indexOf("set_toPss");
      
      Assert.assertTrue(set_sector < set_fromPss);
      Assert.assertTrue(set_fromPss < set_toPss);
   }

   private JsonObject getSets(String configurations) {
      JsonObject object = new Gson().fromJson(configurations, JsonObject.class);
      JsonObject exportConfig = object.getAsJsonObject("1").getAsJsonObject("data").getAsJsonObject("exportConfiguration").getAsJsonObject("0");
      JsonArray energyFlow = exportConfig.getAsJsonArray("var_energyFlow");
      JsonObject sets = energyFlow.get(0).getAsJsonObject().getAsJsonObject("sets");
      return sets;
   }
   
   @Test
   public void createWrongPutConfigurations() throws JsonProcessingException {
      Response response = RESTCaller.callPutResponse(ServerTestUtils.GDX_URI, FAILREQUEST);
      Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
   }

   @Test
   public void createExportConfigurations() throws JsonProcessingException {
      Response response = RESTCaller.callPutResponse(ServerTestUtils.GDX_URI, EXAMPLEPARAMETERSET);
      Assert.assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
      String idString = response.readEntity(String.class);
      final int id = Integer.parseInt(idString);
      Assert.assertTrue(id > 0);
   }

   @Test
   public void getNoExportConfigurationsFormDB() {
      try (final Connection connection = DatabaseConnectionHandler.getInstance().getConnection();
            final Statement st = connection.createStatement();
            final ResultSet conf = st.executeQuery(
                  "SELECT * FROM " + GdxConfiguration.class.getSimpleName() + " WHERE id=" + queryID)) {

         GdxConfiguration gdxConf = null;
         while (conf.next()) {
            gdxConf = new GdxConfiguration();
            gdxConf.setId(conf.getInt(1));
            gdxConf.setCreator(conf.getString(2));
            gdxConf.setData(conf.getString(3));
            gdxConf.setName(conf.getString(4));

            System.out.println(gdxConf);

            Assert.assertNull(gdxConf);
         }
      } catch (final SQLException e) {
         e.printStackTrace();
      }
   }

   @Test
   public void getExportConfigurations() {
      Response response = RESTCaller.callPutResponse(ServerTestUtils.GDX_URI, EXAMPLEPARAMETERSET);
      Assert.assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());

      Response response2 = RESTCaller.callGetResponse(ServerTestUtils.GDX_URI);
      Assert.assertEquals(Response.Status.OK.getStatusCode(), response2.getStatus());

      try (final Connection connection = DatabaseConnectionHandler.getInstance().getConnection()) {
         try (final Statement st = connection.createStatement();
               final ResultSet conf = st.executeQuery(
                     "SELECT * FROM " + GdxConfiguration.class.getSimpleName() + " WHERE id=" + queryID)) {

            GdxConfiguration gdxConf = null;
            while (conf.next()) {
               gdxConf = new GdxConfiguration();
               gdxConf.setId(conf.getInt(1));
               gdxConf.setCreator(conf.getString(2));
               gdxConf.setData(conf.getString(3));
               gdxConf.setName(conf.getString(4));

               LOG.debug("Config: " + gdxConf);
            }

            Assert.assertNotNull(gdxConf);

            GdxConfiguration gdxConfiguration = MAPPER.readValue(EXAMPLEPARAMETERSET, GdxConfiguration.class);
            String idString = response.readEntity(String.class);
            final int id = Integer.parseInt(idString);
            gdxConfiguration.setId(id);
            gdxConfiguration.setData(" " + EXAMPLEDATA + " ");

            Assert.assertEquals(gdxConfiguration.getId(), gdxConf.getId());
            Assert.assertEquals(gdxConfiguration.getName(), gdxConf.getName());
            Assert.assertEquals(gdxConfiguration.getCreator(), gdxConf.getCreator());
            Assert.assertEquals(gdxConfiguration.getData(), gdxConf.getData());
         } catch (JsonProcessingException e) {
            e.printStackTrace();
         }
      } catch (final SQLException e) {
         e.printStackTrace();
      }
   }

   @Test
   public void deleteExportConfigurations() {
      // Put
      Response response = RESTCaller.callPutResponse(ServerTestUtils.GDX_URI, EXAMPLEPARAMETERSET);
      Assert.assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
      String idString = response.readEntity(String.class);
      final int id = Integer.parseInt(idString);

      // Delete 1st
      response = RESTCaller.callDeleteResponse(ServerTestUtils.GDX_URI + "/" + id);
      Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

      // Delete 2nd time
      response = RESTCaller.callDeleteResponse(ServerTestUtils.GDX_URI + "/" + id);
      String entity = response.readEntity(String.class);

      JSONObject jsonObject = new JSONObject(entity);
      JSONArray jsonArray = jsonObject.getJSONArray("messages");
      JSONObject text = jsonArray.getJSONObject(0);

      Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
      Assert.assertEquals("Konfiguration ist nicht vorhanden", text.getString("text"));
   }

}
