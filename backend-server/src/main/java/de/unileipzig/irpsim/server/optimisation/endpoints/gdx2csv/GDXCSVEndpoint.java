package de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.server.data.Responses;
import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading.DataMapReader;
import de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv.loading.GDXSQLiteData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Liefert Informationen über GDX-Ergebnisdateien aus
 * 
 * @author reichelt
 *
 */
@Path("simulations")
@Api(value = "/simulations/{simulationid}", tags = "GDX-CSV-Schnittstelle")
@Produces({ "application/json" })
public class GDXCSVEndpoint {

   private static final ObjectMapper MAPPER = new ObjectMapper();
   private static final Logger LOG = LogManager.getLogger(GDXCSVEndpoint.class);

   /**
    * Gibt die Parameter, die in einer GDX definiert wurden, zurück
    *
    * @param year Das Jahr der spezifischen Simulation
    * @param simulationid Die ID des zurückzugebenden Simulationslaufes
    * @return Die GDX-Ergebnisdatei oder eine Fehlermeldung
    */
   @Path("/{simulationid}/{year}/{modelindex}/gdxcsvparameters/")
   @GET
   @ApiOperation(value = "Gibt die Parameter einer GDX-Datei zurück", notes = "Gibt die Parameter einer GDX-Datei zurück")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Unerwartete Id übergeben") })
   @Produces(MediaType.APPLICATION_JSON)
   public final Response getGDXParameters(@PathParam("simulationid") final int simulationid, @PathParam("year") final int year, @PathParam("modelindex") final int modelindex) {
      try {
         LOG.debug("GDX-Beschreibung geladen");
         GDXSQLiteData data = CSVDataCache.getData(simulationid, year, modelindex);

         final Map<String, ParametermetaData> parameterDependentMap = data.getData();
         final String result = MAPPER.writeValueAsString(parameterDependentMap);
         return Response.ok(result).build();
      } catch (final Throwable e) {
         e.printStackTrace();
         return Responses.errorResponse(e.getMessage());
      }
   }

