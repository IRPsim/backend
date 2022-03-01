package de.unileipzig.irpsim.server.standingdata.transfer;

import java.util.LinkedList;
import java.util.List;

import de.unileipzig.irpsim.core.standingdata.data.AlgebraicData;
import de.unileipzig.irpsim.core.standingdata.data.Stammdatum;

public class TransferData {
	private List<Stammdatum> stammdaten = new LinkedList<>();
	private List<TransferDatensatz> daten = new LinkedList<>();
	private List<AlgebraicData> algebraicData = new LinkedList<>();

	public List<AlgebraicData> getAlgebraicData() {
		return algebraicData;
	}

	public void setAlgebraicData(final List<AlgebraicData> algebraicData) {
		this.algebraicData = algebraicData;
	}

	public List<Stammdatum> getStammdaten() {
		return stammdaten;
	}

	public void setStammdaten(final List<Stammdatum> stammdaten) {
		this.stammdaten = stammdaten;
	}

	public List<TransferDatensatz> getDaten() {
		return daten;
	}

	public void setDaten(final List<TransferDatensatz> daten) {
		this.daten = daten;
	}

}