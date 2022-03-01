package de.unileipzig.irpsim.server.standingdata.endpoints;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityTransaction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.Root;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.standingdata.StaticDataUtil;
import de.unileipzig.irpsim.core.standingdata.data.AlgebraicData;
import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;
import de.unileipzig.irpsim.core.standingdata.data.StaticData;
import de.unileipzig.irpsim.core.standingdata.data.Variable;
import de.unileipzig.irpsim.server.algebraicdata.AlgebraicDataEvaluator;
import de.unileipzig.irpsim.server.data.Responses;
import de.unileipzig.irpsim.server.standingdata.endpoints.utils.AlgebraicDataUpdater;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Dies ist der erste Entwurf für den Endpunkt für algebraische Zeitreihendaten. Perspektivisch wäre es zu überlegen, diesen mit dem DataEndpoint zusammenzuführen (der zurzeit ausschließlich statische
 * Daten behandelt).
 * 
 * @author reichelt
 *
 */
@Path("/stammdaten")
@Api(value = "/stammdaten/{id}/algebraicdata", tags = { "Stammdaten", "Datensatz" })
public class AlgebraicDataEndpoint {

	@Path("/{id}/algebraicdata")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Gibt die vorhandenen algebraischen Datensätze zurück", notes = "")
	@ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid ID supplied"),
			@ApiResponse(code = 404, message = "Not Found") })
	public final Response getAlgebraicDataQueried(@PathParam("id") final int id, @QueryParam("jahr") final Integer jahr)
			throws JsonProcessingException {
		try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final Session s = (Session) em.getDelegate();
			final Stammdatum stammdatum = em.find(Stammdatum.class, id);
			if (stammdatum == null) {
				return Responses.badRequestResponse("Stammdatum " + id + " existiert nicht.");
			}

			final List<AlgebraicData> algebraicDataList = s.createCriteria(AlgebraicData.class)
					.add(Restrictions.eq("stammdatum", stammdatum))
					.add(Restrictions.eqOrIsNull("aktiv", true)).list();
			if (algebraicDataList.size() > 0 && (jahr == null || algebraicDataList.get(0).getJahr() <= jahr)) {
				return Response.ok(algebraicDataList).build();
			} else {
				return Response.ok(new LinkedList<>()).build();
			}
		} catch (final Throwable e) {
			e.printStackTrace();
			return Responses.errorResponse(e);
		}
	}

	@Path("/{id}/algebraicdata/preview")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Gibt eine Datensatz-Vorschau zurück", notes = "Berechnet die Werte für die angegebene Formel und gibt dieses zurück ohne sie in der Datenbank zu speichern.")
	@ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid ID supplied"),
			@ApiResponse(code = 404, message = "Not Found") })
	public final Response getAlgebraicPreviewData(final AlgebraicData algebraicData) {

		try {
			try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {

				// Löse Stammdaten-Referenzen auf. Sowohl für das AlgebraicData Objekt als auch für alle Variablen.
				final Stammdatum stammdatum = em.find(Stammdatum.class, algebraicData.getStammdatum().getId());
				StaticDataUtil.fillStammdatum(stammdatum);
				algebraicData.setStammdatum(stammdatum);

				final HashMap<String, Variable> zuordnung = new HashMap<>();

				for (final String key : algebraicData.getVariablenZuordnung().keySet()) {
					final Variable var = algebraicData.getVariablenZuordnung().get(key);

					final Stammdatum st = em.find(Stammdatum.class, var.getStammdatum().getId());
					StaticDataUtil.fillStammdatum(st);
					var.setStammdatum(st);

					zuordnung.put(key, var);
				}

				algebraicData.setVariablenZuordnung(zuordnung);

				final double[] result = new AlgebraicDataEvaluator().evaluateFormula(algebraicData);

				return Response.ok(result).build();
			}
		} catch (final RuntimeException e) {
			e.printStackTrace();
			return Responses.errorResponse(e);
		}
	}

	@Path("/{id}/algebraicdata")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Fügt Datensatz hinzu", notes = "Fügt einen algebraischen Datensatz hinzu.")
	@ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid ID supplied"),
			@ApiResponse(code = 404, message = "Not Found") })
	public final Response addAlgebraicData(@PathParam("id") final int id, @QueryParam("force") @DefaultValue("false") final boolean force, final AlgebraicData algebraicData) {
		try {
			try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
				final Session s = (Session) em.getDelegate();
				final Stammdatum stammdatum = em.find(Stammdatum.class, id);
				if (stammdatum == null) {
					return Responses.badRequestResponse("Stammdatum " + id + " existiert nicht.");
				}
				if (algebraicData.getStammdatum() != null && algebraicData.getStammdatum().getId() != id) {
					return Responses.badRequestResponse("Es soll ein algebraischer Datensatz für Stammdatum " + id
							+ " hinzugefügt werden, es wurde jedoch Stammdatum " + algebraicData.getStammdatum().getId()
							+ " als Stammdatum im JSON übergeben.");
				}
				// make sure we include all derived attributes
				StaticDataUtil.fillStammdatum(stammdatum);
				if (stammdatum.getBezugsjahr() > algebraicData.getJahr()) {
					return Responses.badRequestResponse("Bezugsjahr ist " + stammdatum.getBezugsjahr()
							+ " Import in Jahr " + algebraicData.getJahr() + " nicht möglich.");
				}
				if (stammdatum.getBezugsjahr() + stammdatum.getPrognoseHorizont() < algebraicData.getJahr()) {
					return Responses.badRequestResponse(
							"Endjahr ist " + (stammdatum.getBezugsjahr() + stammdatum.getPrognoseHorizont())
									+ " Import in Jahr " + algebraicData.getJahr() + " nicht möglich.");
				}
				final List<StaticData> staticData = s.createCriteria(StaticData.class)
						.add(Restrictions.ge("jahr", algebraicData.getJahr()))
						.add(Restrictions.eq("stammdatum", stammdatum)).list();
				if (staticData.size() > 0) {
					if (force == false) {
						String existingDataError = "<table><tr><th>Zeitreihen-Id</th><th>Jahr</th><th>Szenario-Stelle</th></tr>";
						for (final StaticData data : staticData) {
							existingDataError += "<tr><td>" + data.getSeriesid() + "</td><td>" + data.getJahr() + "</td><td>" + data.getSzenario() + "</td></tr>";
						}
						existingDataError += "</table>";
						return Responses.conflictResponse("Es sind bereits statische Datensätze vorhanden: " + existingDataError);
					} else {
						final EntityTransaction et = em.getTransaction();
						et.begin();
						for (final StaticData data : staticData) {
							em.remove(data);
						}
						et.commit();
						return addAlgebraicData(algebraicData, em, s, stammdatum);
					}
				} else {
					return addAlgebraicData(algebraicData, em, s, stammdatum);
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
			return Responses.errorResponse(e);
		}
	}

	public Response addAlgebraicData(final AlgebraicData algebraicData, final ClosableEntityManager em, final Session s, final Stammdatum stammdatum) {
		final List<AlgebraicData> algebraicDataList = s.createCriteria(AlgebraicData.class)
				.add(Restrictions.eq("stammdatum", stammdatum))
				.setResultTransformer(Criteria.DISTINCT_ROOT_ENTITY).list();
		final List<Integer> ids;
		// TODO validate all instance of AlgebraicData using
		// de.unileipzig.irpsim.server.algebraicdata.AlgebraicDataEvaluator.validateDependencies(AlgebraicData)
		// and notify user
		if (algebraicDataList.size() > 0) {
			ids = new AlgebraicDataUpdater().updateAlgebraicdata(algebraicData, em, stammdatum, algebraicDataList);
		} else {
			ids = new AlgebraicDataUpdater().insertAlgebraicdata(algebraicData, em, stammdatum);
		}
		StaticDataUtil.updateCompleteness(em, stammdatum);
		return Response.ok(ids).build();
	}

	@Path("/{id}/algebraicdata/{algebraicid}")
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Löscht einen Datensatz", notes = "Löscht einen algebraischen Datensatz mit all seinen Repräsentationen.")
	@ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid ID supplied"),
			@ApiResponse(code = 404, message = "Not Found") })
	public final Response deleteAlgebraicData(@PathParam("id") final int id,
			@PathParam("algebraicid") final int algebraicid) {
		try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final AlgebraicData ad = em.find(AlgebraicData.class, algebraicid);

			if (ad == null) {
				return Responses.badRequestResponse("Algebraischer Datensatz " + algebraicid + " nicht definiert.");
			}

			if (ad.getStammdatum().getId() != id) {
				return Responses.badRequestResponse("Algebraischer Datensatz " + algebraicid + " gehört zu Stammdatum "
						+ ad.getStammdatum().getId() + ", Anfrage bezog sich aber auf " + id + ".");
			}

			final EntityTransaction et = em.getTransaction();
			et.begin();
			final CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
			final CriteriaDelete<AlgebraicData> delete = criteriaBuilder.createCriteriaDelete(AlgebraicData.class);
			final Root<AlgebraicData> root = delete.from(AlgebraicData.class);
			delete.where(criteriaBuilder.equal(root.get("stammdatum"), ad.getStammdatum()));

			em.createQuery(delete).executeUpdate();

			et.commit();
			StaticDataUtil.updateCompleteness(em, ad.getStammdatum());
			return Responses.okResponse("Erfolgreich", "Algebraischer Datensatz erfolgreich gelöscht");
		} catch (final Exception e) {
			e.printStackTrace();
			return Responses.errorResponse(e);
		}
	}

}
