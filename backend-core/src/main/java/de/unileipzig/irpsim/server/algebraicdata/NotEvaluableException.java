package de.unileipzig.irpsim.server.algebraicdata;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotEvaluableException extends RuntimeException {

	private final Map<Integer, String> missingParameter = new HashMap<>();

	public NotEvaluableException(final List<Integer> ids) {
		for (final Integer id : ids) {
			this.missingParameter.put(id, null);
		}
	}

	public Collection<Integer> getMissingIds() {
		return missingParameter.keySet();
	}

	@Override
	public String getMessage() {
		String missingString = "";
		for (final Map.Entry<Integer, String> missingParameter : missingParameter.entrySet()) {
			missingString += missingParameter.getValue() + " (" + missingParameter.getKey() + ") ";
		}
		return "Formel für " + missingString + "konnte nicht ausgewertet werden.";
	}

	public void setParameterForId(final Integer id, final String parameter) {
		missingParameter.put(id, parameter);
	}
}
