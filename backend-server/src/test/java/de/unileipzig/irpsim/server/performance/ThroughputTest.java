package de.unileipzig.irpsim.server.performance;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import de.unileipzig.irpsim.core.utils.TestFiles;
import de.unileipzig.irpsim.server.marker.PerformanceTest;
import de.unileipzig.irpsim.server.optimisation.queue.OptimisationJobHandler;
import de.unileipzig.irpsim.server.utils.RESTCaller;
import de.unileipzig.irpsim.server.utils.ServerTestUtils;
import de.unileipzig.irpsim.server.utils.ServerTests;
import de.unileipzig.irpsim.server.utils.generatedthroughputxml.BestThroughputRatio;
import de.unileipzig.irpsim.server.utils.generatedthroughputxml.Overview;
import de.unileipzig.irpsim.server.utils.generatedthroughputxml.RawData;
import de.unileipzig.irpsim.server.utils.generatedthroughputxml.Run;
import de.unileipzig.irpsim.server.utils.generatedthroughputxml.Runs;
import de.unileipzig.irpsim.server.utils.generatedthroughputxml.ThroughputResult;
import de.unileipzig.irpsim.server.utils.generatedthroughputxml.XMLElementFactory;

/**
 * Dient dem Testen der Auslastung der Rechenleistung. Rationale: Verschiedene Gams Solver nutzen möglicherweise mehrere Kerne oder auch nicht, mehrere Berechnungen gleichzeitig ausführen lassen kann
 * also zu verschiedenen Performanceeinflüssen führen. Diese Effekte sollen hiermit getestet werden.
 *
 * @author krauss
 */
@Category({ PerformanceTest.class })
public final class ThroughputTest extends ServerTests {

	private static Path logFile = Paths.get("target/site/throughputTests.log");
	private int oldMaxParallelJobs = 2;

	private static final File TEST_SCENARIO = TestFiles.FULL_YEAR.make();
	private static final int MIN_NUMBER_OF_PARALLEL_JOBS = 1, MAX_NUMBER_OF_PARALLEL_JOBS = 5, NUMBER_OF_SEQUENTIAL_JOBS = 4;
	private static final double MAX_ALLOWED_DROP_RATE = 0.1;
	private static final File TARGET_FILE = new File("src/test/throughput_result_" + new Date().toString() + ".xml");

