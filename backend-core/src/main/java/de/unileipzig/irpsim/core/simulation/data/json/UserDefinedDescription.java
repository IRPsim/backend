package de.unileipzig.irpsim.core.simulation.data.json;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * TODO.
 * 
 * @author reichelt
 */
@Entity
@Table
public class UserDefinedDescription {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	@JsonIgnore
	private int id;

	private String supportiveYears;

	private String businessModelDescription;
	private String investmentCustomerSide;
	private String parameterAttention;
	private String creator; // In Analogie zu SimulationParametersMetadata

	public String getSupportiveYears() {
		return supportiveYears;
	}

	public void setSupportiveYears(final String supportiveYears) {
		this.supportiveYears = supportiveYears;
	}

	/**
	 * @return the parameterAttention
	 */
	public String getParameterAttention() {
		return parameterAttention;
	}

	public void setParameterAttention(final String parameterAttention) {
		this.parameterAttention = parameterAttention;
	}

	public int getId() {
		return id;
	}

	public void setId(final int id) {
		this.id = id;
	}

	public String getBusinessModelDescription() {
		return businessModelDescription;
	}

	public void setBusinessModelDescription(final String businessModelDescription) {
		this.businessModelDescription = businessModelDescription;
	}

	public String getInvestmentCustomerSide() {
		return investmentCustomerSide;
	}

	public void setInvestmentCustomerSide(final String investmentCustomerSide) {
		this.investmentCustomerSide = investmentCustomerSide;
	}

	public String getCreator() {
		return creator;
	}

	public void setCreator(final String creator) {
		this.creator = creator;
	}
}
