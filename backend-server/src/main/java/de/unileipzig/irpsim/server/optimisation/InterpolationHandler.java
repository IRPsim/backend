package de.unileipzig.irpsim.server.optimisation;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.unileipzig.irpsim.core.data.timeseries.Timeseries;
import de.unileipzig.irpsim.core.simulation.data.AggregatedResult;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.PostProcessing;
import de.unileipzig.irpsim.core.simulation.data.Set;
import de.unileipzig.irpsim.core.simulation.data.SetElement;

/**
 * Klasse dient dem Interpolieren von Teilen oder aller Daten zweier {@link BackendParametersYearData}.
 *
 * @author krauss
 */
public class InterpolationHandler {

	private static final Logger LOG = LogManager.getLogger(InterpolationHandler.class);

	private final int weightBefore, weightAfter;
	private final BackendParametersYearData yearData, yearBefore, yearAfter;
	private final ImportResultHandler importHandler;

	public InterpolationHandler(final BackendParametersYearData yearBefore,
			final int weightBefore, final BackendParametersYearData yearAfter, final int weightAfter) {
		this.yearBefore = yearBefore;
		this.weightBefore = weightBefore;
		this.yearAfter = yearAfter;
		this.weightAfter = weightAfter;
		this.yearData = new BackendParametersYearData();
		yearData.getConfig().setInterpolated(true);
		yearData.getConfig().setModeldefinition(yearBefore.getConfig().getModeldefinition());
		yearData.getConfig().setSavelength(yearBefore.getConfig().getSavelength());
		yearData.getConfig().setSimulationlength(yearBefore.getConfig().getSimulationlength());
		yearData.getConfig().setTimestep(yearBefore.getConfig().getTimestep());
		yearData.getConfig().setYear(interpolateYearDate());

		importHandler = new ImportResultHandler(yearData);
	}

	// TODO yearData als Übergabevariable entfernen - sollte eigentlich hier erzeugt werden
	public InterpolationHandler(final ImportResultHandler importHandler, final BackendParametersYearData yearData, final BackendParametersYearData yearBefore,
			final int weightBefore, final BackendParametersYearData yearAfter, final int weightAfter) {
		this.yearBefore = yearBefore;
		this.weightBefore = weightBefore;
		this.yearAfter = yearAfter;
		this.weightAfter = weightAfter;
		this.yearData = yearData;
		yearData.getConfig().setInterpolated(true);
		yearData.getConfig().setSavelength(yearBefore.getConfig().getSavelength());
		yearData.getConfig().setSimulationlength(yearBefore.getConfig().getSimulationlength());
		yearData.getConfig().setTimestep(yearBefore.getConfig().getTimestep());
		yearData.getConfig().setYear(interpolateYearDate());

		this.importHandler = new ImportResultHandler(yearData);
	}

	/**
	 * Interpoliert die Jahreszahl.
	 *
	 * @return Die interpolierte Jahreszahl.
	 */
	private int interpolateYearDate() {
		final int yearDateBefore = yearBefore.getConfig().getYear();
		final int yearDateAfter = yearAfter.getConfig().getYear();
		if (yearDateBefore != 0 && yearDateAfter != 0) {
			if ((yearDateAfter - yearDateBefore) % (weightBefore + weightAfter) == 0) {
				final int yearDate = (yearDateBefore * weightBefore + yearDateAfter * weightAfter) / (weightBefore + weightAfter);
				return yearDate;
			}
			LOG.error("Anzahl zu interpolierender Jahre stimmt nicht mit Abstand zwischen Jahren überein!");
		}
		throw new RuntimeException("Fehler in Interpolationhandler: Eingabe-Jahresdaten passen nicht zueinander.");
	}

	// TODO: Lädt Daten aus der Datenbank einzeln (öffnet jedesmal neuen ClosableEntityManager), evtl. Performance
	// Engpass!
	/**
	 * Interpoliere alle Daten, die in den {@link BackendParametersYearData} direkt enthalten sind oder zur Datenbank referenziert sind.
	 *
	 * @return Gibt das vollständig interpolierte Jahr zurück.
	 */
	public final BackendParametersYearData interpolateCompleteYearData() {
		interpolateTimeseries();
		interpolateTableTimeseries();
		interpolateSet();
		interpolatePostprocessing();
		if (importHandler != null) {
			importHandler.executeImports();
		}
		return yearData;
	}

	/**
	 * Interpoliere die Zeitreihendaten, die zur Datenbank referenziert sind.
	 */
	private final void interpolateTimeseries() {
		for (final Map.Entry<String, Timeseries> parameter : yearBefore.getTimeseries().entrySet()) {
			final Timeseries timeseriesAfter = yearAfter.getTimeseries().get(parameter.getKey());
			final Timeseries timeseriesBefore = parameter.getValue();
			interpolateNumList(timeseriesBefore, timeseriesAfter, parameter.getKey());
		}
	}

