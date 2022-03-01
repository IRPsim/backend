package de.unileipzig.irpsim.server.optimisation.endpoints;

import static de.unileipzig.irpsim.server.data.Responses.badRequestResponse;
import static de.unileipzig.irpsim.server.data.Responses.errorResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersMultiModel;
import de.unileipzig.irpsim.core.simulation.data.json.IntermediarySimulationStatus;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationYearPersistent;
import de.unileipzig.irpsim.core.simulation.data.persistence.State;
import de.unileipzig.irpsim.server.optimisation.Job;
import de.unileipzig.irpsim.server.optimisation.OptimisationJobUtils;
import de.unileipzig.irpsim.server.optimisation.queue.OptimisationJobHandler;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Bietet Funktionalitäten zum Abfruf von Optimierungsläufen.
 *
 * @author reichelt
 */
@Path("simulations")
@Api(value = "/simulations/{simulationid}", tags = "Simulation")
@Produces({ "application/json" })
public class OptimisationJobEndpoint { // TODO Umbenennen JobEndpoint

   private static final Logger LOG = LogManager.getLogger(OptimisationJobEndpoint.class);
   private static final ObjectMapper om = Constants.MAPPER;

   /**
    * Prüft, wie viele .csv-Dateien durch GAMS bereits abgelegt wurden und gibt den Simulationsstatus in Abhängigkeit davon aus.
    *
    * @param simulationid Die ID des Simulationslaufes.
    * @return Der Simulationsstatus in Abhängigkeit zu den von GAMS angelegten CSV-Dateien oder eine Fehlermeldung.
    * @throws JsonProcessingException Wird geworfen falls Probleme beim parsen oder generieren der JSON-Daten auftreten.
    */
   @Path("/{simulationid}")
   @GET
   @ApiOperation(value = "Gibt den Simulationsstatus des Simulationslaufs zurück", notes = "Prüft, wie viele .csv-Dateien durch GAMS bereits"
         + " abgelegt wurden und gibt den Simulationsstatus in Abhängigkeit davon aus.")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Unerwartete Id übergeben") })
   @Produces(MediaType.APPLICATION_JSON)
   public final Response getSimulationStatus(@PathParam("simulationid") final int simulationid) {
      try {
         LOG.debug("Lade Status für {}", simulationid);
         final Job job = OptimisationJobHandler.getInstance().getRunningJob(simulationid);
         LOG.trace("Jobs: " + OptimisationJobHandler.getInstance().getRunningJobs().size());
         if (job == null) {
            final OptimisationJobPersistent persistentJob = OptimisationJobHandler.getInstance().getJob(simulationid);
            if (persistentJob == null) {
               return badRequestResponse("Simulationslauf " + simulationid + " existiert nicht", "Simulationsstatus");
            } else {
               LOG.debug("Job: " + persistentJob + " " + persistentJob.getModelVersionHash());
               final String result = om.writeValueAsString(persistentJob.getOptimisationState());
               return Response.ok(result).build();
            }
         } else {
            if (!job.hasError()) {
               final IntermediarySimulationStatus iss = job.getIntermediaryState();
               LOG.info("Id: {}, State: {}, Description: {}", iss.getId(), iss.getState(), iss.getDescription().getBusinessModelDescription());
               final String result = om.writeValueAsString(iss);
               return Response.ok(result).build();
            } else {
               final OptimisationJobPersistent persistentJob = OptimisationJobHandler.getInstance().getJob(simulationid);
               final String result = om.writeValueAsString(persistentJob.getOptimisationState());
               return Response.ok(result).build();
            }
         }
      } catch (final Throwable e) {
         e.printStackTrace();
         return errorResponse(e, "Simulationsstatus");
      }
   }

