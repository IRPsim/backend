package de.unileipzig.irpsim.core.simulation.data.persistence;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import de.unileipzig.irpsim.core.simulation.data.json.YearState;

/**
 * Beinhaltet die zu Persistierenden Daten einer SimulationJob-Ausführung eines bestimmten Jahres.
 * 
 * @author reichelt
 */
@Table
@Entity
public class OptimisationYearPersistent {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private int id;

	@ManyToOne
	private OptimisationJobPersistent job;

	@Column
	private Date start;

	@Column
	private Date end;

	@Column
	private String lstFile;

	@Column
	private String csvTotalData;

	@Column
	private String gdxresult;

	@Column
	private String gdxparameter;
	
	@Column
	private String sqlite;

	@Column
	private int year;
	
	@Column
	private int yearNumber;

	@Column
	private int simulatedYearIndex;
	
	@Column
	private int modelindex = 0;

	public OptimisationYearPersistent() {
	   
	}
	
	public OptimisationYearPersistent(OptimisationJobPersistent job, int simulatedYearIndex, int yearIndex, int modelindex, int yearNumber) {
	   this.job = job;
	   this.start = new Date();
	   this.simulatedYearIndex = simulatedYearIndex;
	   this.year = yearIndex;
	   this.modelindex = modelindex;
	   this.yearNumber = yearNumber;
	}
	
	public  int getId() {
		return id;
	}

	public  OptimisationJobPersistent getJob() {
		return job;
	}

	public  void setJob( OptimisationJobPersistent job) {
		this.job = job;
	}

	public  Date getStart() {
		return start;
	}

	public  void setStart( Date start) {
		this.start = start;
	}

	public Date getEnd() {
		return end;
	}
	
	public String getSqlite() {
		return sqlite;
	}

	public void setSqlite(String sqlite) {
		this.sqlite = sqlite;
	}

	public void setEnd( Date end) {
		this.end = end;
	}

	public  String getCsvTotalData() {
		return csvTotalData;
	}

	public  void setCsvTotalData( String csvTotalData) {
		this.csvTotalData = csvTotalData;
	}

	public  String getGdxresult() {
		return gdxresult;
	}

	public  void setGdxresult( String gdxresult) {
		this.gdxresult = gdxresult;
	}

	public  String getGdxparameter() {
		return gdxparameter;
	}

	public  void setGdxparameter( String gdxparameter) {
		this.gdxparameter = gdxparameter;
	}

	public  String getLstFile() {
		return lstFile;
	}

	public  void setLstFile( String lstFile) {
		this.lstFile = lstFile;
	}

	public  int getYear() {
		return year;
	}

	public  void setYear( int year) {
		this.year = year;
	}
	
	public int getYearNumber() {
		return yearNumber;
	}

	public void setYearNumber(int yearNumber) {
		this.yearNumber = yearNumber;
	}

	public int getSimulatedYearIndex() {
		return simulatedYearIndex;
	}

	public void setSimulatedYearIndex( int simulatedYearIndex) {
		this.simulatedYearIndex = simulatedYearIndex;
	}
	
	public int getModelindex() {
      return modelindex;
   }

   public void setModelindex(int modelindex) {
      this.modelindex = modelindex;
   }

   public YearState getState() {
		final YearState state = new YearState();
		state.setStart(start);
		state.setYear(yearNumber);
		return state;
	}
}
