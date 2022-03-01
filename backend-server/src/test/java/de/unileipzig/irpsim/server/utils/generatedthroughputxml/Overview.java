//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802 generiert 
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren. 
// Generiert: 2015.09.18 um 04:26:11 PM CEST 
//


package de.unileipzig.irpsim.server.utils.generatedthroughputxml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * <p>Java-Klasse für anonymous complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}scenario"/>
 *         &lt;element ref="{}bestThroughputRatio"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "scenario",
    "bestThroughputRatio"
})
@XmlRootElement(name = "overview")
public class Overview {

    @XmlElement(required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlSchemaType(name = "NCName")
    protected String scenario;
    @XmlElement(required = true)
    protected BestThroughputRatio bestThroughputRatio;

    /**
     * Ruft den Wert der scenario-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getScenario() {
        return scenario;
    }

    /**
     * Legt den Wert der scenario-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setScenario(String value) {
        this.scenario = value;
    }

    /**
     * Ruft den Wert der bestThroughputRatio-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link BestThroughputRatio }
     *     
     */
    public BestThroughputRatio getBestThroughputRatio() {
        return bestThroughputRatio;
    }

    /**
     * Legt den Wert der bestThroughputRatio-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link BestThroughputRatio }
     *     
     */
    public void setBestThroughputRatio(BestThroughputRatio value) {
        this.bestThroughputRatio = value;
    }

}