   /**
    * Gibt die Zwischen- oder Endergebnisse des angefragten Laufs, entweder aus den .csv-Dateien oder der GAMS-Datenbank, zurück.
    *
    * @param simulationid Die ID des Simulationslaufes.
    * @return Zwischen- oder Endergebnisse des angefragten Laufs oder Fehlermeldung.
    */
   @Path("/{simulationid}/results")
   @GET
   @ApiOperation(value = "Gibt die Ergebnisse des angefragten Simulationslaufs zurück", notes = "Gibt die Zwischen- oder Endergebnisse des angefragten Laufs, entweder aus den .csv-Dateien oder "
         + "der GAMS-Datenbank, zurück.")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Unerwartete Id übergeben") })
   @Produces(MediaType.APPLICATION_JSON)
   public final Response getSimulationResults(@PathParam("simulationid") final int simulationid) {
      try {
         LOG.info("Endpoint! ID: {}", simulationid);
         final OptimisationJobPersistent persistentJob = OptimisationJobHandler.getInstance().getJob(simulationid);
         final Job job = OptimisationJobHandler.getInstance().getRunningJob(simulationid);
         if (persistentJob == null) {
            if (job == null) {
               return badRequestResponse("Simulationslauf " + simulationid + " existiert nicht", "Simulationsstatus");
            }
            final String result = om.writeValueAsString(job.getIntermediaryState());
            return Response.ok(result).build();
         } else {
            LOG.debug("Persistierter Job vorhanden, Status: " + persistentJob.getState());
            if (persistentJob.getState() == State.ERROR) {
               final String result = om.writeValueAsString(persistentJob.getOptimisationState());
               return Response.ok(result).build();
            }
            final String jsonString = persistentJob.getJsonResult();
            if (jsonString == null) {
               if (job != null) {
                  final BackendParametersMultiModel postProcessingResults = job.fetchPostProcessingResults();
                  if (postProcessingResults != null) {
                     final String results = om.writeValueAsString(postProcessingResults.createJSONParameters());
                     return Response.ok(results).build();
                  } else {
                     return errorResponse(
                           "Postprocessing was originally wanted by IWB, but is no longer required. It is left in single model jobs but will not be implemented for multi model jobs for now.",
                           "Error");
                  }
               } else {
                  LOG.error("Undefinierter Status: Persistierter Job hat kein Ergebnis, laufender Job exitiert jedoch nicht.");
                  return errorResponse("Undefinierter Status: Persistierter Job hat kein Ergebnis, laufender Job exitiert jedoch nicht.", "Fehler");
               }
            }
            final JSONParametersMultimodel results = om.readValue(jsonString, JSONParametersMultimodel.class);
            if (job != null) {
               if (job.getIntermediaryState().getState() == State.WAITING) {
                  final String result = om.writeValueAsString(job.getIntermediaryState());
                  return Response.ok(result).build();
               }
               final BackendParametersMultiModel postProcessingResults = job.fetchPostProcessingResults();
               OptimisationJobUtils.mergeCurrentResult(results, postProcessingResults);
               // TODO Reimplement merging for multimodel
            }
            final String jobresults = om.writeValueAsString(results);
            return Response.ok(jobresults).build();
         }
      } catch (final Throwable e) {
         e.printStackTrace();
         return errorResponse(e.getLocalizedMessage(), "Simulationsergebnis");
      }
   }

   /**
    * Gibt den Parametersatz, der aus der Datenbank geladen wurde, zurück.
    *
    * @param simulationid Die ID des Simulationslaufes.
    * @return Der geladene Parametersatz oder eine Fehlermeldung.
    */
   @Path("/{simulationid}/parameterset")
   @GET
   @ApiOperation(value = "Gibt den Parametersatz eines Simulationslaufs zurück", notes = "Gibt den Parametersatz, der aus der Datenbank geladen wurde, zurück")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Unerwartete Id übergeben") })
   @Produces(MediaType.APPLICATION_JSON)
   public final Response getSimulationParameterset(@PathParam("simulationid") final int simulationid) {
      try {
         LOG.info("Endpoint! ID: {}", simulationid);
         final OptimisationJobPersistent sj = OptimisationJobHandler.getInstance().getJob(simulationid);
         if (sj == null) {
            return badRequestResponse("Simulationslauf mit ID " + simulationid + "existiert nicht");
         }
         final String jsonString = sj.getJsonParameter();
         return Response.ok(jsonString).build();
      } catch (final Exception e) {
         e.printStackTrace();
         return errorResponse(e);
      }
   }

