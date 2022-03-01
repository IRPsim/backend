package de.unileipzig.irpsim.core.simulation.data.json;

import java.util.Date;
import java.util.List;

import de.unileipzig.irpsim.core.data.JSONErrorMessage;
import de.unileipzig.irpsim.core.simulation.data.persistence.State;

public class YearState {

	private int year;
	private Date start;
	private int steps;

	private List<JSONErrorMessage> messages;
	private State state;

	public int getSteps() {
		return steps;
	}

	public void setSteps(final int steps) {
		this.steps = steps;
	}

	public List<JSONErrorMessage> getMessages() {
		return messages;
	}

	public void setMessages(final List<JSONErrorMessage> messages) {
		this.messages = messages;
	}

	public State getState() {
		return state;
	}

	public void setState(final State state) {
		this.state = state;
	}

	public int getYear() {
		return year;
	}

	public void setYear(final int year) {
		this.year = year;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(final Date start) {
		this.start = start;
	}
}
