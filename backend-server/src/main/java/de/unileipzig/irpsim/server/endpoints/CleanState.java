package de.unileipzig.irpsim.server.endpoints;

class CleanState {
	int toAnalyze = -1;
	int analyzed = -1;
	int toDelete = -1;
	int deleted = -1;
	int optimize = -1;
	long start = System.currentTimeMillis();

	public int getToAnalyze() {
		return toAnalyze;
	}

	public void setToAnalyze(int toAnalyze) {
		this.toAnalyze = toAnalyze;
	}

	public int getAnalyzed() {
		return analyzed;
	}

	public void setAnalyzed(int analyzed) {
		this.analyzed = analyzed;
	}

	public int getToDelete() {
		return toDelete;
	}

	public void setToDelete(int toDelete) {
		this.toDelete = toDelete;
	}

	public int getDeleted() {
		return deleted;
	}

	public void setDeleted(int deleted) {
		this.deleted = deleted;
	}

	public int getOptimize() {
		return optimize;
	}

	public void setOptimize(int optimize) {
		this.optimize = optimize;
	}

	public long getStart() {
		return start;
	}

	public void setStart(long start) {
		this.start = start;
	}
}