package de.unileipzig.irpsim.core.data.simulationparameters;

import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Daten eines Simulations-Parametersatzes.
 *
 * @author reichelt
 */
@Entity
@Table
public class OptimisationScenario {
	private int id;
	private String name, creator, description;
	private int modeldefinition;
	private boolean deletable = true;
	private Date date;
	private String version;

	@JsonIgnore
	private String data;

	/**
	 * Liefert die ID eines Simulations-Parametersatzes.
	 *
	 * @return Die ID des Simulations-Parametersatzes als integer
	 */
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public int getId() {
		return id;
	}

	/**
	 * Setzt die ID eines Simulations-Parametersatzes.
	 *
	 * @param id
	 *            Die zu setzende ID
	 */
	public void setId(final int id) {
		this.id = id;
	}
	
	public String getVersion() {
		return version;
	}
	
	public void setVersion(final String version) {
		this.version = version;
	}

	public int getModeldefinition() {
      return modeldefinition;
   }

   public void setModeldefinition(int modeldefinition) {
      this.modeldefinition = modeldefinition;
   }

   /**
	 * Liefert den Namen eines Simulations-Parametersatzes.
	 *
	 * @return Der Name des Simulations-Parametersatzes als String
	 */
	public String getName() {
		return name;
	}

	/**
	 * Setzt den Namen eines Simulations-Parametersatzes.
	 *
	 * @param name
	 *            Der zu setzende Name
	 */
	public void setName(final String name) {
		this.name = name;
	}

	/**
	 * Liefert die Daten des Simulations-Parametersatzes.
	 *
	 * @return Die Daten des Simulations-Parametersatzes als String
	 */
	@Column(columnDefinition = "LONGBLOB")
	@Basic(fetch = FetchType.LAZY)
	public String getData() {
		return data;
	}

	/**
	 * Setzt die Daten des Simulations-Parametersatzes.
	 *
	 * @param data
	 *            Die Daten des Simulations-Parametersatzes
	 */
	public void setData(final String data) {
		this.data = data;
	}

	/**
	 * Liefert die Beschreibung des Simulations-Parametersatzes.
	 *
	 * @return Die Beschreibung des Simulations-Parametersatzes als String
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Setzt die Beschreibung eines Simulations-Parametersatzes.
	 *
	 * @param description
	 *            Die zu setztende Beschreibung
	 */
	public void setDescription(final String description) {
		this.description = description;
	}

	/**
	 * Liefert den Namen des creators.
	 *
	 * @return Der Name des creators
	 */
	public String getCreator() {
		return creator;
	}

	/**
	 * Setzt den Namen des creators.
	 *
	 * @param creator
	 *            Der zu setzende Name des creators
	 */
	public void setCreator(final String creator) {
		this.creator = creator;
	}

	/**
	 * Liefert das Erstellungsdatum des Simulations-Parametersatzes.
	 *
	 * @return Das Erstellungsdatum als Date
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * Setzt das Erstellungsdatum des Simulations-Parametersatzes.
	 *
	 * @param date
	 *            Das zu setzdende Erstellungsdatum
	 */
	public void setDate(final Date date) {
		this.date = date;
	}

	/**
	 * Gibt an ob der Simulations-Parametersatz löschbar ist oder nicht.
	 *
	 * @return true falls der Simulations-Parametersatz löschbar ist, false sonst
	 */
	public boolean isDeletable() {
		return deletable;
	}

	/**
	 * Setzt Wahrheitswert als Information über die löschbarkeit des Simulations-Parametersatzes.
	 *
	 * @param deletable
	 *            Der zu setzende Wahrheitswert
	 */
	public void setDeletable(final boolean deletable) {
		this.deletable = deletable;
	}
}
