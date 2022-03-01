package de.unileipzig.irpsim.core.simulation.data;

import de.unileipzig.irpsim.core.data.timeseries.Timeseries;

@FunctionalInterface
public interface TableConsumer {
	public void consumeTableData(String parametername, String firstDependent, String secondDependent, Timeseries timeseries) throws TimeseriesTooShortException;
}
