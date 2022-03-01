package de.unileipzig.irpsim.server.standingdata.endpoints;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;
import de.unileipzig.irpsim.core.standingdata.data.StaticData;
import de.unileipzig.irpsim.server.data.Responses;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

@Path("/stammdaten")
@Api(value = "/stammdaten/{id}/data", tags = { "Stammdaten", "Datensatz" }, description = "Repräsentiert die Datensätze eines Stammdatums.")
public class DataEndpoint {

	@Path("/{id}/data")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Gibt die vorhandenen Datensätze zurück", notes = "Es werden vorhandene Datensätze zurückgegeben (zurzeit nur StaticData).")
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "Invalid ID supplied"),
			@ApiResponse(code = 404, message = "Not Found") })
	public final Response getDataQueried(@PathParam("id") final int id, @QueryParam("szenarioStelle") final int szenarioStelle, @QueryParam("jahr") final Integer jahr) throws JsonProcessingException {
		try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final Stammdatum stammdatum = em.find(Stammdatum.class, id);
			if (stammdatum == null) {
				return Responses.badRequestResponse("Stammdatum " + id + " existiert nicht.");
			}

			final CriteriaBuilder cBuilder = em.getCriteriaBuilder();
			final CriteriaQuery<StaticData> sdQuery = cBuilder.createQuery(StaticData.class);
			final Root<StaticData> ojpRoot = sdQuery.from(StaticData.class);

			final Predicate[] restrictions = new Predicate[] {
			      cBuilder.equal(ojpRoot.get("stammdatum"), stammdatum),
			      cBuilder.or(cBuilder.equal(ojpRoot.get("aktiv"), true), cBuilder.isNull(ojpRoot.get("aktiv")))
			};

			final List<StaticData> stammdaten = em.createQuery(sdQuery.where(restrictions)).getResultList();

			final JSONArray jsa = new JSONArray();
			stammdaten.stream().forEach(sd -> {
				if (jahr != null && sd.getJahr() != jahr) {
					return;
				}
				if (szenarioStelle != 0 && sd.getSzenario() != szenarioStelle) {
					return;
				}
				final JSONObject info = new JSONObject();
				info.put("szenario", sd.getSzenario());
				info.put("jahr", sd.getJahr());
				info.put("seriesid", sd.getSeriesid());
				jsa.put(info);
			});

			return Response.ok(jsa.toString()).build();
		} catch (final Throwable e) {
			e.printStackTrace();
			return Responses.errorResponse(e);
		}
	}
}
