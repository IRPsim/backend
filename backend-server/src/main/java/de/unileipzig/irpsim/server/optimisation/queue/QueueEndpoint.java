package de.unileipzig.irpsim.server.optimisation.queue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.simulation.data.json.IntermediarySimulationStatus;
import de.unileipzig.irpsim.server.data.Responses;
import de.unileipzig.irpsim.server.optimisation.Job;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Endpunkt für die Verwaltung von Optimierungswarteschlangen.
 */
@Path("/simulations")
@Api(value = "/simulations/queue", tags = "Simulation")
public class QueueEndpoint {

	private static final Logger LOG = LogManager.getLogger(QueueEndpoint.class);

	/**
	 * Gibt die Optimierungs-Wartschlange aus, d.h. die Jobs, die gerade noch auf ihre Ausführung warten.
	 *
	 * @return Die Liste der wartenden Optimierungsjobs.
	 */
	@Path("/queue")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Gibt alle wartenden Optimierungsjobs in ihrer Reihenfolge mit Metadaten zurück.", notes = "Gibt alle "
			+ "wartenden Optimierungsjobs in ihrer Reihenfolge mit Metadaten zurück.")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok") })
	public final Response getOptimisationQueue() {
		try {
			final Collection<Job> jobs = OptimisationJobHandler.getInstance().getWaitingJobs();

			final List<IntermediarySimulationStatus> states = new ArrayList<>(jobs.size());
			for (final Job job : jobs) {
				states.add(job.getIntermediaryState());
			}

			final ObjectMapper om = new ObjectMapper();
			final String result = om.writeValueAsString(states);

			return Response.status(Response.Status.OK).entity(result).build();
		} catch (final Exception e) {
			e.printStackTrace();
			return Responses.errorResponse(e, "Simulationsstatus");
		}
	}

	/**
	 * Sortiert die wartenden Optimierungsjobs in der angegebenen Reihenfolge. Alle wartenden Optimierungsjobs müssen vorhanden sein!
	 *
	 * @param order Neue Ordnung der Jobs als Array der ids.
	 * @return Gibt die neue Ordnung der wartenden Jobs zurück.
	 */
	@Path("/queue")
	@PUT
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Sortiert die wartenden Optimierungsjobs in der angegebenen Reihenfolge.", notes = "Alle wartenden Optimierungsjobs müssen vorhanden sein. "
			+ "Gibt die neue Reihenfolge der wartenden Jobs zurück.")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Ok") })
	public final Response changeOptimisationQueueOrder(final Long[] order) {
		try {
			LOG.debug("Angefragte neue Ordnung: {}", "" + order);
			final String missing = OptimisationJobHandler.getInstance().reschedule(order);
			if (!missing.equals("Reihenfolgenänderung erfolgreich")) {
				LOG.warn("Nicht alle wartenden Jobs angegeben, es fehlen: {}", missing);
			} else {
				LOG.debug("{}", missing);
			}
			return getOptimisationQueue();
		} catch (final Exception e) {
			e.printStackTrace();
			return Responses.errorResponse(e, "Simulationsstatus");
		}
	}
}
