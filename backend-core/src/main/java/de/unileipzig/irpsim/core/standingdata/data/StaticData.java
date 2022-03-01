package de.unileipzig.irpsim.core.standingdata.data;

import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class StaticData extends Datensatz {

	/**
	 * Gibt an, ob der Datensatz in der series_data_in-Tabelle ist (andernfalls ist er in series_data_out).
	 */
	private boolean inData = true;

	@JsonIgnore
	public boolean isInData() {
		return inData;
	}

	public void setInData(final boolean in) {
		this.inData = in;
	}

}