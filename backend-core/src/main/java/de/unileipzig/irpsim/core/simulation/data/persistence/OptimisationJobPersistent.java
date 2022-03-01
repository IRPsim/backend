package de.unileipzig.irpsim.core.simulation.data.persistence;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.annotations.Type;

import de.unileipzig.irpsim.core.data.JSONErrorMessage;
import de.unileipzig.irpsim.core.simulation.data.json.IntermediarySimulationStatus;
import de.unileipzig.irpsim.core.simulation.data.json.UserDefinedDescription;
import de.unileipzig.irpsim.core.simulation.data.json.YearState;

/**
 * Beinhaltet die zu persistierenden Daten einer Simulationsjob-Ausführung. Dabei wird ein Simulationsjob bereits beim Starten persistiert, im Laufe eines
 * Simulationjob-Lebenszyklus werden die Daten der Phasen (Parametrisierung: Eingabe-GDX, Ende: Ausgabe-GDX, -CSV und -JSON) hinzugefügt.
 *
 * @author reichelt
 */
@Entity
@Table
public class OptimisationJobPersistent {

   private static Logger LOG = LogManager.getLogger(OptimisationJobPersistent.class);

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private long id;

   @Column
   @OneToMany(mappedBy = "job")
   private List<OptimisationYearPersistent> years = new LinkedList<>();

   @Column
   private Date creation;

   @Column
   private Date start;

   @Column
   private Date end;

   @Column(name = "modelVersionHash")
   private String modelVersionHash;

   @Type(type = "text")
   private String jsonParameter;

   @Type(type = "text")
   private String jsonResult;

   private int modeldefinition = 1;

   @Column
   private String comment;

   @Column
   private boolean error = false;

   @Type(type = "text")
   private String errorMessage;

   @Column
   private int finishedsteps = 0, simulationsteps = 0;

   @Column
   private State state = State.WAITING;

   @OneToOne(orphanRemoval = true)
   private UserDefinedDescription description;

   public OptimisationJobPersistent() {

   }

   public UserDefinedDescription getDescription() {
      return description;
   }

   public void setDescription(UserDefinedDescription description) {
      this.description = description;
   }

   public int getModeldefinition() {
      return modeldefinition;
   }

   public void setModeldefinition(int modeldefinition) {
      this.modeldefinition = modeldefinition;
   }

   public Long getId() {
      return id;
   }

   public void setId(long id) {this.id = id;}

   public int getSimulationsteps() {
      return simulationsteps;
   }

   public void setSimulationsteps(int simulationsteps) {
      this.simulationsteps = simulationsteps;
   }

   public boolean isError() {
      return error;
   }

   public Date getCreation() {
      return creation;
   }

   public void setCreation(Date creation) {
      this.creation = creation;
   }

   /**
    * Ist Der Status weder ERROR noch FINISHEDERROR wird der er auf ERROR gesetzt.
    *
    * @param error ob ein Error auftrat
    */
   public void setError(boolean error) {
      this.error = error;
      if (error && state != State.ERROR && state != State.FINISHEDERROR) {
         setState(State.ERROR);
      }
   }

   public Date getStart() {
      return start;
   }

   public void setStart(Date start) {
      this.start = start;
   }

   public String getModelVersionHash() {
      return modelVersionHash;
   }

   public void setModelVersionHash(String modelVersionHash) {
      this.modelVersionHash = modelVersionHash;
   }

   public String getComment() {
      return comment;
   }

   public void setComment(String comment) {
      this.comment = comment;
   }

   public Date getEnd() {
      return end;
   }

   public void setEnd(Date end) {
      this.end = end;
   }

   public String getJsonParameter() {
      return jsonParameter;
   }

   public void setJsonParameter(String jsonParameter) {
      this.jsonParameter = jsonParameter;
   }

   public String getJsonResult() {
      return jsonResult;
   }

   public void setJsonResult(String jsonResult) {
      this.jsonResult = jsonResult;
   }

   public State getState() {
      return state;
   }

