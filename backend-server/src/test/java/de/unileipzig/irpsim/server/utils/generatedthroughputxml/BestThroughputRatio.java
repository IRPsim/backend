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
 *         &lt;element ref="{}numberOfParallelProcesses"/>
 *         &lt;element ref="{}avgThroughputPerHour"/>
 *         &lt;element ref="{}rsd"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
		"numberOfParallelProcesses",
		"avgThroughputPerHour",
		"rsd"
})
@XmlRootElement(name = "bestThroughputRatio")
public final class BestThroughputRatio {

	@XmlElement(required = true)
	private int numberOfParallelProcesses;
	@XmlElement(required = true)
	private double avgThroughputPerHour;
	@XmlElement(required = true)
	private double rsd;

	/**
	 * Ruft den Wert der numberOfParallelProcesses-Eigenschaft ab.
	 *
	 * @return possible object is {@link int }
	 */
	public int getNumberOfParallelProcesses() {
		return numberOfParallelProcesses;
	}

	/**
	 * Legt den Wert der numberOfParallelProcesses-Eigenschaft fest.
	 *
	 * @param value allowed object is {@link int }
	 */
	public void setNumberOfParallelProcesses(final int value) {
		this.numberOfParallelProcesses = value;
	}

	/**
	 * Ruft den Wert der avgThroughputPerHour-Eigenschaft ab.
	 *
	 * @return possible object is {@link int }
	 */
	public double getAvgThroughputPerHour() {
		return avgThroughputPerHour;
	}

	/**
	 * Legt den Wert der avgThroughputPerHour-Eigenschaft fest.
	 *
	 * @param value allowed object is {@link int }
	 */
	public void setAvgThroughputPerHour(final double value) {
		this.avgThroughputPerHour = value;
	}

	/**
	 * Ruft den Wert der rsd-Eigenschaft ab.
	 *
	 * @return possible object is {@link int }
	 */
	public double getRsd() {
		return rsd;
	}

	/**
	 * Legt den Wert der rsd-Eigenschaft fest.
	 *
	 * @param value allowed object is {@link int }
	 */
	public void setRsd(final double value) {
		this.rsd = value;
	}

}
