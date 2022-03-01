package de.unileipzig.irpsim.server.endpoints;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.unileipzig.irpsim.server.data.Responses;
import de.unileipzig.irpsim.server.optimisation.queue.OptimisationJobHandler;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Ermöglicht es dem Nutzer, selbst die Daten aufzuräumen. TODO Es sollte beim Job-Start geprüft werden, ob gerade eine Clean-Prozess läuft Achtung: Umlaut-API-Namen sind zurzeit nicht möglich!
 * 
 * @author reichelt
 *
 */
@Path("/cleanup")
@Api(value = "/cleanup", tags = "Bereinigen")
public class CleanupEndpoint {

	private static Cleaner cleaner = null;

	public static boolean isCleaning() {
		if (cleaner != null) {
			return cleaner.isCleaning();
		} else {
			return false;
		}
	}

	/**
	 * Startet einen Aufräum-Prozess
	 */
	@Path("/start/")
	@GET
	@ApiOperation(value = "Bereinigt die Server-Daten.", notes = "Bereinigt die Server-Daten auf, d.h. sucht nicht referenzierte Datensätze und löscht diese und optimiert die Tabellen."
			+ " Achtung: Solange der Prozess läuft, sollten keine Jobs laufen oder gestartet werden!")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok") })
	@Produces(MediaType.APPLICATION_JSON)
	public final Response startCleanup() {
		try {
			if (OptimisationJobHandler.getInstance().getActiveJobs().size() > 0) {
				return Responses.badRequestResponse("Aufräumen sollte nur gestartet werden, wenn kein Job läuft!");
			} else {
				if (isCleaning()) {
					return Responses.badRequestResponse("Aufräumen läuft noch!");
				}
				cleaner = new Cleaner();
				Thread cleanThread = new Thread(cleaner);
				cleanThread.start();
				return Responses.okResponse("Aufräumen gestartet.", "Erfolgreich");
			}
		} catch (final Throwable t) {
			t.printStackTrace();
			return Responses.errorResponse(t);
		}
	}
	
	/**
	 * Gibt den aktuellen Zustand des Aufräumprozesses aus
	 */
	@Path("/state/")
	@GET
	@ApiOperation(value = "Gibt den aktuellen Zustand des Beräumungsprozesses zurück.")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok") })
	@Produces(MediaType.APPLICATION_JSON)
	public final Response getState() {
		try {
			if (isCleaning()) {
				return Response.ok(cleaner.getState()).build();
			} else {
				return Responses.okResponse("Kein Prozess", "Aufräumen läuft zurzeit nicht.");
			}
		} catch (final Throwable t) {
			t.printStackTrace();
			return Responses.errorResponse(t);
		}
	}
}