   /**
    * Ist der übergebene Status ERROR oder FINISHEDERROR wird der error boolean auf true gesetzt.
    *
    * @param state der neue Zustand
    */
   public void setState(State state) {
      LOG.debug("Setting state: {}", state);
      this.state = state;
      if (!error && (state == State.ERROR || state == State.FINISHEDERROR)) {
         setError(true);
      }
   }

   /**
    * @return Gibt den momentanen Status der Simulation zurück in der Form eines {@link IntermediarySimulationStatus}. Dabei wird dieser mit allen bekannten Werten neu
    *         initialisiert.
    */
   public IntermediarySimulationStatus getOptimisationState() {
      IntermediarySimulationStatus status = new IntermediarySimulationStatus();
      status.setId(id);
      status.setEnd(end);
      status.setCreation(creation);
      status.setStart(start);
      status.setRunning(false);
      status.setError(error);
      status.setFinishedsteps(finishedsteps);
      status.setModelVersionHash(modelVersionHash);
      status.setState(getState());
      status.setDescription(description);
      status.setSimulationsteps(simulationsteps);
      if (error) {
         status.setMessages(Arrays.asList(new JSONErrorMessage[] { new JSONErrorMessage(errorMessage) }));
      }
      List<YearState> yearStates = new LinkedList<>();
      for (OptimisationYearPersistent year : years) {
         yearStates.add(year.getState());
      }
      status.setYearStates(yearStates);

      return status;
   }

   /**
    * @param simulatedYearIndex Index des Stützjahres ohne zu interpolierende Jahre.
    * @return Liefert die persistierten Daten des Stützjahres
    */
   public OptimisationYearPersistent getYearDataOfSimulatedYear(int simulatedYearIndex, int modelindex) {
      OptimisationYearPersistent yearData = null;
      LOG.info("YearData: {} {} ", yearData, simulatedYearIndex);
      for (OptimisationYearPersistent yearDataTry : years) {
         // if (yearDataTry.getGdxparameter() != null) {
         LOG.info("Jahr vorhanden: {} Aktuell: {} Id: {}", yearDataTry.getYear(), simulatedYearIndex, yearDataTry.getId());
         if (yearDataTry.getSimulatedYearIndex() == simulatedYearIndex && yearDataTry.getModelindex() == modelindex) {
            yearData = yearDataTry;
            break;
         }
      }
      if (yearData == null) {
         throw new RuntimeException("Achtung: Jahr " + simulatedYearIndex + " in Job " + id + " nicht definiert.");
      }
      return yearData;
   }

   /**
    * @param simulatedYear das Stützjahres ohne zu interpolierende Jahre.
    * @return Liefert die persistierten Daten des Stützjahres
    */
   public OptimisationYearPersistent getYearDataOfSimulatedYearWithoutIndex(int simulatedYear, int modelindex) {
      OptimisationYearPersistent yearData = null;
      LOG.info("YearData: {} {} ", yearData, simulatedYear);
      for (OptimisationYearPersistent yearDataTry : years) {
         LOG.info("Jahr vorhanden: {} Aktuell: {} Id: {}", yearDataTry.getYear(), simulatedYear, yearDataTry.getId());
         if (yearDataTry.getYearNumber() == simulatedYear && yearDataTry.getModelindex() == modelindex) {
            yearData = yearDataTry;
            break;
         }
      }
      if (yearData == null) {
         throw new RuntimeException("Achtung: Jahr " + simulatedYear + " in Job " + id + " nicht definiert.");
      }
      return yearData;
   }

   public int getFinishedsteps() {
      return finishedsteps;
   }

   public void setFinishedsteps(int finishedsteps) {
      this.finishedsteps = finishedsteps;
   }

   public List<OptimisationYearPersistent> getYears() {
      return years;
   }

   public void setYears(List<OptimisationYearPersistent> years) {
      this.years = years;
   }

   public String getErrorMessage() {
      return errorMessage;
   }

   public void setErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
   }

   @Override
   public String toString() {
      final StringBuilder stringer = new StringBuilder("JobId: ");
      stringer.append(id).append(", Erstellung: ").append(creation).append(", Ende: ").append(end);
      stringer.append(", Jahre: ").append(years.size());
      return stringer.toString();
   }
}
