package de.unileipzig.irpsim.core.standingdata.data;

import java.io.IOException;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@Entity
@Table(uniqueConstraints = {
		@UniqueConstraint(columnNames = { "szenario", "jahr", "stammdatum_id" }) })
public abstract class Datensatz {

	public static class StammdatumSerializer extends JsonSerializer<Stammdatum> {

		@Override
		public void serialize(final Stammdatum arg0, final JsonGenerator arg1, final SerializerProvider arg2) throws IOException, JsonProcessingException {
			arg1.writeNumber(arg0.getId());
		}
	}

	public static class StammdatumDeserializer extends JsonDeserializer<Stammdatum> {

		@Override
		public Stammdatum deserialize(final JsonParser arg0, final DeserializationContext arg1) throws IOException, JsonProcessingException {
			final int id = arg0.getIntValue();
			final Stammdatum sd = new Stammdatum();
			sd.setId(id);
			return sd;
		}
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	protected int id;

	private int szenario;
	private int jahr = 0;

	private boolean aktiv = true;

	@ManyToOne
	// @JsonProperty(access = Access.WRITE_ONLY)
	protected Stammdatum stammdatum;

	@JsonIgnore
	public int getId() {
		return id;
	}

	public void setId(final int id) {
		this.id = id;
	}

	public int getSzenario() {
		return szenario;
	}

	public void setSzenario(final int szenario) {
		this.szenario = szenario;
	}

	public int getJahr() {
		return jahr;
	}

	public void setJahr(final int jahr) {
		this.jahr = jahr;
	}

	@JsonSerialize(using = StammdatumSerializer.class)
	@JsonDeserialize(using = StammdatumDeserializer.class)
	public Stammdatum getStammdatum() {
		return stammdatum;
	}

	public void setStammdatum(final Stammdatum stammdatum) {
		this.stammdatum = stammdatum;
	}

	public String getSeriesid() {
		return "" + id;
	}

	public void setSeriesid(final String seriesid) {
		setId(Integer.parseInt(seriesid));
	}

	@JsonIgnore
	public boolean isAktiv() {
		return aktiv;
	}

	public void setAktiv(final boolean aktiv) {
		this.aktiv = aktiv;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		result = prime * result + jahr;
		result = prime * result + szenario;
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Datensatz other = (Datensatz) obj;
		if (id != other.id)
			return false;
		if (jahr != other.jahr)
			return false;
		if (szenario != other.szenario)
			return false;
		return true;
	}
}