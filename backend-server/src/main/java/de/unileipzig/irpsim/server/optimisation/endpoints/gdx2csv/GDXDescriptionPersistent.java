package de.unileipzig.irpsim.server.optimisation.endpoints.gdx2csv;

import java.util.HashMap;
import java.util.Map;

public class GDXDescriptionPersistent {

	private int steps;

	private Map<String, ParametermetaData> data = new HashMap<>();

	public void setData(final Map<String, ParametermetaData> data) {
		this.data = data;
	}

	public int getSteps() {
		return steps;
	}

	public void setSteps(final int steps) {
		this.steps = steps;
	}

	public Map<String, ParametermetaData> getData() {
		return data;
	}

}
