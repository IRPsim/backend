package de.unileipzig.irpsim.server.standingdata.transfer;

public class ExistingStammdatum {
	int importId;
	int existingId;
	boolean notImportedBecauseOfDependency = false;

	public boolean isNotImportedBecauseOfDependency() {
		return notImportedBecauseOfDependency;
	}

	public void setNotImportedBecauseOfDependency(final boolean notImportedBecauseOfDependency) {
		this.notImportedBecauseOfDependency = notImportedBecauseOfDependency;
	}

	public int getImportId() {
		return importId;
	}

	public void setImportId(final int importId) {
		this.importId = importId;
	}

	public int getExistingId() {
		return existingId;
	}

	public void setExistingId(final int existingId) {
		this.existingId = existingId;
	}
}