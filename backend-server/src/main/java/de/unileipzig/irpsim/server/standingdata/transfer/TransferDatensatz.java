package de.unileipzig.irpsim.server.standingdata.transfer;

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import de.unileipzig.irpsim.core.standingdata.TimeseriesValue;

public class TransferDatensatz {
	private List<TimeseriesValue> data = new LinkedList<>();
	private int jahr;
	private int stammdatumId;
	private String szenarioName;
	private int szenarioStelle;

	@JsonIgnore
	private int id;

	public int getId() {
		return id;
	}

	public void setId(final int id) {
		this.id = id;
	}

	public int getStammdatumId() {
		return stammdatumId;
	}

	public void setStammdatumId(final int stammdatumId) {
		this.stammdatumId = stammdatumId;
	}

	public int getJahr() {
		return jahr;
	}

	public void setJahr(final int jahr) {
		this.jahr = jahr;
	}

	public List<TimeseriesValue> getData() {
		return data;
	}

	public void setData(final List<TimeseriesValue> data) {
		this.data = data;
	}

	public String getSzenarioName() {
		return szenarioName;
	}

	public void setSzenarioName(final String szenarioName) {
		this.szenarioName = szenarioName;
	}

	public int getSzenarioStelle() {
		return szenarioStelle;
	}

	public void setSzenarioStelle(final int szenarioStelle) {
		this.szenarioStelle = szenarioStelle;
	}

}