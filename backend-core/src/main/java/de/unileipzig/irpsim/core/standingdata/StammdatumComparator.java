package de.unileipzig.irpsim.core.standingdata;

import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;

public interface StammdatumComparator {
	boolean compareValues(Stammdatum s1, Stammdatum s2);
}