	/**
	 * Interpoliere die Setzeitreihendaten, die zur Datenbank referenziert sind.
	 */
	private final void interpolateSet() {
		yearBefore.getSets().values().forEach(setBefore -> {
			final Set setAfter = yearAfter.getSetWithName(setBefore.getName());
			setBefore.getElements().forEach(setElementBefore -> {
				final SetElement setElementAfter = setAfter.getElement(setElementBefore.getName());
				setElementBefore.getTimeseries().entrySet().forEach(parameter -> {
					final String parameterName = parameter.getKey();

					final Timeseries timeseriesBefore = parameter.getValue();
					final Timeseries timeseriesAfter = setElementAfter.getTimeseries().get(parameter.getKey());
					timeseriesBefore.loadTimeseries(false);

					timeseriesAfter.loadTimeseries(false);
					LOG.debug("Interpoliere {}", parameterName);
					final Iterator<Number> numberAfterIterator = timeseriesAfter.getData().iterator();
					int index = 0;
					for (final Number numBef : timeseriesBefore.getData()) {
						// int jahr = yearBefore + numBef;
						final Number numAft = numberAfterIterator.next();
						final Number interpolatedNumber = interpolateDouble(numBef, numAft);
						if (interpolatedNumber != null) {
							importHandler.manageResult(parameterName + "(ii" + index + "," + setElementBefore.getName() + ")", (double) interpolatedNumber);
						}
						index++;
					}

				});
			});
		});
	}

	/**
	 * Interpoliere die Tabellenzeitreihendaten, die zur Datenbank referenziert sind.
	 */
	private final void interpolateTableTimeseries() {
		yearBefore.getTableTimeseries().forEach((parameter, firstDependentsBefore) -> {
			final Map<String, Map<String, Timeseries>> firstDependentsAfter = yearAfter.getTableTimeseries().get(parameter);
			firstDependentsBefore.forEach((firstDependent, secondDependentsBefore) -> {
				final Map<String, Timeseries> secondDependentsAfter = firstDependentsAfter.get(firstDependent);
				secondDependentsBefore.forEach((secondDependent, timeseriesBefore) -> {
					final Timeseries timeseriesAfter = secondDependentsAfter.get(secondDependent);

					timeseriesBefore.loadTimeseries(false);

					timeseriesAfter.loadTimeseries(false);
					LOG.debug("Interpoliere {}", parameter);
					final Iterator<Number> numberAfterIterator = timeseriesAfter.getData().iterator();
					int index = 0;
					for (final Number numBef : timeseriesBefore.getData()) {
						// int jahr = yearBefore + numBef;
						final Number numAft = numberAfterIterator.next();
						final Number interpolatedNumber = interpolateDouble(numBef, numAft);
						if (interpolatedNumber != null) {
							importHandler.manageResult(parameter + "(ii" + index + "," + firstDependent + "," + secondDependent + ")", (double) interpolatedNumber);
						}
						index++;
					}
				});
			});
		});
	}

	/**
	 * Interpoliert die Daten des Postprocessing.
	 *
	 * @return Die gesamten Jahresdaten mit interpoliertem Postprocessing
	 */
	public final BackendParametersYearData interpolatePostprocessing() {
		final PostProcessing postBefore = yearBefore.getPostprocessing();
		final PostProcessing postAfter = yearAfter.getPostprocessing();
		final PostProcessing post = new PostProcessing();
		yearData.setPostprocessing(post);
		post.setScalars(interpolateAggregatedResultMap(postBefore.getScalars(), postAfter.getScalars()));
		post.setSets(interpolateAggregatedResultMapMapMap(postBefore.getSets(), postAfter.getSets()));
		post.setTables(interpolateAggregatedResultMapMapMap(postBefore.getTables(), postAfter.getTables()));
		return yearData;
	}

