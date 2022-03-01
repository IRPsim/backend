package de.unileipzig.irpsim.core.simulation.data.json;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import de.unileipzig.irpsim.core.data.JSONErrorMessage;
import de.unileipzig.irpsim.core.simulation.data.persistence.State;

/**
 * Repräsentiert den Status eines Simulationslaufs.
 *
 * @author reichelt
 */
public class IntermediarySimulationStatus implements Serializable, Comparable<IntermediarySimulationStatus> {

	private static final long serialVersionUID = -1723285242533329461L;
	private long id;
	private boolean running;
	private int simulationsteps;
	private int finishedsteps;
	private boolean error;
	private List<JSONErrorMessage> messages;
	private Date creation, start, end;
	private String modelVersionHash;
	private State state;
	private String stateDesc;
	private int yearIndex;
	private UserDefinedDescription description;
	private List<YearState> yearStates;

	/**
	 * Setter für den Zustand, setzt außerdem die zu dem Zustand gehörende Beschreibung.
	 *
	 * @param state Der Übergebene Zustand
	 */
	public final void setState(final State state) {
		this.state = state;
		stateDesc = state.getDesc();
	}

	public long getId() {
		return id;
	}

	public void setId(final long id) {
		this.id = id;
	}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(final boolean running) {
		this.running = running;
	}

	public int getSimulationsteps() {
		return simulationsteps;
	}

	public void setSimulationsteps(final int simulationsteps) {
		this.simulationsteps = simulationsteps;
	}

	public int getFinishedsteps() {
		return finishedsteps;
	}

	public void setFinishedsteps(final int finishedsteps) {
		this.finishedsteps = finishedsteps;
	}

	public boolean isError() {
		return error;
	}

	public void setError(final boolean error) {
		this.error = error;
	}

	public List<JSONErrorMessage> getMessages() {
		return messages;
	}

	public void setMessages(final List<JSONErrorMessage> messages) {
		this.messages = messages;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(final Date start) {
		this.start = start;
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(final Date end) {
		this.end = end;
	}

	public String getModelVersionHash() {
		return modelVersionHash;
	}

	public void setModelVersionHash(final String modelVersionHash) {
		this.modelVersionHash = modelVersionHash;
	}

	public String getStateDesc() {
		return stateDesc;
	}

	public void setStateDesc(final String stateDesc) {
		this.stateDesc = stateDesc;
	}

	public int getYearIndex() {
		return yearIndex;
	}

	public void setYearIndex(final int yearIndex) {
		this.yearIndex = yearIndex;
	}

	public UserDefinedDescription getDescription() {
		return description;
	}

	public void setDescription(final UserDefinedDescription description) {
		this.description = description;
	}

	public State getState() {
		return state;
	}

	@Override
	public boolean equals(final Object other) {
		if (other instanceof IntermediarySimulationStatus) {
			final IntermediarySimulationStatus otherState = (IntermediarySimulationStatus) other;
			return otherState.id == id;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (int) id;
	}

	@Override
	public int compareTo(final IntermediarySimulationStatus iss2) {
		if (getEnd() == null || iss2.getEnd() == null) {
			if (getStart() != null && iss2.getStart() != null) {
				return getStart().compareTo(iss2.getStart());
			} else {
				return ((Long) getId()).compareTo(iss2.getId());
			}
		}
		return getEnd().compareTo(iss2.getEnd());
	}

	public Date getCreation() {
		return creation;
	}

	public void setCreation(final Date creation) {
		this.creation = creation;
	}

	public List<YearState> getYearStates() {
		return yearStates;
	}

	public void setYearStates(final List<YearState> yearStates) {
		this.yearStates = yearStates;
	}
}
