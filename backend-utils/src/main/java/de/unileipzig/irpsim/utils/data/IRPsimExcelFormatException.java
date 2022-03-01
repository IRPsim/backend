package de.unileipzig.irpsim.utils.data;

/**
 * Exception sollte geworfen werden, wenn das Format der angegebenen Exceldatei nicht verarbeitbar ist.
 *
 * @author krauss
 */
public final class IRPsimExcelFormatException extends Exception {
	private static final long serialVersionUID = 321911718114643848L;

	/**
	 *
	 */
	public IRPsimExcelFormatException() {
		super();
	}

	/**
	 * @param message Die zu übergebende Nachricht
	 */
	public IRPsimExcelFormatException(final String message) {
		super(message);
	}
}