	/**
	 * Erzeugt die logFile, in der die Ergebnisse gespeichert werden.
	 */
	@BeforeClass
	public static void createLogFile() {
		try {
			Files.createDirectories(logFile.getParent());
			if (!Files.exists(logFile)) {
				Files.createFile(logFile);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Speichert bisherige Anzahl maximal gleichzeitig auszuführender Optimierungen.
	 */
	@Before
	public void rememberMaxJobs() {
		oldMaxParallelJobs = OptimisationJobHandler.getMaxParallelJobs();
	}

	/**
	 * Stellt ursprüngliche Anzahl maximal auszuführender Optimierungen wieder her.
	 */
	@After
	public void resetMaxJobs() {
		OptimisationJobHandler.setMaxParallelJobs(oldMaxParallelJobs);
	}

	/**
	 * Testet die Dauer einer einzelnen Optimierung abhängig von der Anzahl parallel laufender Jobs. TODO: REST Anfrage vs. direkter Test
	 */
	@Test
	public void testThroughput() {
		try {
			final long refScenario = ServerTestUtils.putScenarioWithREST(TEST_SCENARIO);
			final String scenario = RESTCaller.callGet(ServerTestUtils.SZENARIEN_URI + "/" + refScenario);

			final Runs runs = new XMLElementFactory().createRuns();
			for (int numberOfParallelJobs = MIN_NUMBER_OF_PARALLEL_JOBS; numberOfParallelJobs <= MAX_NUMBER_OF_PARALLEL_JOBS; numberOfParallelJobs++) {
				OptimisationJobHandler.setMaxParallelJobs(numberOfParallelJobs);
				final List<Long> ids = new LinkedList<>();

				final long startTime = new Date().getTime();
				for (int job = 0; job < numberOfParallelJobs * NUMBER_OF_SEQUENTIAL_JOBS; job++) {
					ids.add(startSimulation(scenario));
				}
				final List<Long> endTimes = ServerTestUtils.getInstance().waitForPreciseOptimisationEnds(ids);
				final Run run = singleRunResult(numberOfParallelJobs, startTime, endTimes);
				runs.getRun().add(run);
				if (throughputDroppedBy(MAX_ALLOWED_DROP_RATE, runs)) {
					break;
				}
			}
			final ThroughputResult result = drawConclusion(runs);
			fetchXMLConverter().marshal(result, TARGET_FILE);
		} catch (final IOException e) {
			e.printStackTrace();
		} catch (final JAXBException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @return Gibt einen generischen JAXB Marshaller zurück.
	 */
	private Marshaller fetchXMLConverter() throws JAXBException, PropertyException {
		final JAXBContext resultContext = JAXBContext.newInstance(ThroughputResult.class);
		final Marshaller converter = resultContext.createMarshaller();
		converter.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		return converter;
	}

	/**
	 * Gibt das Ergebnis eines TestRuns an.
	 *
	 * @param numberOfParallelJobs
	 *            Zugelassene Anzahl parallel laufender Jobs
	 * @param startTime
	 *            Die Zeit zu der der erste Job gestartet wurde
	 * @param endTimes
	 *            Zeitdauer, die es gedauert hat
	 * @return Gibt die durchschnittliche Zeit pro Job wieder
	 */
	private Run singleRunResult(final int numberOfParallelJobs, final long startTime, final List<Long> endTimes) throws IOException {
		final XMLElementFactory factory = new XMLElementFactory();
		final Run run = factory.createRun();
		run.setRawData(factory.createRawData());
		run.setNumberOfSequentialProcesses(NUMBER_OF_SEQUENTIAL_JOBS);
		run.setNumberOfParallelProcesses(numberOfParallelJobs);

		final RawData rawTimes = run.getRawData();
		rawTimes.setStart(startTime);
		rawTimes.getEnd().addAll(endTimes);

		final List<Long> sortedEnds = new ArrayList<>(endTimes);
		Collections.sort(sortedEnds);
		final List<Long> times = new ArrayList<>(sortedEnds.size());
		double avgTime = 0;
		for (int count = 0; count < sortedEnds.size(); count++) {
			if (count < numberOfParallelJobs) {
				times.add(count, sortedEnds.get(count) - startTime);
			} else {
				times.add(count, sortedEnds.get(count) - sortedEnds.get(count - numberOfParallelJobs));
			}
			avgTime += times.get(count) / sortedEnds.size();
		}
		run.setAvgMinutesPerProcess(avgTime / 60000);
		run.setAvgThroughputPerHour(numberOfParallelJobs * 3600000 / avgTime);
		final double sd = calculateCorrectedStandartDeviation(times, avgTime);
		run.setSd(sd);
		run.setRsd(sd / avgTime);
		return run;
	}

	/**
	 * Berechnet die Standartabweichung einschließlich der Korrektur für normalverteilte Werte. (Das braucht man hier nicht unabhängiger, wenn doch sollte man avg aus den Werten berechnen lassen.)
	 *
	 * @param values
	 *            Die Werte
	 * @param avg
	 *            Der Durchschnitt über diese Werte
	 * @return die Standartabweichung
	 */
	private double calculateCorrectedStandartDeviation(final List<Long> values, final double avg) {
		long squareSum = 0;
		for (final long value : values) {
			squareSum += (value - avg) * (value - avg);
		}
		// TODO: Hier wird eine normale Verteilung der Berechnungszeiten angenommen. Der Fehler durch die Korrektur
		// sollte jedoch recht klein im Vergleich zu anderen Fehlerquellen sein.
		final double sd = Math.sqrt(squareSum / (values.size() - 1.5));
		return sd;
	}

	/**
	 * Testet, ob die Rate Zeit je Optimierung um die angegebene Menge geringer als die Maximalrate ist. Geht davon aus, dass es höchstens ein stark ausgeprägtes Minimum gibt.
	 *
	 * @param diff
	 *            Grenzdifferenz
	 * @param runs
	 *            Zeiten der einzelnen Jobs
	 * @return True wenn die Rate um mindestens diesen Wert abgefallen ist.
	 */
	private boolean throughputDroppedBy(final double diff, final Runs runs) {
		double maxAvgThroughput = Double.NEGATIVE_INFINITY;
		for (final Run run : runs.getRun()) {
			if (run.getAvgThroughputPerHour() > maxAvgThroughput) {
				maxAvgThroughput = run.getAvgThroughputPerHour();
			} else {
				if (run.getAvgThroughputPerHour() < (1 - diff) * maxAvgThroughput) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Gibt die höchste Rate Optimierungen/Zeit aus. Es wird davon ausgegangen, dass index+1 Optimierungen parallel stattfanden.
	 *
	 * @param runs
	 *            Die jeweils benötigten Zeiten.
	 * @return Schreibbare Schlussfolgerung
	 */
	private ThroughputResult drawConclusion(final Runs runs) {
		final XMLElementFactory factory = new XMLElementFactory();
		final ThroughputResult result = factory.createThroughputResult();

		result.setRuns(runs);

		result.setOverview(factory.createOverview());
		final Overview overview = result.getOverview();
		Run bestRun = null;
		for (final Run run : runs.getRun()) {
			if (bestRun == null || run.getAvgThroughputPerHour() > bestRun.getAvgThroughputPerHour()) {
				bestRun = run;
			}
		}
		overview.setScenario(TEST_SCENARIO.getAbsolutePath());
		overview.setBestThroughputRatio(factory.createBestThroughputRatio());
		final BestThroughputRatio best = overview.getBestThroughputRatio();
		best.setNumberOfParallelProcesses(bestRun.getNumberOfParallelProcesses());
		best.setAvgThroughputPerHour(bestRun.getAvgThroughputPerHour());
		best.setRsd(bestRun.getRsd());
		return result;
	}
}
