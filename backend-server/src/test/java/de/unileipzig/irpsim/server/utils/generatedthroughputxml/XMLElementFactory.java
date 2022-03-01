//
// Diese Datei wurde mit der JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// generiert
// Siehe <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Änderungen an dieser Datei gehen bei einer Neukompilierung des Quellschemas verloren.
// Generiert: 2015.09.18 um 04:26:11 PM CEST
//

package de.unileipzig.irpsim.server.utils.generatedthroughputxml;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;

/**
 * This object contains factory methods for each Java content interface and Java element interface generated in the
 * de.unileipzig.irpsim.server.utils package.
 * <p>
 * An ObjectFactory allows you to programatically construct new instances of the Java representation for XML content.
 * The Java representation of XML content can consist of schema derived interfaces and classes representing the binding
 * of schema type definitions, element declarations and model groups. Factory methods for each of these are provided in
 * this class.
 */
@XmlRegistry
public final class XMLElementFactory {

	private static final QName _NumberOfSequentialProcesses_QNAME = new QName("", "numberOfSequentialProcesses");
	private static final QName _AvgThroughputPerHour_QNAME = new QName("", "avgThroughputPerHour");
	private static final QName _AvgMinutesPerProcess_QNAME = new QName("", "avgMinutesPerProcess");
	private static final QName _Sd_QNAME = new QName("", "sd");
	private static final QName _Rsd_QNAME = new QName("", "rsd");
	private static final QName _Scenario_QNAME = new QName("", "scenario");
	private static final QName _NumberOfParallelProcesses_QNAME = new QName("", "numberOfParallelProcesses");
	private static final QName _Start_QNAME = new QName("", "start");
	private static final QName _End_QNAME = new QName("", "end");

	/**
	 * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package:
	 * de.unileipzig.irpsim.server.utils
	 */
	public XMLElementFactory() {
	}

	/**
	 * Create an instance of {@link Overview }
	 */
	public Overview createOverview() {
		return new Overview();
	}

	/**
	 * Create an instance of {@link BestThroughputRatio }
	 */
	public BestThroughputRatio createBestThroughputRatio() {
		return new BestThroughputRatio();
	}

	/**
	 * Create an instance of {@link Run }
	 */
	public Run createRun() {
		return new Run();
	}

	/**
	 * Create an instance of {@link RawData }
	 */
	public RawData createRawData() {
		return new RawData();
	}

	/**
	 * Create an instance of {@link Runs }
	 */
	public Runs createRuns() {
		return new Runs();
	}

	/**
	 * Create an instance of {@link ThroughputResult }
	 */
	public ThroughputResult createThroughputResult() {
		return new ThroughputResult();
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
	 */
	@XmlElementDecl(namespace = "", name = "numberOfSequentialProcesses")
	public JAXBElement<BigInteger> createNumberOfSequentialProcesses(final BigInteger value) {
		return new JAXBElement<BigInteger>(_NumberOfSequentialProcesses_QNAME, BigInteger.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link BigDecimal }{@code >}}
	 */
	@XmlElementDecl(namespace = "", name = "avgThroughputPerHour")
	public JAXBElement<BigDecimal> createAvgThroughputPerHour(final BigDecimal value) {
		return new JAXBElement<BigDecimal>(_AvgThroughputPerHour_QNAME, BigDecimal.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link BigDecimal }{@code >}}
	 */
	@XmlElementDecl(namespace = "", name = "avgMinutesPerProcess")
	public JAXBElement<BigDecimal> createAvgMinutesPerProcess(final BigDecimal value) {
		return new JAXBElement<BigDecimal>(_AvgMinutesPerProcess_QNAME, BigDecimal.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link BigDecimal }{@code >}}
	 */
	@XmlElementDecl(namespace = "", name = "sd")
	public JAXBElement<BigDecimal> createSd(final BigDecimal value) {
		return new JAXBElement<BigDecimal>(_Sd_QNAME, BigDecimal.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link BigDecimal }{@code >}}
	 */
	@XmlElementDecl(namespace = "", name = "rsd")
	public JAXBElement<BigDecimal> createRsd(final BigDecimal value) {
		return new JAXBElement<BigDecimal>(_Rsd_QNAME, BigDecimal.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
	 */
	@XmlElementDecl(namespace = "", name = "scenario")
	@XmlJavaTypeAdapter(CollapsedStringAdapter.class)
	public JAXBElement<String> createScenario(final String value) {
		return new JAXBElement<String>(_Scenario_QNAME, String.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
	 */
	@XmlElementDecl(namespace = "", name = "numberOfParallelProcesses")
	public JAXBElement<BigInteger> createNumberOfParallelProcesses(final BigInteger value) {
		return new JAXBElement<BigInteger>(_NumberOfParallelProcesses_QNAME, BigInteger.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
	 */
	@XmlElementDecl(namespace = "", name = "start")
	public JAXBElement<BigInteger> createStart(final BigInteger value) {
		return new JAXBElement<BigInteger>(_Start_QNAME, BigInteger.class, null, value);
	}

	/**
	 * Create an instance of {@link JAXBElement }{@code <}{@link BigInteger }{@code >}}
	 */
	@XmlElementDecl(namespace = "", name = "end")
	public JAXBElement<BigInteger> createEnd(final BigInteger value) {
		return new JAXBElement<BigInteger>(_End_QNAME, BigInteger.class, null, value);
	}

}