   /**
    * Gibt für die übergebenen Parameter die Komma-Separierten-Werte zurück
    *
    * @param year Das Jahr der spezifischen Simulation
    * @param simulationid Die ID des zurückzugebenden Simulationslaufes
    * @return Die GDX-Ergebnisdatei oder eine Fehlermeldung
    */
   @Path("/gdxresultfile/csvalues/")
   @PUT
   @ApiOperation(value = "Gibt die CSV-Werte zu einer GDX zurück", notes = "Gibt die CSV-Werte zu einer GDX zurück")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Unerwartete Id übergeben") })
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.TEXT_PLAIN)
   public final Response getGDXCSValues(final List<RequestedParametersYear> requestedParameters) {
      try {
         final DataMapReader dataMapReader = new DataMapReader(requestedParameters);
         final String result = new CSVWriter(dataMapReader, requestedParameters).write();
         return Response.ok(result).build();
      } catch (final Throwable e) {
         e.printStackTrace();
         return Responses.errorResponse(e.getMessage());
      }
   }

   @Path("/gdxresultfile/gegenueberstellung/")
   @PUT
   @ApiOperation(value = "Gibt die Gegenüberstellung-Visualisierung einer CSV zurück", notes = "Erstellt Gegenüberstellung-Visualisierung")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Unerwartete Id übergeben") })
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_OCTET_STREAM)
   public final Response getGegenueberstellung(final List<RequestedParametersYear> requestedParameters) {
      try {
         LOG.debug(requestedParameters);
         final String data = new GegenueberstellungWriter(requestedParameters).write(); // data can be obtained from an input stream too.
         final InputStream plotFile = GDXCSVEndpoint.class.getResourceAsStream("/visualisierungen/gegenueberstellung.plt");
         StreamingOutput streamingOutput = WritingUtil.writeZIP(data, plotFile);

         ResponseBuilder response = Response.ok(streamingOutput);
         response.header("content-disposition", "attachment; filename=\"data.zip\"");
         LOG.debug("Returning {}", data.length());
         return response.build();
      } catch (final Throwable e) {
         e.printStackTrace();
         return Responses.errorResponse(e.getMessage());
      }
   }

   @Path("/gdxresultfile/gegenueberstellung/png/")
   @PUT
   @ApiOperation(value = "Gibt das PNG einer Gegenüberstellung-Visualisierung zurück", notes = "Erstellt PNG von Gegenüberstellung-Visualisierung")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Unerwartete Id übergeben") })
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces("image/png")
   public final Response getGegenueberstellungPNG(final List<RequestedParametersYear> requestedParameters) {
      try {
         LOG.debug(requestedParameters);
         final String data = new GegenueberstellungWriter(requestedParameters).write();
         File tempFolder = WritingUtil.createFiles(data, "gegenueberstellung.plt");

         Process process = WritingUtil.runGnuplotProcess(tempFolder, "gegenueberstellung.plt");
         if (process.isAlive()) {
            return Responses.errorResponse("Dateischreiben nicht erfolgreich abgeschlossen.");
         } else {
            byte[] imageData = WritingUtil.getImageData(tempFolder);
            return Response.ok(imageData).build();
         }
      } catch (final Throwable e) {
         e.printStackTrace();
         return Responses.errorResponse(e.getMessage());
      }
   }

   @Path("/gdxresultfile/kostenumsatz/")
   @PUT
   @ApiOperation(value = "Gibt die Kostenumsatz-Visualisierung einer CSV zurück", notes = "Erstellt Kostenumsatz-Visualisierung")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Unerwartete Id übergeben") })
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_OCTET_STREAM)
   public final Response getKostenumsatz(final List<RequestedParametersYear> requestedParameters) {
      try {
         LOG.debug(requestedParameters);
         final String data = new KostenUmsatzWriter(new DataMapReader(requestedParameters), requestedParameters).write(); // data can be obtained from an input stream too.
         final InputStream plotFile = GDXCSVEndpoint.class.getResourceAsStream("/visualisierungen/kostenumsatz.plt");
         StreamingOutput streamingOutput = WritingUtil.writeZIP(data, plotFile);

         ResponseBuilder response = Response.ok(streamingOutput);
         response.header("content-disposition", "attachment; filename=\"data.zip\"");
         LOG.debug("Returning {}", data.length());
         return response.build();
      } catch (final Throwable e) {
         e.printStackTrace();
         return Responses.errorResponse(e.getMessage());
      }
   }

   @Path("/gdxresultfile/kostenumsatz/png/")
   @PUT
   @ApiOperation(value = "Gibt das PNG einer Kostenumsatz-Visualisierung zurück", notes = "Erstellt PNG von Kostenumsatz-Visualisierung")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"),
         @ApiResponse(code = 400, message = "Unerwartete Id übergeben") })
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces("image/png")
   public final Response getKostenumsatzPNG(final List<RequestedParametersYear> requestedParameters) {
      try {
         LOG.debug(requestedParameters);
         final String data = new KostenUmsatzWriter(new DataMapReader(requestedParameters), requestedParameters).write();
         File tempFolder = WritingUtil.createFiles(data, "kostenumsatz.plt");

         Process process = WritingUtil.runGnuplotProcess(tempFolder, "kostenumsatz.plt");

         if (process.isAlive()) {
            return Responses.errorResponse("Dateischreiben nicht erfolgreich abgeschlossen.");
         } else {
            byte[] imageData = WritingUtil.getImageData(tempFolder);
            return Response.ok(imageData).build();
         }
      } catch (final Throwable e) {
         e.printStackTrace();
         return Responses.errorResponse(e.getMessage());
      }
   }

   /**
    * Gibt für den Parameter set_ii alle möglichen Optionen zurück
    *
    * @return Ein Array mit allen Optionen
    */
   @Path("/gdxresultfile/options/")
   @GET
   @ApiOperation(value = "Gibt alle Optionen für set_ii zurück", notes = "Gibt alle Optionen für set_ii zurück")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok") })
   @Produces(MediaType.APPLICATION_JSON)
   public final Response getSetIiOptions() {
      try {
         final ObjectMapper mapper = MAPPER;
         final SetIiOptions options = new SetIiOptions();
         options.setOptions(new String[] {
               ParameterdataWriter.VIERTELSTUNDENWERTE,
               ParameterdataWriter.SUMME,
               ParameterdataWriter.DURCHSCHNITT,
               ParameterdataWriter.STUNDEN_DURCHSCHNITT,
               ParameterdataWriter.DURCHSCHNITTS_JAHRESWOCHE
         });

         final String result = mapper.writeValueAsString(options);
         return Response.ok(result).build();
      } catch (final Throwable e) {
         e.printStackTrace();
         return Responses.errorResponse(e.getMessage());
      }
   }
}