	/**
	 * @param aggResMapMapMapBef
	 *            Die verschachtelten {@link AggregatedResult} des vorhergehenden Jahres
	 * @param aggResMapMapMapAft
	 *            Die verschachtelten {@link AggregatedResult} des nachfolgenden Jahres
	 * @return Die verschachtelten {@link AggregatedResult} des interpolierten Jahres
	 */
	private Map<String, LinkedHashMap<String, Map<String, AggregatedResult>>> interpolateAggregatedResultMapMapMap(
			final Map<String, LinkedHashMap<String, Map<String, AggregatedResult>>> aggResMapMapMapBef,
			final Map<String, LinkedHashMap<String, Map<String, AggregatedResult>>> aggResMapMapMapAft) {

		final Map<String, LinkedHashMap<String, Map<String, AggregatedResult>>> yearAggResMapMapMap = new LinkedHashMap<>();
		for (final Map.Entry<String, LinkedHashMap<String, Map<String, AggregatedResult>>> aggResMapMapBef : aggResMapMapMapBef.entrySet()) {
			final LinkedHashMap<String, Map<String, AggregatedResult>> aggResMapMapAft = aggResMapMapMapAft.get(aggResMapMapBef.getKey());

			final LinkedHashMap<String, Map<String, AggregatedResult>> yearAggResMapMap = new LinkedHashMap<>();
			for (final Map.Entry<String, Map<String, AggregatedResult>> aggResMapBef : aggResMapMapBef.getValue().entrySet()) {
				final Map<String, AggregatedResult> aggResMapAft = aggResMapMapAft.get(aggResMapBef.getKey());

				yearAggResMapMap.put(aggResMapBef.getKey(), interpolateAggregatedResultMap(aggResMapBef.getValue(), aggResMapAft));
			}
			yearAggResMapMapMap.put(aggResMapMapBef.getKey(), yearAggResMapMap);
		}
		return yearAggResMapMapMap;
	}

	/**
	 * @param aggResMapBef
	 *            Die verschachtelten {@link AggregatedResult} des vorhergehenden Jahres
	 * @param aggResMapAft
	 *            Die verschachtelten {@link AggregatedResult} des nachfolgenden Jahres
	 * @return Die verschachtelten {@link AggregatedResult} des interpolierten Jahres
	 */
	private Map<String, AggregatedResult> interpolateAggregatedResultMap(final Map<String, AggregatedResult> aggResMapBef,
			final Map<String, AggregatedResult> aggResMapAft) {
		final Map<String, AggregatedResult> yearAggResMap = new LinkedHashMap<>();
		for (final Map.Entry<String, AggregatedResult> aggResBef : aggResMapBef.entrySet()) {
			final AggregatedResult aggResAft = aggResMapAft.get(aggResBef.getKey());
			yearAggResMap.put(aggResBef.getKey(), interpolateAggregatedResult(aggResBef.getValue(), aggResAft));
		}
		return yearAggResMap;
	}

	/**
	 * @param aggResBef
	 *            Die {@link AggregatedResult} des vorhergehenden Jahres
	 * @param aggResAft
	 *            Die {@link AggregatedResult} des nachfolgenden Jahres
	 * @return Die {@link AggregatedResult} des interpolierten Jahres
	 */
	private AggregatedResult interpolateAggregatedResult(final AggregatedResult aggResBef, final AggregatedResult aggResAft) {
		final AggregatedResult yearAggRes = new AggregatedResult();
		yearAggRes.setAvg(interpolateDouble(aggResBef.getAvg(), aggResAft.getAvg()));
		yearAggRes.setMax(interpolateDouble(aggResBef.getMax(), aggResAft.getMax()));
		yearAggRes.setMin(interpolateDouble(aggResBef.getMin(), aggResAft.getMin()));
		yearAggRes.setSum(interpolateDouble(aggResBef.getSum(), aggResAft.getSum()));
		return yearAggRes;
	}

	public final BackendParametersYearData getYearData() {
		return yearData;
	}

	/**
	 * @param timeseriesBefore
	 *            Die {@link Timeseries} des vorherigen Jahres, wenn diese als Referenz vorliegt, wird sie aus der Datenbank geladen
	 * @param timeseriesAfter
	 *            Die {@link Timeseries} des nachfolgenden Jahres
	 * @param parameterName
	 *            Der Parametername
	 */
	private void interpolateNumList(final Timeseries timeseriesBefore, final Timeseries timeseriesAfter, final String parameterName) {
		timeseriesBefore.loadTimeseries(false);
		timeseriesAfter.loadTimeseries(false);
		LOG.debug("Interpoliere {}", parameterName);
		final Iterator<Number> numberAfterIterator = timeseriesAfter.getData().iterator();
		int index = 0;
		for (final Number numBef : timeseriesBefore.getData()) {
			// int jahr = yearBefore + numBef;
			final Number numAft = numberAfterIterator.next();
			final Number interpolatedNumber = interpolateDouble(numBef, numAft);
			if (interpolatedNumber != null) {
				importHandler.manageResult(parameterName + "(ii" + index + ")", (double) interpolatedNumber);
			}
			index++;
		}
	}

	/**
	 * @param numBef
	 *            Zahl aus dem vorherigen Jahr
	 * @param numAft
	 *            Wert aus dem nachfolgenden Jahr
	 * @return Der interpolierte Wert
	 */
	private Double interpolateDouble(final Number numBef, final Number numAft) {
		if (numBef != null && numAft != null) {
			return (numBef.doubleValue() * weightBefore + numAft.doubleValue() * weightAfter) / (weightBefore + weightAfter);
		} else if (numBef == null || numAft == null) {
			return null;
		} else {
			LOG.error("Zahlen haben anderen Typ als Double");
			return null;
		}
	}
}