   /**
    * Beendet den Simulationslauf über die GAMS-API.
    *
    * @param simulationid Die ID des Simulationslaufes.
    * @param delete Bestimmt, ob eine gespeicherte Optimierung gelöscht werden soll.
    * @return Gibt die Üblichen Antworten zurück (200 für ok, 500 für Error)
    */
   @Path("/{simulationid}")
   @DELETE
   @Produces(MediaType.APPLICATION_JSON)
   @ApiOperation(value = "Beendet den aktiven Simulationslauf ODER Löscht den gepeicherten bereits beendeten Simulationsjob.", notes = "Beendet den Simulationslauf über die GAMS-API.")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "OK"),
         @ApiResponse(code = 500, message = "Interner Server Fehler (Beenden ist evtl. dennoch erfolgt)") })
   public final Response killSimulation(@PathParam("simulationid") final long simulationid,
         @DefaultValue("false") @QueryParam("delete") final boolean delete) {
      try {
         final Job sj = OptimisationJobHandler.getInstance().getRunningJob(simulationid);
         if (sj != null) {
            LOG.info("Stoppe aktiven SimulationJob: {}", simulationid);
            final boolean ret = sj.kill();
            return Response.ok(ret).build();
         } else {
            LOG.info("Kein laufender Job für Id {} gefunden", simulationid);
         }
         if (delete) {
            return deleteJob(simulationid);
         } else {
            return badRequestResponse("Laufender Simulationslauf mit ID " + simulationid + "existiert nicht");
         }
      } catch (final Exception e) {
         e.printStackTrace();
         return errorResponse(e);
      }

   }

   private Response deleteJob(final long simulationid) throws IOException, Exception {
      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final OptimisationJobPersistent sjp = em.merge(OptimisationJobHandler.getInstance().getJob(simulationid));
         if (sjp == null) {
            LOG.error("Simulationslauf mit ID " + simulationid + "existiert nicht");
            return badRequestResponse("Simulationslauf mit ID " + simulationid + "existiert nicht");
         }
         LOG.info("Lösche OptimisationYearPersistent: {}", simulationid);
         new JobFileCleaner(simulationid, sjp).clean();
         try {
            em.getTransaction().begin();
            for (final OptimisationYearPersistent year : sjp.getYears()) {
               LOG.debug("Lösche SimulationYearPersistent: {} (Job {})", year.getId(), simulationid);
               em.remove(year);
            }
            em.remove(sjp);
            em.getTransaction().commit();
         } catch (final Exception e) {
            e.printStackTrace();
            em.getTransaction().rollback();
            throw e;
         }
      }
      return Response.ok().build();
   }

   /**
    * Gibt die Listing-Datei des Simulationslaufs zu Debugging-Zwecken zurück.
    *
    * @param year Das Jahr.
    * @param simulationid Die ID des Simulationslaufes.
    * @return Die Lst Datei
    */
   @Path("/{simulationid}/{year}/{modelindex}/lstfile")
   @GET
   @ApiOperation(value = "Gibt die Listing-Datei des Simulationslaufs zurück", notes = "Gibt die Listing-Datei des Simulationslaufs zu Debugging-Zwecken zurück.")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Unerwartete Id übergeben") })
   @Produces({ MediaType.TEXT_PLAIN })
   public final Response getSimulationListingFile(@PathParam("simulationid") final int simulationid, @PathParam("year") final int year,
         @PathParam("modelindex") final int modelindex) {
      try {
         LOG.info("Endpoint! ID: {} Jahr: {}", simulationid, year);
         final OptimisationJobPersistent persistentJob = OptimisationJobHandler.getInstance().getJob(simulationid);
         if (persistentJob == null) {
            LOG.error("Simulationslauf mit ID " + simulationid + "existiert nicht");
            return badRequestResponse("Simulationslauf mit ID " + simulationid + "existiert nicht");
         }
         LOG.trace("SimulationJobPersistent: {}, Start: {}, End: {}", persistentJob.getId(), persistentJob.getStart(), persistentJob.getEnd());
         final OptimisationYearPersistent yearData = persistentJob.getYearDataOfSimulatedYear(year, modelindex);
         if (yearData == null) {
            return badRequestResponse("Stützjahr: " + year + " existiert nicht!");
         }
         final String listingFile = yearData.getLstFile();
         final File fileObject = new File(listingFile);
         if (!fileObject.exists()) {
            return badRequestResponse(listingFile);
         }
         LOG.trace("SimulationYearPersistent: {}, Listing-Datei: {}, SimulationJobPersistent: {}, Start: {}, End: {}", yearData.getId(), listingFile,
               yearData.getJob().getId(), yearData.getStart(), yearData.getEnd());
         final FileInputStream listingFileStream = new FileInputStream(listingFile);
         return Response.ok(listingFileStream).header("Content-Disposition", "attachment; filename=\"job-" + simulationid + "-jahr-" + yearData.getYear() + ".zip\"").build();
      } catch (final Exception e) {
         e.printStackTrace();
         return errorResponse(e);
      }
   }

   /**
    * Gibt die CSV-Ergebnisdatei des Simulationslaufs zu Debugging-Zwecken zurück.
    *
    * @param year Das Jahr der spezifischen Simulation.
    * @param simulationid Die ID des zurückzugebenden Simulationslaufes.
    * @return Die Endergebnisse als CSV-Datei oder eine Fehlermeldung.
    */
   @Path("/{simulationid}/{year}/{modelindex}/csvfile")
   @GET
   @ApiOperation(value = "Gibt die CSV-Ergebnisdatei des Simulationslaufs zurück", notes = "Gibt die CSV-Ergebnisdatei des Simulationslaufs zu Debugging-Zwecken zurück. "
         + "Hierfür werden die Endergebnis-CSV-Dateien aggregiert")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Unerwartete Id übergeben") })
   @Produces({ MediaType.TEXT_PLAIN })
   public final Response getSimulationCSVFile(@PathParam("simulationid") final int simulationid, @PathParam("year") final int year, @PathParam("modelindex") final int modelindex) {
      try {
         LOG.info("Endpoint CSVFile! ID: {}", simulationid);
         File response;
         final Job job = OptimisationJobHandler.getInstance().getRunningJob(simulationid);
         final int indexOfSimulatedYear;
         if (job == null) {
            final OptimisationJobPersistent persistentJob = OptimisationJobHandler.getInstance().getJob(simulationid);
            if (persistentJob == null) {
               return badRequestResponse("Simulationslauf mit ID " + simulationid + "existiert nicht");
            } else {
               final OptimisationYearPersistent yearData = persistentJob.getYearDataOfSimulatedYear(year, modelindex);
               if (yearData == null) {
                  return badRequestResponse("Stützjahr: " + year + " existiert nicht!");
               }
               indexOfSimulatedYear = yearData.getYear();
               final String gdxparameter = yearData.getCsvTotalData();
               if (gdxparameter != null) {
                  response = new File(gdxparameter);
               } else {
                  return errorResponse("CSV-Ergebnisdatei wurde nicht gespeichert", "Fehler");
               }

            }
         } else {
            indexOfSimulatedYear = job.fetchIndexOfSimulatedYear(year);
            if (indexOfSimulatedYear == -1) {
               return badRequestResponse("Stützjahr: " + year + " existiert nicht!");
            }
            response = job.getCSVFile(0, indexOfSimulatedYear);
         }
         if (response == null || !response.exists()) {
            return errorResponse("CSV-Ergebnisdatei wurde noch nicht definiert");
         }

         LOG.debug("Datei: " + response.getAbsolutePath());
         return Response.ok(response).header("Content-Disposition", "attachment; filename=\"job-" + simulationid + "-jahr-" + indexOfSimulatedYear + "-parameter.gdx\"").build();
      } catch (final Exception e) {
         e.printStackTrace();
         return errorResponse(e);
      }
   }

   /**
    * Gibt die CSV-Ergebnisdatei des Simulationslaufs zu Debugging-Zwecken zurück.
    *
    * @param year Das Jahr der spezifischen Simulation
    * @param simulationid Die ID des zurückzugebenden Simulationslaufes
    * @return Die Ergebnisse als Die Edergebnisse als CSV_Datei oder eine Fehlermeldung
    */
   @Path("/{simulationid}/{year}/{modelindex}/gdxparameterfile")
   @GET
   @ApiOperation(value = "Gibt die GDX-Parameterdatei des Simulationslaufs zurück", notes = "Gibt die GDX-Parameterdatei des angefragten Jahres des Simulationslaufs zu Debugging-Zwecken zurück. "
         + "Hierfür werden die Endergebnis-CSV-Dateien aggregiert")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Unerwartete Id übergeben") })
   @Produces({ MediaType.APPLICATION_OCTET_STREAM })
   public final Response getSimulationGDXParameterFile(@PathParam("simulationid") final int simulationid, @PathParam("year") final int year,
         @PathParam("modelindex") final int modelindex) {
      try {
         LOG.info("Endpoint GDX-Parameterfile! ID: {}", simulationid);
         File response;
         final Job job = OptimisationJobHandler.getInstance().getRunningJob(simulationid);
         if (job == null) {
            final OptimisationJobPersistent persistentJob = OptimisationJobHandler.getInstance().getJob(simulationid);
            if (persistentJob == null) {
               return badRequestResponse("Simulationslauf mit ID " + simulationid + "existiert nicht");
            } else {
               final OptimisationYearPersistent yearData = persistentJob.getYearDataOfSimulatedYear(year, modelindex);
               if (yearData == null) {
                  return badRequestResponse("Stützjahr: " + year + " existiert nicht!");
               }
               final String gdxparameter = yearData.getGdxparameter();
               if (gdxparameter != null) {
                  response = new File(gdxparameter);
               } else {
                  return errorResponse("GDX-Parameterdatei wurde nicht gespeichert", "Fehler");
               }

            }
         } else {
            final int indexOfSimulatedYear = job.fetchIndexOfSimulatedYear(year);
            if (indexOfSimulatedYear == -1) {
               return badRequestResponse("Stützjahr: " + year + " existiert nicht!");
            }
            response = job.getGDXParameterFile(0, indexOfSimulatedYear);
         }
         if (response == null || !response.exists()) {
            return errorResponse("GDX-Parameterdatei wurde noch nicht definiert");
         }

         LOG.debug("Datei: " + response.getAbsolutePath());
         String fileEnding = response.getName().substring(response.getName().lastIndexOf('.') + 1);
         return Response.ok(response).header("Content-Disposition", "attachment; filename=\"job-" + simulationid + "-jahr-" + year + "-parameter." + fileEnding + "\"").build();
      } catch (final Exception e) {
         e.printStackTrace();
         return errorResponse(e);
      }
   }

   /**
    * Gibt die GDX-Ergebnisdatei des Simulationslaufs zu Debugging-Zwecken zurück.
    *
    * @param year Das Jahr der spezifischen Simulation
    * @param simulationid Die ID des zurückzugebenden Simulationslaufes
    * @return Die GDX-Ergebnisdatei oder eine Fehlermeldung
    */
   @Path("/{simulationid}/{year}/{modelindex}/gdxresultfile")
   @GET
   @ApiOperation(value = "Gibt die GDX-Ergebnisdatei des Simulationslaufs zurück", notes = "Gibt die GDX-Ergebnisdatei des angefragten Jahres Simulationslaufs zu Debugging-Zwecken zurück. ")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Unerwartete Id übergeben") })
   @Produces({ MediaType.APPLICATION_OCTET_STREAM })
   public final Response getSimulationGDXResultFile(@PathParam("simulationid") final int simulationid, @PathParam("year") final int year,
         @PathParam("modelindex") final int modelindex) {
      try {
         LOG.info("Endpoint! ID: {}", simulationid);
         final OptimisationJobPersistent persistentJob = OptimisationJobHandler.getInstance().getJob(simulationid);
         if (persistentJob == null) {
            LOG.error("Simulationslauf mit ID " + simulationid + "existiert nicht");
            return badRequestResponse("Simulationslauf mit ID " + simulationid + "existiert nicht");
         }
         final OptimisationYearPersistent yearData = persistentJob.getYearDataOfSimulatedYear(year, modelindex);
         if (yearData == null) {
            return badRequestResponse("Stützjahr: " + year + " existiert nicht!");
         }
         final File response = new File(yearData.getGdxresult());
         return Response.ok(response).header("Content-Disposition", "attachment; filename=\"job-" + simulationid + "-jahr-" + yearData.getYear() + "-ergebnis.gdx\"").build();
      } catch (final Exception e) {
         e.printStackTrace();
         return errorResponse(e);
      }
   }

   /**
    * Gibt ein Bild des Simulationslaufs für die Anzeige zurück.
    *
    * @param simulationid Die ID des zurückzugebenden Simulationslaufes
    * @param year Das Jahr der spezifischen Simulation
    * @param modelindex Das Model der spezifischen Simulation
    * @param imagename Der Name des gesuchten Bildes
    * @return Das Bild oder eine Fehlermeldung
    */
   @GET
   @Path("/{simulationid}/{year}/{modelindex}/{imagename}")
   @Produces("image/png")
   @Consumes(MediaType.APPLICATION_JSON)
   @ApiOperation(value = "Liefert Bild aus IRPact Szenario", notes = "Gibt ein Bild aus einem IRPact Szenario zurück.")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"),
         @ApiResponse(code = 400, message = "Bad Request") })
   public Response getImage(@PathParam("simulationid") final int simulationid, @PathParam("year") final int year,
         @PathParam("modelindex") final int modelindex, @PathParam("imagename") final String imagename) {
      LOG.info("Image Endpoint! ID: {} Jahr: {} Image{}", simulationid, year, imagename);
      try {
         final OptimisationJobPersistent persistentJob = OptimisationJobHandler.getInstance().getJob(simulationid);
         if (persistentJob == null) {
            LOG.error("Simulationslauf mit ID " + simulationid + "existiert nicht");
            return badRequestResponse("Simulationslauf mit ID " + simulationid + "existiert nicht");
         }
         LOG.trace("SimulationJobPersistent: {}, Start: {}, End: {}", persistentJob.getId(), persistentJob.getStart(), persistentJob.getEnd());
         final OptimisationYearPersistent yearData = persistentJob.getYearDataOfSimulatedYearWithoutIndex(year, modelindex);
         if (yearData == null) {
            return badRequestResponse("Stützjahr: " + year + " existiert nicht!");
         }

         File lstFile = new File(yearData.getLstFile());
         File root = lstFile.getParentFile();
         if (!root.exists()){
            LOG.error("Simulationsordner mit ID " + simulationid + "existiert nicht");
            return badRequestResponse("Simulationsordner mit ID " + simulationid + "existiert nicht");
         }

         final File imagesDir = new File(root+File.separator+"images");
         if (!imagesDir.exists()){
            LOG.error("Image Ordner für Simulation mit ID " + simulationid + "existiert nicht");
            return badRequestResponse("Image Ordner für Simulation mit ID " + simulationid + "existiert nicht");
         }

         final File image = new File(imagesDir+File.separator+imagename);
         if (!image.exists()) {
            LOG.error("Image "+ imagename+" für Simulation mit ID " + simulationid + "existiert nicht");
            return badRequestResponse("Image "+ imagename+" für Simulation mit ID " + simulationid + "existiert nicht");
         }

         LOG.trace("SimulationYearPersistent: {}, Image-Datei: {}, SimulationJobPersistent: {}, Start: {}, End: {}", yearData.getId(), image,
               yearData.getJob().getId(), yearData.getStart(), yearData.getEnd());
         final FileInputStream listingFileStream;
         listingFileStream = new FileInputStream(image);
         return Response.ok(listingFileStream).header("Content-Disposition", "attachment; filename="+imagename).build();
      } catch (FileNotFoundException e) {
         e.printStackTrace();
         return errorResponse(e);
      }
   }

   /**
    * Gibt die im IRPACT erstellten Bilder als Bulk (zip) des Simulationslaufs zu Debugging-Zwecken zurück.
    *
    * @param year Das Jahr der spezifischen Simulation
    * @param simulationid Die ID des zurückzugebenden Simulationslaufes
    * @return Das Bild Archiv oder eine Fehlermeldung
    */
   @Path("/{simulationid}/{year}/{modelindex}/bulkImages")
   @GET
   @ApiOperation(value = "Gibt ein Archive mit Bildern des Simulationslaufs zurück", notes = "Gibt das Bildarchiv des angefragten Jahres Simulationslaufs zu Debugging-Zwecken zurück. ")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Unerwartete Id übergeben") })
   @Produces({ MediaType.APPLICATION_OCTET_STREAM })
   public final Response getBulkIrpActImages(@PathParam("simulationid") final int simulationid, @PathParam("year") final int year,
         @PathParam("modelindex") final int modelindex) {
      try {
         // TODO Find Job Folder
         // TODO contains image folder ?
         // contains zip?
         // TODO zip image folder if not already down
         // return zip
         LOG.info("Bulk Image Endpoint! ID: {} Jahr: {}", simulationid, year);
         final OptimisationJobPersistent persistentJob = OptimisationJobHandler.getInstance().getJob(simulationid);
         if (persistentJob == null) {
            LOG.error("Simulationslauf mit ID " + simulationid + "existiert nicht");
            return badRequestResponse("Simulationslauf mit ID " + simulationid + "existiert nicht");
         }
         LOG.trace("SimulationJobPersistent: {}, Start: {}, End: {}", persistentJob.getId(), persistentJob.getStart(), persistentJob.getEnd());
         final OptimisationYearPersistent yearData = persistentJob.getYearDataOfSimulatedYear(year, modelindex);
         if (yearData == null) {
            return badRequestResponse("Stützjahr: " + year + " existiert nicht!");
         }

         final String lstFile = yearData.getLstFile();
         final String imageArchivFile = lstFile.replace("listing", "images");
         final File fileObject = new File(imageArchivFile);
         if (!fileObject.exists()) {
            return badRequestResponse(imageArchivFile);
         }
         LOG.trace("SimulationYearPersistent: {}, Listing-Datei: {}, SimulationJobPersistent: {}, Start: {}, End: {}", yearData.getId(), imageArchivFile,
               yearData.getJob().getId(), yearData.getStart(), yearData.getEnd());
         final FileInputStream listingFileStream = new FileInputStream(imageArchivFile);
         return Response.ok(listingFileStream).header("Content-Disposition", "attachment; filename=\"job-" + simulationid + "-jahr-" + yearData.getYear() + "images.zip\"").build();
      } catch (final Exception e) {
         e.printStackTrace();
         return errorResponse(e);
      }
   }

   /**
    * Gibt ein Flag zurück ob die im IRPACT Bilder erstellt wurden.
    *
    * @param year Das Jahr der spezifischen Simulation
    * @param simulationid Die ID des zurückzugebenden Simulationslaufes
    * @return True if Archive existiert sonst false
    */
   @Path("/{simulationid}/{year}/{modelindex}/bulkImagesExists")
   @GET
   @ApiOperation(value = "Gibt ein Flag zurück ob im Simulationslaufs  Bildern erstellt wurden", notes = "Ture or False jenach dem ob Bilder/das Archiv existieren")
   @ApiResponses(value = { @ApiResponse(code = 200, message = "Ok"), @ApiResponse(code = 400, message = "Unerwartete Id übergeben") })
   @Produces({ MediaType.APPLICATION_JSON })
   public final Response checkImagesExists(@PathParam("simulationid") final int simulationid, @PathParam("year") final int year,
         @PathParam("modelindex") final int modelindex) {
      try {
         LOG.info("Bulk Image Endpoint! ID: {} Jahr: {}", simulationid, year);
         final OptimisationJobPersistent persistentJob = OptimisationJobHandler.getInstance().getJob(simulationid);
         if (persistentJob == null) {
            LOG.error("Simulationslauf mit ID " + simulationid + "existiert nicht");
            // false for simplifying frontend
            return Response.ok(false).build();
         }
         LOG.trace("SimulationJobPersistent: {}, Start: {}, End: {}", persistentJob.getId(), persistentJob.getStart(), persistentJob.getEnd());
         final OptimisationYearPersistent yearData = persistentJob.getYearDataOfSimulatedYear(year, modelindex);
         if (yearData == null) {
            return Response.ok(false).build();
         }

         final String lstFile = yearData.getLstFile();
         final String imageArchivFile = lstFile.replace("listing", "images");
         LOG.trace("SimulationYearPersistent: {}, Listing-Datei: {}, SimulationJobPersistent: {}, Start: {}, End: {}", yearData.getId(), imageArchivFile,
               yearData.getJob().getId(), yearData.getStart(), yearData.getEnd());

         final File fileObject = new File(imageArchivFile);
         return Response.ok(fileObject.exists()).build();
      } catch (final Exception e) {
         e.printStackTrace();
         return Response.ok(false).build();
      }
   }
}
