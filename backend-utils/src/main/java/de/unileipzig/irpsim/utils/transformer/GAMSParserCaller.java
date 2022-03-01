package de.unileipzig.irpsim.utils.transformer;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.LazySeq;

/**
 * Beinhaltet Methoden zu Aufbereitung der GAMS-Daten in die für das frontend nötige Form.
 *
 * @author reichelt
 */
public final class GAMSParserCaller {
	private static final Logger LOG = LogManager.getLogger(GAMSParserCaller.class);

	static final ByteArrayOutputStream BAOS = new ByteArrayOutputStream();

	/**
	 * Privater Konstruktor.
	 */
	private GAMSParserCaller() {

	}

	/**
	 * Erstellt die Abhängigkeiten des Backends durch einen Clojure-Aufruf.
	 *
	 * @param source
	 *            Der Name des Quellverzeichnisses der Daten
	 * @param destination
	 *            Der Name der Zieldatei der Daten
	 */
	public static void createBackendDependencies(final String source, final String destination) {
		LOG.info("Quellordner: {} Zieldatei: {}", source, destination);

		final String result = callClojure(source, "backend-generator", "generate-backend-dependencies");
		final File f = new File(destination);
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
			bw.write(result);
			bw.flush();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Generiert mit Hilfe der callClojure()-Methode aus den GAMS-Daten die für das frontend nötigen Daten.
	 *
	 * @param source
	 *            Der Name des Quellverzeichnisses der Daten
	 * @param destination
	 *            Der name der Zieldatei der Daten
	 */
	public static void createFrontendData(final String source, final String destination) {
		// final String folder = ModelTransformer.MODELSOURCEFOLDER + model;
		LOG.info("Ordner: {}", source);

		final String result = callClojure(source, "frontend-generator", "-main");

		final File f = new File(destination);
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(f))) {
			bw.write(result);
			bw.flush();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Prüft das GAMS-Modell.
	 *
	 * @param sourceFolder
	 *            Ordner des GAMS-Modells
	 * @return Das Ergebnis des callClojureTests
	 */
	public static boolean checkGAMS(final String sourceFolder) {
		final boolean result = callClojureTest(sourceFolder, "check-gams-model", "check-model");
		if (!result) {
			LOG.info("Ordner: {}, Result: {}", sourceFolder, result);
		}
		// System.out.println("Result: " + result);
		return result;
	}

	/**
	 * Testet den Rückgabewert der callClojure()-Methode. Ob sie Ergebnisse liefert und diese Ergebnisse vom erwarteten Typen sind.
	 *
	 * @param folder
	 *            Der Ordner auf dem die Clojure-Methode aufgerufen werden soll
	 * @param clojureNamespace
	 *            Der Clojurenamespace
	 * @param clojureMethod
	 *            Die auszuführende Clojure-Methode
	 * @return true falls die Größe des zurückzugebenden Elements Null ist false sonst
	 */
	public static boolean callClojureTest(final String folder, final String clojureNamespace, final String clojureMethod) {

		final PrintStream localOut = System.out;
		final PrintStream localErr = System.err;
		Object returnObject;
		try {
			System.setOut(new PrintStream(BAOS));
			System.setErr(new PrintStream(BAOS));

			final IFn require = Clojure.var("clojure.core", "require");
			final Object read = Clojure.read(clojureNamespace);
			require.invoke(read);

			final IFn generateDependencies = Clojure.var(clojureNamespace, clojureMethod);
			returnObject = generateDependencies.invoke(folder);
			System.out.flush();
		} finally {
			System.setOut(localOut);
			System.setErr(localErr);
		}
		if (returnObject instanceof LazySeq) {
			final LazySeq ls = (LazySeq) returnObject;
			for (int i = 0; i < ls.count(); i++) {
				LOG.debug("Test: {} {}", i, ls.get(i));
			}
			LOG.debug("LS: " + ls + " " + ls.count());
			return ls.count() == 0;
		} else {
			LOG.error("Unerwarteter Typ: {}", returnObject);
		}

		return false;
	}

	/**
	 * Läd die Clojure Bibliotheken zur interaktion mit java, ruft eine Clojuremethode auf einem mitgegebenen Quelle auf und stellt das Ergebnis in einem OutputStream zur Verfügung.
	 *
	 * @param folder
	 *            Name des Ordners auf dem die Closjuremethode aufgerufen wird
	 * @param clojureNamespace
	 *            Der Clojure-namespace
	 * @param clojureMethod
	 *            Der Name der aufzurufenden Closjuremethode
	 * @return Outputstream mit Ergebnis
	 */
	public static String callClojure(final String folder, final String clojureNamespace, final String clojureMethod) {
		final PrintStream localOut = System.out;
		final PrintStream localErr = System.err;
		try {
			System.setOut(new PrintStream(BAOS));
			System.setErr(new PrintStream(BAOS));

			final IFn require = Clojure.var("clojure.core", "require");
			require.invoke(Clojure.read(clojureNamespace));

			final IFn generateDependencies = Clojure.var(clojureNamespace, clojureMethod);
			generateDependencies.invoke(folder);
			System.out.flush();
		} finally {
			System.setOut(localOut);
			System.setErr(localErr);
		}

		final String result = BAOS.toString();
		BAOS.reset();
		return result;
	}
}
