package de.unileipzig.irpsim.core.simulation.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Beinhaltet die Globale Konfigurationsdaten für einen GAMS-Lauf.
 *
 * @author reichelt
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Globalconfig {
	private int simulationlength, savelength = 96, optimizationlength = 192, resolution = 35040;
	private double timestep;
	private int modeldefinition;
	private int year;
	private Integer prognoseszenario;
	private boolean interpolated = false;
	
	public int getResolution() {
	  // length 0 is from old scenarios and disallowed - default changed to 35040
      return resolution != 0 ? resolution : 35040;
   }
	
	public void setResolution(int resolution) {
      this.resolution = resolution;
   }

	public final int getSimulationlength() {
		return simulationlength;
	}

	public Integer getPrognoseszenario() {
		return prognoseszenario;
	}

	public void setPrognoseszenario(final Integer prognoseszenario) {
		this.prognoseszenario = prognoseszenario;
	}

	public final void setSimulationlength(final int simulationlength) {
		this.simulationlength = simulationlength;
	}

	public final double getTimestep() {
		return timestep;
	}

	public final void setTimestep(final double d) {
		this.timestep = d;
	}

	public int getOptimizationlength() {
      return optimizationlength;
   }

   public void setOptimizationlength(int optimizationlength) {
      this.optimizationlength = optimizationlength;
   }

   public final int getSavelength() {
		return savelength;
	}

	public final void setSavelength(final int savelength) {
		this.savelength = savelength;
	}

	public int getModeldefinition() {
      return modeldefinition != 0 ? modeldefinition : 1;
   }

   public void setModeldefinition(int modeldefinition) {
      this.modeldefinition = modeldefinition;
   }

   public final int getYear() {
		return year;
	}

	public final void setYear(final int year) {
		this.year = year;
	}

	/**
	 * Kopiert die primitiven/ immutable Felder in ein neues Object.
	 *
	 * @return Kopie des Objects
	 */
	public final Globalconfig copy() {
		final Globalconfig config = new Globalconfig();
		config.savelength = this.savelength;
		config.simulationlength = this.simulationlength;
		config.optimizationlength = this.optimizationlength;
		config.resolution = this.resolution;
		config.timestep = this.timestep;
		config.year = this.year;
		config.prognoseszenario = this.prognoseszenario;
		return config;
	}

	public boolean isInterpolated() {
		return interpolated;
	}

	public void setInterpolated(final boolean interpolated) {
		this.interpolated = interpolated;
	}

}
