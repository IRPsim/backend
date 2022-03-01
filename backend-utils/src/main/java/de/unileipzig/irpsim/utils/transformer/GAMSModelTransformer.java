package de.unileipzig.irpsim.utils.transformer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Transormiert das GAMS-Model in der Version, die die Excel-Datei als Eingabe nutzt, sowie in eine Version, die die Datenbank als Eingabe nutzt.
 *
 * @author reichelt
 *
 */
public final class GAMSModelTransformer {

	private static final Logger LOG = LogManager.getLogger(GAMSModelTransformer.class);

	/**
	 * Transformiert ein Modell, d.h. nutzt das von Fachexperten definierte Modell, bearbeitet das GDX-Einlesen und schreibt das geänderte Modell dann in den Ausgabeordner.
	 *
	 * @param source Der Name der Quelldatei
	 * @param destination Der Name der Zieldatei
	 * @throws IOException Tritt auf falls Fehler beim lesen und schreiben auftrerten
	 */
	public static void transformModel(final File source, final File destFolder) throws IOException {

		// final File destFolder = new File(destination);
		if (destFolder.exists()) {
			FileUtils.deleteDirectory(destFolder);
		}
		LOG.debug("Kopiere von " + source + " zu " + destFolder);
		prepareGAMSModelFolders(source, destFolder);

		prepareMainModel(destFolder);
	}

	private static void prepareMainModel(final File destFolder) throws IOException {
		final File sourceMainFile = new File(destFolder, "main.gms");
		LOG.debug("MainFile: " + sourceMainFile.getAbsolutePath());
		final Stream<String> lines = Files.lines(sourceMainFile.toPath(), Charset.forName("ISO-8859-15"));
		final File mainTempFile = new File(destFolder, "/main_temp.gms");
		Boolean foundGDXIN = false;
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(mainTempFile))) {
			for (final String line : lines.collect(Collectors.toList())) {
				try {
					if (line.startsWith("$CALL GDXXRW") || line.startsWith("execute_unload") || line.startsWith("execute 'gdxxrw.exe")) {
						bw.write("***" + line + "\n");
					} else if (line.startsWith("$GDXIN modelinput")) {
						LOG.debug("GDXIN gefunden");
						bw.write("$GDXIN %gdxincname%\n");
						foundGDXIN = true;
					} else {
						bw.write(line + "\n");
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
			bw.flush();
		}
		lines.close();

		if (!foundGDXIN) {
			throw new RuntimeException("GDXIN modelinput nicht gefunden - Transformation fehlgeschlagen.");
		}

		sourceMainFile.delete();
		mainTempFile.renameTo(sourceMainFile);
	}

	private static void prepareGAMSModelFolders(final File source, final File destFolder) throws IOException {
		FileUtils.copyDirectory(source, destFolder,
				pathname -> pathname.isDirectory() || pathname.getName().endsWith("gms") || pathname.getName().endsWith("opt") || pathname.getName().equals("csv.txt"));
		final File resultFolder = new File(destFolder, "output/results");
		resultFolder.mkdir();
		final File doNotDeleteFolderFile = new File(resultFolder, "doNotDeleteFolder.txt");
		try (FileWriter fw = new FileWriter(doNotDeleteFolderFile)) {
			fw.write("");
		}
	}
}
