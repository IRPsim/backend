package de.unileipzig.irpsim.core.standingdata.data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Entity
@Table
public class Variable {

	@Id
	@JsonIgnore
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	@ManyToOne
	@JsonSerialize(using = Datensatz.StammdatumSerializer.class)
	@JsonDeserialize(using = Datensatz.StammdatumDeserializer.class)
	private Stammdatum stammdatum;

	private int jahr;

	public int getId() {
		return id;
	}

	public void setId(final int id) {
		this.id = id;
	}

	public Stammdatum getStammdatum() {
		return stammdatum;
	}

	public void setStammdatum(final Stammdatum stammdatum) {
		this.stammdatum = stammdatum;
	}

	public int getJahr() {
		return jahr;
	}

	public void setJahr(final int jahr) {
		this.jahr = jahr;
	}

	@Override
	public String toString() {
		return "Variable [id=" + id + ", stammdatum=" + stammdatum + ", jahr=" + jahr + "]";
	}
}