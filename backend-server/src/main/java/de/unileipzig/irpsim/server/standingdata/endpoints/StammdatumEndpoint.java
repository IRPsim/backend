package de.unileipzig.irpsim.server.standingdata.endpoints;

import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.EntityTransaction;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.simulation.data.TimeInterval;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.standingdata.StaticDataUtil;
import de.unileipzig.irpsim.core.standingdata.SzenarioSet;
import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;
import de.unileipzig.irpsim.server.data.Responses;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

/**
 * Ermöglicht das Erstellen und Löschen von Stammdaten.
 *
 */
@Path("/stammdaten")
@Api(value = "/stammdaten", tags = "Stammdaten", description = "Repräsentiert Stammdaten.")
public class StammdatumEndpoint {

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Gibt die Stammdaten zurück", notes = "Es werden Stammdaten zurück gegeben.")
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "Invalid ID supplied"),
			@ApiResponse(code = 404, message = "Not Found") })
	public final Response getStammdatumQueried(
			@ApiParam(name = "all", value = "Definiert, ob die gesamten Informationen zum Stammdatum oder nur die Ids zurückgegeben werden sollen. Default: false.") @QueryParam("all") final Boolean all,
			@ApiParam(name = "id", value = "Definiert die Id des angeforderten Stammdatums") @QueryParam("id") final Integer id,
			@ApiParam(name = "abstrakt", value = "Definiert, ob die gesuchten Stammdaten abstrakt sind.") @QueryParam("abstrakt") final Boolean abstrakt,
			@ApiParam(name = "bezugsjahr", value = "Definiert das Bezugsjahr der angeforderten Stammdaten.") @QueryParam("bezugsjahr") final Integer bezugsjahr,
			@ApiParam(name = "prognoseHorizont", value = "Definiert den Prognosehorizont der angeforderten Stammdaten.") @QueryParam("prognoseHorizont") final Integer prognoseHorizont,
			@ApiParam(name = "name", value = "Definiert einen Filter für den Namen des Stammdatums.") @QueryParam("name") final String name,
			@ApiParam(name = "typ", value = "Definiert einen Filter für den Typ des Stammdatums.") @QueryParam("typ") final String typ,
			@ApiParam(name = "zeitintervall", value = "Definiert das Datum, ab dem Zeitreihendaten zurückgegeben werden sollen") @QueryParam("zeitintervall") final TimeInterval zeitintervall,
			@ApiParam(name = "referenz", value = "Definiert die Referenz der angeforderten Stammdaten.") @QueryParam("referenz") final Integer referenz,
			@ApiParam(name = "verantwortlicherBezugsjahrEmail", value = "Definiert einen Filter für die Email des Verantwortlichen des Bezugsjahr des Stammdatums.") @QueryParam("verantwortlicherBezugsjahrEmail") final String verantwortlicherBezugsjahrEmail,
			@ApiParam(name = "verantwortlicherBezugsjahrName", value = "Definiert einen Filter für den Namen des Verantwortlichen des Bezugsjahr des Stammdatums.") @QueryParam("verantwortlicherBezugsjahrName") final String verantwortlicherBezugsjahrName,
			@ApiParam(name = "verantwortlicherPrognosejahrEmail", value = "Definiert einen Filter für die Email des Verantwortlichen des Prognosejahr des Stammdatums.") @QueryParam("verantwortlicherPrognosejahrEmail") final String verantwortlicherPrognosejahrEmail,
			@ApiParam(name = "verantwortlicherPrognosejahrName", value = "Definiert einen Filter für den Namen des Verantwortlichen des Prognosejahr des Stammdatums.") @QueryParam("verantwortlicherPrognosejahrName") final String verantwortlicherPrognosejahrName,
			@ApiParam(name = "maxcount", value = "Gibt die maximale Anzahl an Ergebnissen an, die zurückgegeben werden soll") @QueryParam("maxcount") final Integer maxcount)
			throws JsonProcessingException {
		try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final Session session = (Session) em.getDelegate();
			final Criteria createCriteria = createQueryCriteria(id, abstrakt, bezugsjahr, prognoseHorizont, name, typ, zeitintervall, referenz, verantwortlicherBezugsjahrEmail,
               verantwortlicherBezugsjahrName, verantwortlicherPrognosejahrEmail, verantwortlicherPrognosejahrName, maxcount, session);
			final List<Stammdatum> stammdaten = createCriteria.list();
			String result;
			if (all != null && all == true) {
				result = new ObjectMapper().writeValueAsString(stammdaten);
			} else {
				final List<Integer> ids = stammdaten.stream().map(sd -> sd.getId()).collect(Collectors.toList());
				result = new ObjectMapper().writeValueAsString(ids);
			}

			return Response.ok(result).build();
		} catch (final Throwable t) {
			t.printStackTrace();
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

   private Criteria createQueryCriteria(final Integer id, final Boolean abstrakt, final Integer bezugsjahr, final Integer prognoseHorizont, final String name, final String typ,
         final TimeInterval zeitintervall, final Integer referenz, final String verantwortlicherBezugsjahrEmail, final String verantwortlicherBezugsjahrName,
         final String verantwortlicherPrognosejahrEmail, final String verantwortlicherPrognosejahrName, final Integer maxcount, final Session session) {
      final Criteria createCriteria = session.createCriteria(Stammdatum.class);
      if (id != null) {
      	createCriteria.add(Restrictions.eq("id", id));
      }
      if (abstrakt != null) {
      	createCriteria.add(Restrictions.eq("abstrakt", abstrakt));
      }
      if (bezugsjahr != null) {
      	createCriteria.add(Restrictions.eq("bezugsjahr", bezugsjahr));
      }
      if (prognoseHorizont != null) {
      	createCriteria.add(Restrictions.eq("prognoseHorizont", prognoseHorizont));
      }
      if (name != null) {
      	createCriteria.add(Restrictions.ilike("name", "%" + name + "%"));
      }
      if (typ != null) {
      	createCriteria.add(Restrictions.ilike("typ", "%" + typ + "%"));
      }
      if (zeitintervall != null) {
      	createCriteria.add(Restrictions.eq("zeitintervall", zeitintervall));
      }
      if (referenz != null) {
      	final Criteria referenzCriteria = createCriteria.createCriteria("referenz");
      	referenzCriteria.add(Restrictions.eq("id", referenz));
      }
      if (verantwortlicherBezugsjahrEmail != null) {
      	final Criteria personCriteria = createCriteria.createCriteria("verantwortlicherBezugsjahr");
      	personCriteria.add(Restrictions.ilike("email", "%" + verantwortlicherBezugsjahrEmail + "%"));
      }
      if (verantwortlicherBezugsjahrName != null) {
      	final Criteria personCriteria = createCriteria.createCriteria("verantwortlicherBezugsjahr");
      	personCriteria.add(Restrictions.ilike("name", "%" + verantwortlicherBezugsjahrName + "%"));
      }
      if (verantwortlicherPrognosejahrEmail != null) {
      	final Criteria personCriteria = createCriteria.createCriteria("verantwortlicherPrognosejahr");
      	personCriteria.add(Restrictions.ilike("email", "%" + verantwortlicherPrognosejahrEmail + "%"));
      }
      if (verantwortlicherPrognosejahrName != null) {
      	final Criteria personCriteria = createCriteria.createCriteria("verantwortlicherPrognosejahr");
      	personCriteria.add(Restrictions.ilike("name", "%" + verantwortlicherPrognosejahrName + "%"));
      }
      if (maxcount != null && maxcount >= 1) {
      	createCriteria.setMaxResults(maxcount);
      }
      return createCriteria;
   }

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Fügt neues Stammdatum hinzu", notes = "Fügt Stammdaten hinzu. Sind sie bereits vorhanden, wird ein Fehler ausgegeben. Für das Ändern sollte der Endpunkt des Stammdatums angesprochen werden.")
	public final Response putStammdatum(final Stammdatum stammdatum) throws JsonProcessingException {
		try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final Session s = (Session) em.getDelegate();
			final List<Stammdatum> stammdatenIdentical = s.createCriteria(Stammdatum.class).add(Restrictions.eq("id", stammdatum.getId())).list();
			if (stammdatenIdentical.size() > 0) {
				return Responses.badRequestResponse("Stammdatum " + stammdatenIdentical.size() + " mal vorhanden");
			} else {
				if (stammdatum.getBezugsjahr() != null) {
					final SzenarioSet prognoseszenarien = em.find(SzenarioSet.class, stammdatum.getBezugsjahr());
					if (prognoseszenarien == null || prognoseszenarien.getSzenarien().size() == 0) {
						return Responses.badRequestResponse("Prognoseszenarien für " + stammdatum.getBezugsjahr() + " sind nicht definiert; das Stammdatum kann nicht hinzugefügt werden.");
					}
				}

				final Stammdatum equal = StaticDataUtil.findEqualStammdatum(stammdatum, em);
				if (equal != null) {
					return Responses.badRequestResponse("Stammdatum mit gleichem Namen, Typ und Bezugsjahr vorhanden: " + equal.getId());
				}
				if (stammdatum.getReferenz() != null) {
					final List<Stammdatum> stammdatenParent = s.createCriteria(Stammdatum.class).add(Restrictions.eq("id", (stammdatum.getReferenz().getId()))).list();
					if (stammdatenParent.size() == 0) {
						return Responses.badRequestResponse("Stammdatum mit Parent-ID " + stammdatum.getReferenz().getId() + " nicht vorhanden");
					} else {
						stammdatum.setReferenz(stammdatenParent.get(0));
					}
				}
				stammdatum.setIsNulldata();

				final EntityTransaction et = em.getTransaction();
				et.begin();
				em.persist(stammdatum.getVerantwortlicherBezugsjahr());
				em.persist(stammdatum.getVerantwortlicherPrognosejahr());
				em.persist(stammdatum);
				et.commit();
				return Response.ok("[" + stammdatum.getId() + "]").build();
			}
		} catch (final Throwable t) {
			t.printStackTrace();
			return Responses.errorResponse(t);
		}
	}

}
