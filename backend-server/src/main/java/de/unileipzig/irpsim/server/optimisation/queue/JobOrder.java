package de.unileipzig.irpsim.server.optimisation.queue;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
class JobOrder {
	@Id
	private int position;
	private long id;

	public JobOrder() {

	}

	public JobOrder(final int position, final long id) {
		super();
		this.position = position;
		this.id = id;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(final int position) {
		this.position = position;
	}

	public long getId() {
		return id;
	}

	public void setId(final long id) {
		this.id = id;
	}

}