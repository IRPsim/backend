package de.unileipzig.irpsim.server.optimisation.postprocessing;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import de.unileipzig.irpsim.core.simulation.data.AggregatedResult;
import de.unileipzig.irpsim.core.simulation.data.Calculation;
import de.unileipzig.irpsim.core.simulation.data.PostProcessing;
import de.unileipzig.irpsim.core.utils.ParameterOutputDependenciesUtil;

/**
 * Verarbeitet das Berechnen von zusammengefassten Ergebnissen aus mehreren Jahres-Ergebnissätzen.
 *
 * @author reichelt
 */
public class MultipleYearPostprocessorHandler extends AbstractPostProcessorHandler {

	private final double interest;

	/**
	 * Initialisiert den MultipleYearPostprocessorHandler. Der Zinssatz wird hierbei als Zahl größer als 1 angegeben, d.h. ein Zinssatz von 0,03% sollte hier
	 * als 1,03 übergeben werden.
	 *
	 * @param model Name des bearbeiteten Modells
	 * @param interest Jährlicher Zinssatz der über 1 liegen sollte (d.h. ein Zins von 0.03% wäre 1,03)
	 * @see AbstractPostProcessorHandler#AbstractPostProcessorHandler
	 */
	public MultipleYearPostprocessorHandler(final double interest, int modeldefinition) {
		super(modeldefinition);
		this.interest = interest;
		final Set<String> overviewData = ParameterOutputDependenciesUtil.getInstance().getNoOverviewData(1);
		for (final String parameterName : overviewData) {
			getProcessors().remove(parameterName);
		}
	}

	/**
	 * Fügt ein Mehrjahres-Ergebnis zu den aktuellen Berechnungen hinzu.
	 *
	 * @param result Mehrjahres-Ergebnis
	 * @param index Jahresindex
	 */
	public final void addResult(final PostProcessing result, final int index) {
		for (final Entry<String, AggregatedResult> value : result.getScalars().entrySet()) {
			addResultValue(index, new String[0], value.getKey(), value.getValue());
		}

		for (final Entry<String, LinkedHashMap<String, Map<String, AggregatedResult>>> setEntryValueMap : result.getSets().entrySet()) {
			final String[] setName = new String[1];
			for (final Entry<String, Map<String, AggregatedResult>> nameEntryValueMap : setEntryValueMap.getValue().entrySet()) {
				setName[0] = nameEntryValueMap.getKey();
				for (final Entry<String, AggregatedResult> value : nameEntryValueMap.getValue().entrySet()) {

					addResultValue(index, setName, value.getKey(), value.getValue());
				}
			}

		}

		for (final Entry<String, LinkedHashMap<String, Map<String, AggregatedResult>>> tableEntryValueMap : result.getTables().entrySet()) {
			final String[] dependents = new String[2];
			for (final Entry<String, Map<String, AggregatedResult>> nameEntryValueMap : tableEntryValueMap.getValue().entrySet()) {
				dependents[0] = nameEntryValueMap.getKey();
				for (final Entry<String, AggregatedResult> value : nameEntryValueMap.getValue().entrySet()) {
					dependents[1] = value.getKey();

					addResultValue(index, dependents, tableEntryValueMap.getKey(), value.getValue());
				}
			}
		}
	}

	/**
	 * Fügt den Ergebniswert zum dazugehörigen Mehrjahres-Prozessor hinzu.
	 * 
	 * @param index Jahresindex
	 * @param dependents Die Abhängigen Sets bzw. Elemente
	 * @param parameter Parametername
	 * @param result Die zu ergänzenden Ergebnisse
	 */
	private void addResultValue(final int index, final String[] dependents, final String parameter, final AggregatedResult result) {
		if (getProcessors().containsKey(parameter)) {
			getProcessors().get(parameter).stream().filter(p -> p.getCalculation() != Calculation.OUTLINE).forEach(processor -> {
				final Calculation calculation = processor.getCalculation();
				double resultValue = result.fetchValue(calculation);
				if (calculation.equals(Calculation.SUM) && parameter.split("_")[2].matches("(I)|(O)|(IuO)")) {
					resultValue /= Math.pow(interest, index);
				}
				processor.addValue(dependents, resultValue);
			});
		}
	}
}
