package de.unileipzig.irpsim.core.data;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Erzeugt eine Fehlermeldung im JSON-Format mit Nachricht, Titel und Schweregrad/Ernsthaftigkeit des Fehlers.
 *
 * @author reichelt
 */
public class JSONErrorMessage {
	/**
	 * Kapselt Auskunft über den Schweregrad des Fehlers.
	 *
	 * @author reichelt
	 */
	public enum Severity {
		ERROR, INFO, WARNING, SUCCESS;
	}

	private String text;
	private String title;
	private Severity severity;

	/**
	 * Default Konstrktor.
	 */
	public JSONErrorMessage() {

	}

	/**
	 * Initialisiert die JSON-Meldung ausschließlich mit einem Text.
	 *
	 * @param text
	 *            Nachricht der Fehlermeldung
	 */
	public JSONErrorMessage(final String text) {
		super();
		this.text = text;
	}

	/**
	 * Initialisiert die JSON-Meldung mit Text, Titel und Ernsthaftigkeit.
	 *
	 * @param text
	 *            Die Nachricht der Fehlermeldung
	 * @param title
	 *            Der Titel der Fehlermeldung
	 * @param severity
	 *            Die Ernsthaftigkeit der Fehlermeldung
	 */
	public JSONErrorMessage(final String text, final String title, final Severity severity) {
		super();
		this.text = text;
		this.title = title;
		this.severity = severity;
	}

	/**
	 * Liefert die textNachricht der JSON-Fehlermeldung.
	 *
	 * @return Die Textnachricht der Fehlermeldung
	 */
	public final String getText() {
		return text;
	}

	/**
	 * Setzt die Nachricht der Fehlermeldung.
	 *
	 * @param text
	 *            Die zu setzende Nachricht der Fehlermeldung
	 */
	public final void setText(final String text) {
		this.text = text;
	}

	/**
	 * Liefert den Titel der Fehlermeldung.
	 *
	 * @return Der Titel der Fehlermeldung als String
	 */
	public final String getTitle() {
		return title;
	}

	/**
	 * Setzt den Titel der Fehlermeldung.
	 *
	 * @param title
	 *            Der zu setzende Titel der Fehlermeldung.
	 */
	public final void setTitle(final String title) {
		this.title = title;
	}

	/**
	 * Liefert die Ernsthaftigkeit der Fehlermeldung.
	 *
	 * @return Die serberity als Severity
	 */
	public final Severity getSeverity() {
		return severity;
	}

	/**
	 * Setzt die Ernsthafigkeit der Fehlermeldung.
	 *
	 * @param severity
	 *            Die zu setzende Ernsthaftigkeit
	 */
	public final void setSeverity(final Severity severity) {
		this.severity = severity;
	}

	/**
	 * Konvertieret und Liefert die Fehlermeldung im JSON-Format.
	 *
	 * @return String im JSON-Format
	 */
	@JsonIgnore
	public final String toJSON() {
		try {
			final ObjectMapper objectMapper = new ObjectMapper();
			final String writeValueAsString = objectMapper.writeValueAsString(this);
			return writeValueAsString;
		} catch (final JsonProcessingException e) {
			e.printStackTrace();
		}
		return "Error building error message!";
	}

	/**
	 * Fügt eine Nachricht im JSON-Format an ein mitgegbenes JSON-Objekt an.
	 *
	 * @param jso
	 *            Das zu erwiternde JSON-Objekt
	 */
	@JsonIgnore
	public final void enrichJSONObject(final JSONObject jso) {
		jso.put("messages", new JSONArray(toJSON()));
	}

	/**
	 * Konvertiert ein JSONObject zu einem String im JSON-Format.
	 *
	 * @return String im JSON-Format
	 */
	@JsonIgnore
	public final String createPureJSON() {
		final JSONObject jso = new JSONObject();
		jso.put("messages", new JSONArray(new Object[] { new JSONObject(this.toJSON()) }));
		final String result = jso.toString();
		return result;
	}
}
