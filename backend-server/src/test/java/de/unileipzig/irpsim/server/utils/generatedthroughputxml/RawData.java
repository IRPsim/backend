//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// generiert
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren.
// Generiert: 2015.09.18 um 04:26:11 PM CEST
//

package de.unileipzig.irpsim.server.utils.generatedthroughputxml;

import java.util.ArrayList;
import java.util.List;

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
 *         &lt;element ref="{}start"/>
 *         &lt;element ref="{}end" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
		"start",
		"end"
})
@XmlRootElement(name = "rawData")
public final class RawData {

	@XmlElement(required = true)
	private long start;
	@XmlElement(required = true)
	private List<Long> end;

	/**
	 * Ruft den Wert der start-Eigenschaft ab.
	 *
	 * @return possible object is {@link int }
	 */
	public long getStart() {
		return start;
	}

	/**
	 * Legt den Wert der start-Eigenschaft fest.
	 *
	 * @param value allowed object is {@link int }
	 */
	public void setStart(final long value) {
		this.start = value;
	}

	/**
	 * Gets the value of the end property.
	 * <p>
	 * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
	 * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
	 * the end property.
	 * <p>
	 * For example, to add a new item, do as follows:
	 *
	 * <pre>
	 * getEnd().add(newItem);
	 * </pre>
	 * <p>
	 * Objects of the following type(s) are allowed in the list {@link int }
	 *
	 * @return Die Liste der Endzeitpunkte
	 */
	public List<Long> getEnd() {
		if (end == null) {
			end = new ArrayList<Long>();
		}
		return this.end;
	}

}
