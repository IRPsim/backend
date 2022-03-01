/**
 *
 */
package de.unileipzig.irpsim.server.data;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.exception.ExceptionUtils;

import de.unileipzig.irpsim.core.data.JSONErrorMessage;

/**
 * Create error messages to be returned in Endpoints. Automatically sends error as JSON in a homogenous format.
 *
 * @author sdienst
 */
public final class Responses {

	/**
	 * Privater Konstruktor.
	 */
	private Responses() {

	}

	/**
	 * Liefert die mitgegebene Nachricht als errorResponse.
	 *
	 * @param s
	 *            Die zu setzende Nachricht
	 * @return Fehlermeldung mit Nachricht
	 */
	public static Response errorResponse(final String s) {
		return errorResponse(s, "Fehler");
	}

	/**
	 * Liefert eine Throwable Response.
	 *
	 * @param e
	 *            Das Throwable Objekt
	 * @return Throwable errorResponse
	 */
	public static Response errorResponse(final Throwable e) {
		return errorResponse(e, "Interner Fehler");
	}

	/**
	 * Liefert eine Fehlermeldung mit Titel und Nachricht.
	 *
	 * @param e
	 *            Das Throwable Objekt
	 * @param title
	 *            Der Titel der Nachricht
	 * @return Throwable errorResponse mit Titel und Nachricht
	 */
	public static Response errorResponse(final Throwable e, final String title) {
		final String contentString = e.getLocalizedMessage() + "\n" + ExceptionUtils.getStackTrace(e);
		return errorResponse(contentString, title);
	}

	/**
	 * Liefert eine Statusmeldung mit Titel und Nachricht.
	 *
	 * @param s
	 *            Die Nachricht der Meldung
	 * @param title
	 *            Der Titel der Meldung
	 * @return Statusmeldung mit Titel und Nachricht
	 */
	public static Response errorResponse(final String s, final String title) {
		final JSONErrorMessage jm = new JSONErrorMessage(s, title, JSONErrorMessage.Severity.ERROR);
		return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(jm.createPureJSON()).build();
	}

	/**
	 * @param message
	 *            Nachricht
	 * @param title
	 *            Statustitel
	 * @return Response mit code 200 und Nachricht
	 */
	public static Response okResponse(final String message, final String title) {
		return okResponse(message, title, JSONErrorMessage.Severity.SUCCESS);
	}

	public static Response okResponse(final String message, final String title, final JSONErrorMessage.Severity severity) {
		final JSONErrorMessage jm = new JSONErrorMessage(message, title, severity);
		return Response.status(Response.Status.OK).entity(jm.createPureJSON()).build();
	}

	/**
	 * Liefert Fehlermeldung mit Nachricht für falsche oder fehlerhaft Anfragen.
	 *
	 * @param s
	 *            Die Nachricht der Fehlermeldung
	 * @return Fehlermeldung
	 */
	public static Response badRequestResponse(final String s) {
		return badRequestResponse(s, "Fehler der Anfrage");
	}

	public static Response conflictResponse(final String s) {
		final JSONErrorMessage jm = new JSONErrorMessage(s, "Fehler der Anfrage", JSONErrorMessage.Severity.ERROR);
		return Response.status(Response.Status.CONFLICT).entity(jm.createPureJSON()).build();
	}

	/**
	 * Liefert eine Throwable Fehlermeldung mit Nachricht für falsche oder fehlerhaft Anfragen.
	 *
	 * @param e
	 *            Das Throwable-Objekt
	 * @return Fehlermeldung
	 */
	public static Response badRequestResponse(final Throwable e) {
		return badRequestResponse(e, "Fehler der Anfrage");
	}

	/**
	 * Liefert eine Throwable Fehlermeldung mit Nachricht für falsche oder fehlerhaft Anfragen mit Titel.
	 *
	 * @param e
	 *            Das Throwable-Objekt
	 * @param title
	 *            Titel der Fehlermeldung
	 * @return Fehlermeldung mit Titel und Nachricht
	 */
	public static Response badRequestResponse(final Throwable e, final String title) {
		return badRequestResponse(e.getLocalizedMessage(), title);
	}

	/**
	 * Liefert eine Statusmeldung bei Fehlerhaften Anfragen.
	 *
	 * @param s
	 *            Nachricht der Fehlermeldung
	 * @param title
	 *            Titel der Fehermeldung
	 * @return Statusmeldung mit Titel und Nachricht
	 */
	public static Response badRequestResponse(final String s, final String title) {
		final JSONErrorMessage jm = new JSONErrorMessage(s, title, JSONErrorMessage.Severity.ERROR);
		return Response.status(Response.Status.BAD_REQUEST).entity(jm.createPureJSON()).build();
	}

	/**
	 * Liefert eine Throwable Fehlermeldung mit Nachricht für Anfragen, für die kein korrekter Endpoint gefunden wurde.
	 *
	 * @param e
	 *            Das Throwable-Objekt
	 * @return Fehlermeldung
	 */
	public static Response notFoundResponse(final Throwable e) {
		return notFoundResponse(e, "Fehlerhafte URI");
	}

	/**
	 * Liefert eine Throwable Fehlermeldung mit Nachricht für Anfragen, für die kein korrekter Endpoint gefunden wurde.
	 *
	 * @param e
	 *            Das Throwable-Objekt
	 * @param title
	 *            Titel der Fehlermeldung
	 * @return Fehlermeldung mit Titel und Nachricht
	 */
	public static Response notFoundResponse(final Throwable e, final String title) {
		return notFoundResponse(e.getLocalizedMessage(), title);
	}

	/**
	 * Liefert eine Statusmeldung bei Anfragen, für die kein korrekter Endpoint gefunden wurde.
	 *
	 * @param message
	 *            Nachricht der Fehlermeldung
	 * @param title
	 *            Titel der Fehermeldung
	 * @return Statusmeldung mit Titel und Nachricht
	 */
	public static Response notFoundResponse(final String message, final String title) {
		final JSONErrorMessage jm = new JSONErrorMessage(message, title, JSONErrorMessage.Severity.ERROR);
		return Response.status(Response.Status.NOT_FOUND).entity(jm.createPureJSON()).build();
	}
}
