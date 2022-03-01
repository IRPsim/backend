//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// generiert
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren.
// Generiert: 2015.09.18 um 04:26:11 PM CEST
//

package de.unileipzig.irpsim.server.utils.generatedthroughputxml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

/**
 * <p>
 * Java-Klasse für anonymous complex type.
 * <p>
 * Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}overview"/>
 *         &lt;element ref="{}runs"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
		"overview",
		"runs"
})
@XmlRootElement(name = "throughputResult")
public final class ThroughputResult {

	@XmlElement(required = true)
	private Overview overview;
	@XmlElement(required = true)
	private Runs runs;

	/**
	 * Ruft den Wert der overview-Eigenschaft ab.
	 *
	 * @return possible object is {@link Overview }
	 */
	public Overview getOverview() {
		return overview;
	}

	/**
	 * Legt den Wert der overview-Eigenschaft fest.
	 *
	 * @param value allowed object is {@link Overview }
	 */
	public void setOverview(final Overview value) {
		this.overview = value;
	}

	/**
	 * Ruft den Wert der runs-Eigenschaft ab.
	 *
	 * @return possible object is {@link Runs }
	 */
	public Runs getRuns() {
		return runs;
	}

	/**
	 * Legt den Wert der runs-Eigenschaft fest.
	 *
	 * @param value allowed object is {@link Runs }
	 */
	public void setRuns(final Runs value) {
		this.runs = value;
	}

}
