package de.unileipzig.irpsim.server.optimisation.endpoints;

import static de.unileipzig.irpsim.server.data.Responses.badRequestResponse;
import static de.unileipzig.irpsim.server.data.Responses.errorResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.persistence.EntityTransaction;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.unileipzig.irpsim.core.data.simulationparameters.GdxConfiguration;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Endpunkt für das Speichern / Laden einer GDX-Konfiguration.
 */
@Path("/exportconfigurations")
@Api(value = "/exportconfigurations", tags = "GDX")
public class GdxExportEndpoint {

   private static final Logger LOG = LogManager.getLogger(GdxExportEndpoint.class);
   private static final ObjectMapper MAPPER = new ObjectMapper();

   private static String getDataString(String parameterstring) {
      final int startIndex = parameterstring.indexOf("\"data\"");
      final int stopIndex = parameterstring.lastIndexOf("}");
      final String parameters = parameterstring.substring(startIndex + 7, stopIndex);
      return parameters;
   }

   @PUT
   @Produces(MediaType.APPLICATION_JSON)
   @Consumes(MediaType.APPLICATION_JSON)
   @ApiOperation(value = "Fügt die Exportierten Konfigurationen zu einer Datenbank hinzu", notes = "Fügt den übergebenen Parametersatz in die Datenbank ein. "
         + "Dabei werden Metadaten, wie Erstellungsdatum und Modelltyp, automatisch generiert.")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Bad Request") })
   public final Response createExportConfigurations(final String configurationParameters) {
      GdxConfiguration gdxConfiguration;
      try {
         gdxConfiguration = MAPPER.readValue(configurationParameters, GdxConfiguration.class);
      } catch (final IOException e) {
         e.printStackTrace();
         return badRequestResponse(e, "Parameter anlegen");
      }

      String dataString = getDataString(configurationParameters);
      gdxConfiguration.setData(dataString);

      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         LOG.debug("Beginne Persistieren");
         final EntityTransaction et = em.getTransaction();
         et.begin();
         em.persist(gdxConfiguration);
         em.flush();
         et.commit();
         LOG.debug("Persistieren Abgeschlossen, gespeichert unter " + gdxConfiguration.getId());
         return Response.accepted(MAPPER.writeValueAsString(gdxConfiguration.getId())).build();
      } catch (Exception e) {
         e.printStackTrace();
         return badRequestResponse(e, "Parameter anlegen");
      }
   }

   /**
    * Liest alle GDX Konfigurationen aus und gibt sie inklusive ihrer Metadaten zurück.
    *
    * @return Die GDX Konfigurationen und ihre Metadaten
    *
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @ApiOperation(value = "Liefert GDX Konfigurationen zurück", notes = "Liest alle GDX Konfigurationen aus und gibt sie inklusive ihrer Metadaten zurück.")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Bad Request") })
   public Response getExportConfigurations() {
      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final Session s = em.unwrap(Session.class);
         Map<Integer, GdxConfiguration> dataMap = getConfigurations(s);

         for (GdxConfiguration d : dataMap.values()) {
            System.out.println(d);
         }

         String metadataString = "";
         try {
            metadataString = MAPPER.writeValueAsString(dataMap);
            metadataString = appendUnknownJsonDataStructure(metadataString, dataMap);

         } catch (final JsonProcessingException e1) {
            e1.printStackTrace();
         }
         if (metadataString.length() > 100) {
            LOG.debug("Gebe Szenarien zurück: {}", metadataString.substring(0, 100));
         }
         return Response.ok(metadataString).build();
      } catch (final Exception e) {
         LOG.error(e);
         e.printStackTrace();
         return errorResponse(e, "parametersets");
      }

   }

   /**
    * Erweitert den [metadataString] um die noch nicht genau definierten Data Objekte.
    * 
    * @param metadataString gespeicherte Konfigurationen
    * @param dataMap aus Datenbank geladene Konfigurationen
    * @return Serialisierte JsonObjekte mit Sub Json Daten Blöcken.
    */
   private String appendUnknownJsonDataStructure(String metadataString, Map<Integer, GdxConfiguration> dataMap) {

      final JsonObject jso = JsonParser.parseString(metadataString).getAsJsonObject();
      for (final Object key : jso.keySet()) {
         final JsonObject value = (JsonObject) jso.get((String) key);
         Optional<GdxConfiguration> conf = dataMap.entrySet().stream()
               .filter(entry -> entry.getKey().toString().equals(key.toString())).map(Map.Entry::getValue)
               .findFirst();
         if (conf.isPresent()) {
            JsonObject data = JsonParser.parseString(conf.get().getData()).getAsJsonObject();
            value.add("data", data);
         } else {
            LOG.error("no Data to Add to Configuration " + key);
         }
      }
      return jso.toString();
   }

   /**
    * Lädt Konfigurationen aus der Datenbank
    * 
    * @param session der Datenbankverbindung
    * @return Map<Id, Konfiguration>
    */
   private Map<Integer, GdxConfiguration> getConfigurations(Session session) {
      List<GdxConfiguration> data = session.createQuery(
            "SELECT a FROM GdxConfiguration a", GdxConfiguration.class).getResultList();

      LOG.error(data.size());

      final Map<Integer, GdxConfiguration> dataMap = new HashMap<>();

      for (GdxConfiguration d : data) {
         dataMap.put(d.getId(), d);
      }
      return dataMap;
   }

   @DELETE
   @Produces(MediaType.APPLICATION_JSON)
   @Path("/{id}")
   @ApiOperation(value = "Löscht Parameter nach einer bestimmen Id", notes = "Prüft, ob der Parametersatz mit der übergebenen Id löschbar ist, und löscht ihn, falls er löschbar ist.")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Bad Request") })
   public final Response deleteExportConfigurations(@PathParam("id") final int id) {
      LOG.info("Lade Parameter für: {}", id);
      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final Session s = (Session) em.getDelegate();
         final GdxConfiguration gdxC = s.get(GdxConfiguration.class, id);

         if (gdxC != null) {
            LOG.debug("Löschbar: {}", gdxC);
            em.getTransaction().begin();
            s.delete(gdxC);
            em.getTransaction().commit();
            return Response.ok().build();
         } else {
            LOG.debug("Konfiguration ist nicht vorhanden");
            return badRequestResponse("Konfiguration ist nicht vorhanden", "Löschen");
         }
      } catch (final Throwable e) {
         e.printStackTrace();
         return errorResponse(e, "Löschen");
      }
   }
}
