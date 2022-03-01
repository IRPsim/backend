package de.unileipzig.irpsim.server.standingdata.endpoints;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityTransaction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
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

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.data.JSONErrorMessage;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.standingdata.SzenarioSet;
import de.unileipzig.irpsim.core.standingdata.SzenarioSetElement;
import de.unileipzig.irpsim.server.data.Responses;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Path("/szenariosets")
@Api(value = "/szenariosets", tags = "Prognoseszenarien")
public class SzenarioSetEndpoint {

	static final Logger LOG = LogManager.getLogger(SzenarioSetEndpoint.class);

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Gibt alle Prognoseszenarien-Sets zurück.", notes = "")
	public Response getSzenarioSet() {
		try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final Session s = (Session) em.getDelegate();
			CriteriaBuilder builder = s.getCriteriaBuilder();
			//Create Criteria
			CriteriaQuery<SzenarioSet> criteria = builder.createQuery(SzenarioSet.class);
			Root<SzenarioSet> szenarioSetRoot = criteria.from(SzenarioSet.class);
			criteria.select(szenarioSetRoot);

			final List<SzenarioSet> sets = s.createQuery(criteria).getResultList();
			// final List<SzenarioSet> sets = s.createCriteria(SzenarioSet.class).list();
			return Response.ok(new ObjectMapper().writeValueAsString(sets)).build();
		} catch (final Throwable t) {
			t.printStackTrace();
			return Responses.errorResponse("Interner Server-Fehler");
		}
	}

	private static Thread currentUpdateThread = new Thread();

	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Fügt ein Prognoseszenarien-Sets hinzu oder bearbeitet es", notes = "")
	public Response putSzenarioSet(final SzenarioSet set) {
		for (final SzenarioSetElement sse : set.getSzenarien()) {
			sse.setSet(set);
		}
		try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			synchronized (currentUpdateThread) {
				if (currentUpdateThread != null && currentUpdateThread.isAlive()) {
					return Responses.badRequestResponse("Das Hinzufügen Ihres Prognoseszenarios kann erst ausgeführt werden, wenn der Update-Prozess des letzten Hinzufügens abgeschlossen wurde.");
				}

				final Session session = (Session) em.getDelegate();
				final SzenarioSet setOld = em.find(SzenarioSet.class, set.getJahr());

				final List<Integer> newSSEs = new LinkedList<>();

				final EntityTransaction transaction = em.getTransaction();
				transaction.begin();

				String errorMessage = "";
				if (setOld == null) {
					em.persist(set);
					for (final SzenarioSetElement sse : set.getSzenarien()) {
						em.persist(sse);
					}
				} else {
					errorMessage = invalidateOldSzenarioSetElements(set, em, session, setOld, errorMessage);
					errorMessage = buildNewSzenarioSetElements(set, em, session, setOld, newSSEs, errorMessage);

				}
				transaction.commit();

				if (newSSEs.size() > 0) {
					currentUpdateThread = new Thread(new UpdateStandardszenarioThread(newSSEs));
					currentUpdateThread.start();
				}

				if (errorMessage.length() > 0) {
					return Responses.okResponse("Warnung", errorMessage, JSONErrorMessage.Severity.WARNING);
				} else {
					return Responses.okResponse("Erfolgreich", "Erfolgreich hinzugefügt");
				}
			}
		} catch (

		final Throwable t) {
			t.printStackTrace();
			return Responses.errorResponse("Interner Server-Fehler");
		}
	}

   private String buildNewSzenarioSetElements(final SzenarioSet set, final ClosableEntityManager em, final Session session, final SzenarioSet setOld, final List<Integer> newSSEs,
         String errorMessage) throws SQLException {
      for (final SzenarioSetElement sse : set.getSzenarien()) {
      	SzenarioSetElement sseOldFound = null;
      	for (final SzenarioSetElement sseOld : setOld.getSzenarien()) {
      		if (sseOld.getStelle() == sse.getStelle()) {
      			sseOldFound = sseOld;
      			break;
      		}
      	}
      	if (sseOldFound != null) {
      		sseOldFound.setName(sse.getName());
      		em.merge(sseOldFound);
      	} else {
      		sse.setSet(setOld);
      		setOld.getSzenarien().add(sse);
      		em.persist(sse);

      		newSSEs.add(sse.getStelle());
            errorMessage = reactivateOldDatensatz(set, session, errorMessage, sse);
      	}
      }
      em.merge(setOld);
      return errorMessage;
   }

   private String reactivateOldDatensatz(final SzenarioSet set, final Session session, String errorMessage, final SzenarioSetElement sse) throws SQLException {
      final String selectQuery = "SELECT COUNT(*) FROM Datensatz LEFT JOIN Stammdatum ON Datensatz.stammdatum_id =Stammdatum.id  WHERE Stammdatum.bezugsjahr="
            + set.getJahr() + " AND szenario =" + sse.getStelle();

      try (final ResultSet result = session.doReturningWork(connection -> connection.createStatement().executeQuery(selectQuery))) {
         result.next();
         final Integer value = result.getInt(1);
         if (value > 0) {
            errorMessage += "Achtung: Szenario " + sse.getStelle() + " " + sse.getName() + " hat bereits " + value + " Datensätze, die aktiviert werden.\n ";

            try (final Statement statement = session.doReturningWork(connection -> connection.createStatement())) {
               statement.executeUpdate("UPDATE Datensatz LEFT JOIN Stammdatum ON Datensatz.stammdatum_id =Stammdatum.id SET aktiv=true WHERE bezugsjahr="
                     + set.getJahr() + " AND szenario=" + sse.getStelle());
            }
         }
      }
      return errorMessage;
   }

   private String invalidateOldSzenarioSetElements(final SzenarioSet set, final ClosableEntityManager em, final Session session, final SzenarioSet setOld, String errorMessage)
         throws SQLException {
      for (final SzenarioSetElement sseOld : setOld.getSzenarien()) {
      	boolean found = false;
      	for (final SzenarioSetElement sse : set.getSzenarien()) {
      		if (sseOld.getStelle() == sse.getStelle()) {
      			found = true;
      		}
      	}
         if (!found) {
            em.remove(sseOld);

            try (Statement statement = session.doReturningWork(connection -> connection.createStatement())) {

               final String updateQuery = "UPDATE Datensatz LEFT JOIN Stammdatum ON Datensatz.stammdatum_id =Stammdatum.id SET aktiv=false WHERE bezugsjahr="
                     + set.getJahr() + " AND szenario=" + sseOld.getStelle();
               LOG.debug(updateQuery);

               statement.executeUpdate(updateQuery);

               final String selectQuery = "SELECT COUNT(*) FROM Datensatz LEFT JOIN Stammdatum ON Datensatz.stammdatum_id =Stammdatum.id  WHERE Stammdatum.bezugsjahr="
                     + set.getJahr() + " AND szenario =" + sseOld.getStelle();
               try (final ResultSet result = session.doReturningWork(connection -> connection.createStatement().executeQuery(selectQuery))) {
                  result.next();
                  final Integer value = result.getInt(1);
                  if (value > 0) {
                     errorMessage += "Achtung: Szenario " + sseOld.getStelle() + " " + sseOld.getName() + " hat " + value
                           + " Datensätze, die nun auf inaktiv gesetzt werden.\n ";
                  }
               }
            }
         }
      }
      return errorMessage;
   }

	@DELETE
	@Path("/{jahr}")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Löscht ein Prognoseszenarien-Set", notes = "")
	public Response deleteSzenarioSet(@PathParam("jahr") final int jahr) {
		try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final SzenarioSet setOld = em.find(SzenarioSet.class, jahr);
			final EntityTransaction transaction = em.getTransaction();
			transaction.begin();
			if (setOld != null) {
				// and/or UPDATE JOIN ?
				for (final SzenarioSetElement sse : setOld.getSzenarien()){
					em.remove(sse);
				}
				em.remove(setOld);
				transaction.commit();
				return Responses.okResponse("Löschen erfolgreich", "Szenarioset erfolgreich gelöscht.");
			} else {
				return Responses.badRequestResponse("Löschen fehlgeschlagen", "Szenarioset konnte nicht gelöscht werden.");
			}
		} catch (final Throwable t) {
			t.printStackTrace();
			return Responses.errorResponse("Interner Server-Fehler");
		}
	}
}
