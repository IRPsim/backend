package de.unileipzig.irpsim.core.simulation.data;

import de.unileipzig.irpsim.core.data.timeseries.Timeseries;

@FunctionalInterface
public interface SetConsumer{
	public void consumeSetData(String parametername, String element, Timeseries timeseries);
}
