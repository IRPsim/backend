package de.unileipzig.irpsim.core.simulation.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Wird geworfen, wenn eine Zeitreihe die als Referenz übergeben wurde in der Datenbank zu kurz ist.
 * 
 * @author reichelt
 */
public class TimeseriesTooShortException extends Exception {
	private static final long serialVersionUID = 1674482155493437935L;
	private static final Logger LOG = LogManager.getLogger(TimeseriesTooShortException.class);

	public TimeseriesTooShortException(final String errorMessage) {
	   super(errorMessage);
	   LOG.debug(errorMessage);
	}

}
