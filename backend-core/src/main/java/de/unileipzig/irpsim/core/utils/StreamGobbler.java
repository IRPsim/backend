package de.unileipzig.irpsim.core.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Klasse fasst 2 Streams zu einer Ausgabe zusammen.
 * 
 * @author reichelt
 */
public final class StreamGobbler extends Thread {

	private static final Logger LOG = LogManager.getLogger(StreamGobbler.class);
	/**
	 * Der verwendete Inputstream.
	 */
	private final InputStream is;
	/**
	 * Typname des erstellten StreamGobblers.
	 */
	// private String type;
	/**
	 * Gibt an ob eine Zusamenfassung erstellt werden soll oder nicht.
	 */
	private final boolean summarize;
	/**
	 * Gibt an ob eine Ausgabe angezeigt werden soll oder nicht.
	 */
	private final boolean showOutput;
	/**
	 * Die statische Varialbe hält die Ausgabe als String.
	 */
	private static String output;
	static {
		output = "";
	}

	/**
	 * Konstruktor erzeugt neue StreamGobbler-Instanz.
	 * 
	 * @param is
	 *            Der vom StreamGobbler verwendete InputStream.
	 * @param type
	 *            Der Name des StreamGobblers.
	 * @param summarize
	 *            Zusammenfassen ja/nein.
	 * @param showOutput
	 *            Ausgabe anzeigen ja/nein.
	 */
	private StreamGobbler(final InputStream is, final String type, final boolean summarize, final boolean showOutput) {
		this.is = is;
		// this.type = type;
		this.summarize = summarize;
		this.showOutput = showOutput;

	}

	@Override
	public void run() {
		try {
			final InputStreamReader isr = new InputStreamReader(is);
			final BufferedReader br = new BufferedReader(isr);
			String line = null;
			while ((line = br.readLine()) != null) {
				if (summarize) {
					output += line + "\n";
				}
				if (showOutput) {
					System.out.println(line);
				}
			}
		} catch (final IOException ioe) {
			ioe.printStackTrace();
		}
	}

	/**
	 * Methode erstellt neuen StreamGobbler für die Prozessfehlerausgabe oder Prozessausgabe.
	 * 
	 * @param p
	 *            Auszugebender Prozess.
	 */
	public static void showFullProcess(final Process p) {
		final StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), "ERROR", false, true);

		final StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), "OUTPUT", false, true);

		// start gobblers
		outputGobbler.start();
		errorGobbler.start();

		try {
			outputGobbler.join();
			errorGobbler.join();
		} catch (final InterruptedException e) {
			LOG.error(e);
		}
	}

	public static void showFullProcessThrowing(final Process p) throws InterruptedException {
		final StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), "ERROR", false, true);

		final StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), "OUTPUT", false, true);

		// start gobblers
		outputGobbler.start();
		errorGobbler.start();

		outputGobbler.join();
		errorGobbler.join();
	}

	/**
	 * Methode erstellt neue StreamGobbler für die Prozessfehlerausgabe oder Prozessausgabe, liefert Ausgabe als String.
	 * 
	 * @param p
	 *            Auszugebender Prozess.
	 * @param showOutput
	 *            Gibt an ob die Ausgabe angezeigt werden soll oder nicht.
	 * @return Liefert die Ausgabe als String zurück.
	 */
	public static String getFullProcess(final Process p, final boolean showOutput) {
		output = ""; // Muss jedes mal geleert werden..
		final StreamGobbler errorGobbler = new StreamGobbler(p.getErrorStream(), "ERROR", true, showOutput);

		final StreamGobbler outputGobbler = new StreamGobbler(p.getInputStream(), "OUTPUT", true, showOutput);

		// start gobblers
		outputGobbler.start();
		errorGobbler.start();

		try {
			outputGobbler.join();
			errorGobbler.join();
		} catch (final InterruptedException e) {
			LOG.error(e);
		}
		return output;
	}
}
