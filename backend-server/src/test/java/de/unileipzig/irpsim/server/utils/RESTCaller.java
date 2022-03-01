package de.unileipzig.irpsim.server.utils;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.JerseyClient;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.client.JerseyInvocation.Builder;
import org.glassfish.jersey.client.JerseyWebTarget;

/**
 * Hilfsklasse, um REST-Anfragen abzusenden.
 *
 * @author reichelt
 */
public final class RESTCaller {

	private static final Logger LOG = LogManager.getLogger(RESTCaller.class);

	private static final JerseyClient jc = new JerseyClientBuilder().build();

	/**
	 * Führt eine REST GET Anfrage aus.
	 *
	 * @param uri Die URI, an die angefragt wird
	 * @return Die Antwort als {@link String}
	 */
	public static String callGet(final String uri) throws AssertionError {
		LOG.trace("callGet: {}", uri);
		final JerseyWebTarget jwt = jc.target(uri);
		final Builder response = jwt.request();
		final Response r = response.get();
		if (r.getStatus() != 200) {
			LOG.warn("Die Rückgabe liefert einen Fehler! Status {}: {}", r.getStatus(), r.getStatusInfo());
			throw new AssertionError(r.getStatus() + "");
		}

		final String loadstring = r.readEntity(String.class);
		return loadstring;
	}

	/**
	 * Führt GET auf der angegebenen URI aus.
	 *
	 * @param uri Ziel-Adresse
	 * @return Response-Objekt der Anfrage
	 */
	public static Response callGetResponse(final String uri) {
		final JerseyWebTarget jwt = jc.target(uri);
		final Builder response = jwt.request();
		final Response r = response.get();
		return r;
	}

	/**
	 * Führt PUT mit den übergebenen Daten auf der übergebenen URI aus.
	 *
	 * @param uri Ziel-Adresse
	 * @param load Last, die übergeben werden soll
	 * @return Response-Objekt der Anfrage
	 */
	public static Response callPutResponse(final String uri, final String load) {
		final JerseyWebTarget jwt = jc.target(uri);
		final Builder response = jwt.request();
		final Response r = response.put(Entity.entity(load, MediaType.APPLICATION_JSON));
		return r;
	}

	/**
	 * Führt die HTTP DELETE Anfrage aus um einen laufen Simulationjob zu beenden.
	 *
	 * @param uri Zieladresse
	 * @return Antwort der Delete Anfrage
	 */
	public static Response callDeleteResponse(final String uri) {
		final JerseyWebTarget jwt = jc.target(uri);
		final Builder response = jwt.request();
		final Response r = response.delete();
		return r;
	}

	/**
	 * Führt die HTTP DELTE Anfrage aus um einen gespeicherten SimulationJob zu löschen und ihn evtl vorher zu beenden.
	 *
	 * @param uri Zieladresse mit SimulationJob id
	 * @param delete Ob gelöscht werden soll (bei false wird ein laufender Job dennoch beendet)
	 * @return 200 für ok oder 500 für Error
	 */
	public static Response callDeleteResponse(final String uri, final boolean delete) {
		final JerseyWebTarget jwt = jc.target(uri + "?delete=" + delete);
		final Builder response = jwt.request();
		final Response r = response.delete();
		return r;
	}

	public static Response callPost(final String uri, final String load) {
		final JerseyWebTarget jwt = jc.target(uri);
		final Builder response = jwt.request();
		final Response r = response.post(Entity.entity(load, MediaType.APPLICATION_JSON));
		return r;
	}

}
