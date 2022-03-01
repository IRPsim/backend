package de.unileipzig.irpsim.utils.nonautomated;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

/**
 * Fasst die performance.csv aus den Performanztests zusammen (d.h. berechnet Mittelwerte).
 *
 * @author reichelt
 */
public final class SummarizePerformanceUtil {

	/**
	 * Privater Konstruktor.
	 */
	private SummarizePerformanceUtil() {
	}

	/**
	 * Führt die Zusammenfassung der Performanztests aus.
	 * 
	 * @param args Programmstartargumente
	 * @throws FileNotFoundException Tritt auf falls die zu parsende .csv-Datei nicht gefunden werden kannn
	 * @throws IOException Tritt auf falls Fehler beim Lesen und Schreiben der Datei auftreten
	 * @throws InterruptedException Tritt auf falls der Thread schläft, unterbricht,
	 */
	public static void main(final String[] args) throws FileNotFoundException, IOException, InterruptedException {
		final File f = new File(args[0]);
		final CSVFormat format = CSVFormat.DEFAULT.withDelimiter(',');
		final CSVParser cp = new CSVParser(new FileReader(f), format);

		final Map<Integer, List<Double>> creationTime = new TreeMap<>();
		final Map<Integer, List<Double>> apiTime = new TreeMap<>();

		for (final CSVRecord record : cp) {
			final Integer count = Integer.valueOf(record.get(0));
			if (!creationTime.containsKey(count)) {
				creationTime.put(count, new LinkedList<Double>());
			}
			creationTime.get(count).add(Double.valueOf(record.get(1)));
			if (record.size() > 2) {
				if (!apiTime.containsKey(count)) {
					apiTime.put(count, new LinkedList<Double>());
				}
				apiTime.get(count).add(Double.valueOf(record.get(2)));
			}
		}
		cp.close();

		final BufferedWriter bw = new BufferedWriter(new FileWriter("performance2.csv"));

		for (final Map.Entry<Integer, List<Double>> creation : creationTime.entrySet()) {
			final List<Double> creationTimes = creation.getValue();
			final List<Double> apiTimes = apiTime.get(creation.getKey());

			final double avgCreation = creationTimes.stream().mapToDouble(i -> i).average().orElse(0.0);
			double avgTime = 0.0;
			if (apiTime.size() > 0) {
				avgTime = apiTimes.stream().mapToDouble(i -> i).average().orElse(0.0);
			}

			bw.write(creation.getKey() + "," + avgCreation + "," + avgTime + "\n");
			System.out.println(creation.getKey() + "," + avgCreation + "," + avgTime);
		}
		bw.flush();
		bw.close();
		final ProcessBuilder pb = new ProcessBuilder("./plot.plt");
		pb.redirectOutput(new File("plot.png"));
		final Process p = pb.start();
		p.waitFor();
	}
}
