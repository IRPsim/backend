package de.unileipzig.irpsim.server.standingdata.endpoints.data;

public class ExportError {
	private int id;
	private int referencedId;

	public int getId() {
		return id;
	}

	public void setId(final int id) {
		this.id = id;
	}

	public int getReferencedId() {
		return referencedId;
	}

	public void setReferencedId(final int referencedId) {
		this.referencedId = referencedId;
	}
}