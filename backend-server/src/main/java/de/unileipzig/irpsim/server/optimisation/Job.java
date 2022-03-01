package de.unileipzig.irpsim.server.optimisation;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityTransaction;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.data.JSONErrorMessage;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersMultiModel;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.ConnectedModelParameter;
import de.unileipzig.irpsim.core.simulation.data.GenericParameters;
import de.unileipzig.irpsim.core.simulation.data.TimeseriesTooShortException;
import de.unileipzig.irpsim.core.simulation.data.json.IntermediarySimulationStatus;
import de.unileipzig.irpsim.core.simulation.data.json.YearState;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.core.simulation.data.persistence.State;
import de.unileipzig.irpsim.core.utils.PersistenceFolderUtil;
import de.unileipzig.irpsim.models.ModelInformation;
import de.unileipzig.irpsim.server.data.modeldefinitions.ModelDefinitionsEndpoint;
import de.unileipzig.irpsim.server.modelstart.GAMSModelStarter;
import de.unileipzig.irpsim.server.modelstart.JavaModelStarter;
import de.unileipzig.irpsim.server.modelstart.ModelStarter;
import de.unileipzig.irpsim.server.optimisation.persistence.OptimisationJobPersistenceManager;
import de.unileipzig.irpsim.server.optimisation.postprocessing.PostProcessorHandler;
import de.unileipzig.irpsim.server.optimisation.queue.OptimisationJobHandler;

/**
 * Verwaltet Informationen zu einem GAMS-Simulationslauf.
 *
 * @author reichelt
 */
public abstract class Job {

   private static final Logger LOG = LogManager.getLogger(Job.class);

   protected long jobid;
   protected boolean hasError = false;
   protected boolean interrupted = false;
   protected File workspaceFolder;
   protected OptimisationJobPersistenceManager persistencemanager;

   private final Thread jobThread;

   private Date start = new Date();
   private final Date creation = new Date();

   protected BackendParametersMultiModel genericParameters, genericResults;

   protected PostProcessorHandler[] postProcessors;

   private State state = State.WAITING;

   protected final List<OptimisationYear> currentYears = new LinkedList<>();
   protected final boolean useInterpolation;
   protected final boolean interpolateTimeseries;
   private int yearIndex = 0;
   public static boolean deleteAfterwards = true;

   protected final OptimisationJobPersistent persistentJob;

   /**
    * Startet einen Simulationsjob mit dem übergebenen Parametersatz, dem übergebenen Modell-Quelltextordner und dem übergebenen Arbeitsordner. In dem Arbeitsordner wird für jedes
    * Jahr ein separater Ordner erzeugt, in den das Modell kopiert wird. Anschließend wird sequentiell in jedem Ordner die Optimierung durchgeführt.
    *
    * @param gp GAMS-Parametersatz
    * @param sourceFolder Quelltextordner mit Modell, das ausgeführt werden soll
    * @param workspaceFolder Arbeitsordner, in dem die Berechnungen ausgeführt und die Ergebnisse abgelegt werden sollen
    * @param useInterpolation Bestimmt, ob überhaupt interpoliert wird
    * @param interpolateTimeseries Bestimmt, ob das gesamte Ergebnis interpoliert wird einschließlich aller Zeitreihen, oder nur die Nachberechnungen
    */
   public Job(final boolean useInterpolation, final boolean interpolateTimeseries) {
      this.useInterpolation = useInterpolation;
      this.interpolateTimeseries = interpolateTimeseries;

      persistentJob = new OptimisationJobPersistent();

      final Runnable runSimulation = () -> {
         try {
            if (!interrupted) {
               importTimeseriesReferences();
            }
            if (!interrupted) {
               runMultiYearModel();
            }

         } catch (final Exception e) {
            throw new RuntimeException(e);
         }
      };

      jobThread = new Thread(runSimulation);
      jobThread.setUncaughtExceptionHandler(new UncaughtJobExceptionHandler(this));
   }

   public long getJobid() {
      return jobid;
   }

   public void importTimeseriesReferences() throws JsonProcessingException, InterruptedException {
      JobImportHandler importHandler = new JobImportHandler(genericParameters, persistentJob);
      importHandler.importScenarioTimeseries();
   }

   public BackendParametersMultiModel getGenericParameters() {
      return genericParameters;
   }
   
   public List<OptimisationYear> getCurrentYears() {
      return currentYears;
   }
   
   protected abstract void addYearsToQueue();

   /**
    * Überprüft, ob der übergebene Arbeitsordner schon existiert und nutzt dann einen Neuen und gibt eine Fehlermeldung aus.
    *
    * @param workspaceFolder der übergebene Arbeitsordner
    * @return der zu nutzende Arbeitsordner
    */
   protected File makeWorkspaceFolder(final File workspaceFolder) {
      File folderTest = new File(workspaceFolder, "job_" + jobid);
      while (folderTest.exists()) {
         folderTest = new File(workspaceFolder, folderTest.getName() + "_error");
         LOG.warn("Der Ordner " + folderTest + " existiert bereits für Job " + jobid + " - undefinierter Zustand?");
      }
      return folderTest;
   }

   /**
    * Weißt dem PersistentJob die initialen Werte zu.
    *
    * @param gp die übergebenen Parameter
    */
   protected void configurePersistentJob(final int modeldefinition, final OptimisationJobPersistent persistentJob) {
      persistentJob.setState(State.WAITING);
      persistentJob.setCreation(creation);
      persistentJob.setStart(start);

      persistentJob.setModelVersionHash(getModelVersionInfo());
      persistentJob.setModeldefinition(modeldefinition);
      persistentJob.setDescription(genericParameters.getDescription());
      String supportiveYearString = "";
      int simulationsteps = 0;
      for (final BackendParametersYearData year : genericParameters.getAllYears()) {
         if (year != null) {
            supportiveYearString += year.getConfig().getYear() + ";";
            simulationsteps += year.getConfig().getSimulationlength();
         }
      }
      persistentJob.setSimulationsteps(simulationsteps);
      persistentJob.getDescription().setSupportiveYears(supportiveYearString.substring(0, supportiveYearString.length() - 1));
      try {
         persistentJob.setJsonParameter(Constants.MAPPER.writeValueAsString(genericParameters.createJSONParameters()));
      } catch (final JsonProcessingException e1) {
         e1.printStackTrace();
      }
   }

   /**
    * Startet den Job, wenn er nicht mehr in der Warteschlange ist. Hierfür wird der eigentliche Ausführungs-Thread gestartet und der Status des Jobs auf Loading gesetzt.
    */
   public synchronized void start() {
      if (!interrupted) {
         if (jobThread.isAlive() || state != State.WAITING) {
            throw new RuntimeException("Jobs können nicht doppelt gestartet werden, Status war: " + state);
         }
         LOG.info("Id: {} Interrupted: {}", jobid, interrupted);
         setState(State.LOADING);
         start = new Date();
         persistentJob.setStart(start);
         jobThread.start();
      }
   }

   /**
    * Startet Model.
    *
    * @throws TimeseriesTooShortException Wird geworfen, wenn eine Zeitreihe die als Referenz übergeben wurde in der Datenbank zu kurz ist.
    * @throws IOException Tritt auf falls Fehler beim Lesen oder Schreiben auftreten
    * @throws InterruptedException
    */
   protected void runMultiYearModel() throws TimeseriesTooShortException {
      try {
         LOG.debug("Parametrisiere Modell... ");

         runYearModel();
         persistencemanager.persistEnd(new Date());

         if (interrupted) {
            setState(State.INTERRUPTED);
         } else if (hasError) {
            setState(State.FINISHEDERROR);
         } else {
            setState(State.FINISHED);
         }
         if (!interrupted && !hasError && deleteAfterwards) {
            FileUtils.deleteDirectory(workspaceFolder);
         }
      } catch (final InterruptedException | ClosedByInterruptException e) {
         if (Thread.interrupted()) {
            interrupted = true;
            setState(State.INTERRUPTED);
         }
      } catch (final IOException e) {
         e.printStackTrace();
         setState(State.ERROR);
      } finally {
         OptimisationJobHandler.getInstance().jobStoppedRunning(jobid);
      }
   }

   protected void addModelYearsToQueue(int modelIndex, ConnectedModelParameter model) {
      int simulatedYears = 0;
      int startYearIndex = 0;
      for (final BackendParametersYearData yeardata : model.getYeardata()) {
         if (yeardata == null) {
            startYearIndex++;
            continue;
         }
         
         ModelStarter caller = getCaller(modelIndex, startYearIndex, yeardata);
         final OptimisationYear year = new OptimisationYear(modelIndex, startYearIndex, jobid, persistencemanager
               , postProcessors, yeardata, genericResults, this, simulatedYears, caller);
         OptimisationYearHandler.addYear(year);
         currentYears.add(year);

         simulatedYears++;
         LOG.debug("Nachverarbeitung beendet");
         if (startYearIndex < model.getYeardata().length) {
            startYearIndex++;
         }
      }

   }

   private ModelStarter getCaller(int modelIndex, int startYearIndex, BackendParametersYearData yearParameters) {
      final ModelStarter caller;
      ModelInformation information = ModelDefinitionsEndpoint.getModelInformation(yearParameters.getConfig().getModeldefinition());
      if ("GDX".equals(information.getType())) {
         caller = new GAMSModelStarter(jobid, startYearIndex, modelIndex, yearParameters.getConfig().getModeldefinition());
      } else if ("Java".equals(information.getType())) {
         caller = new JavaModelStarter(jobid, startYearIndex, modelIndex, yearParameters);
      } else {
         throw new RuntimeException("Model type " + information.getType() + " not supported");
      }
      return caller;
   }

   protected abstract void interpolate();

   /**
    * Führt ein Jahres-Optimierungslauf aus.
    *
    * @throws IOException Tritt auf falls Fehler beim Lesen oder Schreiben auftreten
    * @throws TimeseriesTooShortException Wird geworfen, wenn eine Zeitreihe die als Referenz übergeben wurde in der Datenbank zu kurz ist.
    * @throws InterruptedException
    */
   public void runYearModel() throws IOException, TimeseriesTooShortException, InterruptedException {
      try {
         addYearsToQueue();
         waitForYears();

         if (hasError || interrupted) {
            LOG.debug("Beendet mit Fehler / unterbrochen.");
            return;
         } else {
            LOG.debug("Kein Fehler");
         }

         interpolate();
         postProcess();
      } catch (final RuntimeException ge) {
         ge.printStackTrace();
         LOG.error("Fehler aufgetreten: ", ge.getMessage());
         addError(ge.getLocalizedMessage());
      }

      finishYear();
   }

   protected abstract void postProcess();

   public abstract BackendParametersMultiModel fetchPostProcessingResults();

   private void waitForYears() throws InterruptedException {
      for (final OptimisationYear year : currentYears) {
         OptimisationYearHandler.join(year);
         synchronized (this) {
            yearIndex++;
            if (!interrupted) {
               setState(State.PARAMETERIZING);
            }
         }
      }
   }

   private void finishYear() {
      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final OptimisationJobPersistent persistentJob = em.find(OptimisationJobPersistent.class, jobid);
         final EntityTransaction et = em.getTransaction();
         et.begin();
         try {
            final String value = Constants.MAPPER.writeValueAsString(genericResults.createJSONParameters());
            persistentJob.setJsonResult(value);
         } catch (final JsonProcessingException e1) {
            e1.printStackTrace();
         }
         et.commit();
      }
   }

   /**
    * Liefert den aktuellen jobthread zurück.
    *
    * @return Der aktuelle jobthread.
    */
   public Thread getJobThread() {
      return jobThread;
   }

   /**
    * Liefert job-id des Simulationsjobs zurück.
    *
    * @return Die job-id des Simulationsjobs.
    */
   public long getId() {
      return jobid;
   }

   /**
    * Gibt mittels Wahrheitswert Auskunft darüber ob ein Simulationsjob fehlerfrei läuft.
    *
    * @return Wahrheitswert welcher Auskunft über das fehlerlose laufen des Simulationsjos gibt.
    */
   public boolean isRunning() {
      return jobThread.isAlive();
   }

   public State getState() {
      return state;
   }

   public synchronized void setState(final State state) {
      if (Thread.currentThread().isInterrupted()) {
         return;
      }
      LOG.info("Set State: {} {} {} Previous State: {}", state, interrupted, jobid, this.state);
      this.state = state;
      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final OptimisationJobPersistent dbJob = em.find(OptimisationJobPersistent.class, jobid);
         dbJob.setState(state);
         dbJob.setEnd(new Date());
         if (state.equals(State.FINISHED)) {
            dbJob.setFinishedsteps(dbJob.getSimulationsteps());
         }
         final EntityTransaction et = em.getTransaction();
         et.begin();
         em.persist(dbJob);
         LOG.info(jobid + " State: " + dbJob.getState() + " " + state);
         et.commit();
      }
   }

   public void setError(final boolean hasError) {
      this.hasError = hasError;
   }

   /**
    * Gibt Auskunft darüber ob ein Fehler aufgetreten ist oder nicht.
    *
    * @return Der Wahrheitswert, welcher Auskunft über das Auftreten eines Fehlers gibt.
    */
   public boolean hasError() {
      return hasError;
   }

   public GenericParameters getResults() {
      return genericResults;
   }

   /**
    * Liefert Stringrepräsentationen einzelner Simulationsjobs zurück, welche Auskunft darüber geben ob ein job gerafe läuft und dabei möglicherweise Fehler aufgetreten sind.
    *
    * @return Die jobid und der Status des jobs als String.
    */
   @Override
   public String toString() {
      return "ID: " + jobid + ", Jobthread aktiv: " + isRunning();
   }

   /**
    * Gibt den aktuellen Simulationsstatus zurück.
    *
    * @return Aktueller Simulationsstatus.
    */
   public synchronized IntermediarySimulationStatus getIntermediaryState() {
      final IntermediarySimulationStatus iss = new IntermediarySimulationStatus();
      final int totalSteps = genericParameters.fetchSimulationLength();
      final int finishedSteps = getFinishedSteps();
      iss.setId(getId());
      iss.setFinishedsteps(finishedSteps);
      iss.setRunning(isRunning());
      iss.setSimulationsteps(totalSteps);
      iss.setError(hasError);
      iss.setModelVersionHash(getModelVersionInfo());
      iss.setCreation(creation);
      iss.setStart(start);
      iss.setYearIndex(yearIndex);
      iss.setDescription(genericParameters.getDescription());
      iss.setState(state);

      List<YearState> yearStates = new LinkedList<>();
      for (OptimisationYear year : currentYears) {
         yearStates.add(year.getState());
      }
      iss.setYearStates(yearStates);

      if (hasError) {
         final JSONErrorMessage jm = new JSONErrorMessage("Im Job ist ein Fehler aufgetreten!");
         iss.setMessages(Arrays.asList(new JSONErrorMessage[] { jm }));
      }
      return iss;
   }

   protected abstract int getFinishedSteps();

   protected abstract String getModelVersionInfo();

   /**
    * Gibt die .lst-Datei des Simulationslaufs zurück.
    *
    * @param year Jahr, für das die Listing-Datei zurückgegeben werden soll
    * @return Die lst-Datei des Simulationslaufes
    */
   public File getListingFile(int modelindex, final int yearIndex) {
      final File yearWorkspace = PersistenceFolderUtil.getWorkspaceFolder(jobid, modelindex, yearIndex);
      LOG.debug("Datei: " + yearWorkspace + " " + yearIndex);
      for (final File lstFile : FileUtils.listFiles(yearWorkspace, new WildcardFileFilter("*.lst"), FalseFileFilter.INSTANCE)) {
         return lstFile;
      }
      return null;
   }

   /**
    * @param year Index des Stützjahres ohne zu interpolierende Jahre
    * @return Index des Stützjahres mit zu interpolierenden Jahren
    */
   public int fetchIndexOfSimulatedYear(final int year) {
      int yearIndex = 0;
      int simulatedYears = 0;
      boolean yearExists = false;
      for (final BackendParametersYearData yearParametersTry : genericParameters.getAllYears()) {
         if (yearParametersTry != null) {
            if (simulatedYears == year) {
               yearExists = true;
               break;
            }
            simulatedYears++;
         }
         yearIndex++;
      }
      if (!yearExists) {
         return -1;
      }
      return yearIndex;
   }

   public static final String GAMSRESULTFILE = "irpsimresult.gdx";
   public static final String GAMSPARAMETERFILE = "gamsirpsim.gdx";

   /**
    * Liefert die GDX-Datei zurück, die die Parameter des Jahres enthält.
    *
    * @param year Das spezifische Jahr.
    * @return Die GDX-Datei mit Parametern.
    */
   public File getGDXParameterFile(final int modelindex, final int year) {
      final File resultFile = new File(PersistenceFolderUtil.getWorkspaceFolder(jobid, modelindex, year), GAMSPARAMETERFILE);
      return resultFile;
   }

   /**
    * Liefert die GDX-Daten für ein bestimmtes Jahr.
    *
    * @param year Das bestimmte Jahr
    * @return Die GDX-Daten des Jahres
    */
   public File getGDXResultFile(final int modelindex, final int year) {
      final File resultFile = new File(PersistenceFolderUtil.getWorkspaceFolder(jobid, modelindex, year), GAMSRESULTFILE);
      return resultFile;
   }

   /**
    * @return True, wenn leere Jahre interpoliert werden, false, wenn leere Jahre ignoriert werden.
    */
   public boolean isUseInterpolation() {
      return useInterpolation;
   }

   public boolean isInterpolateOnlyPostprocessing() {
      return interpolateTimeseries;
   }

   /**
    * Beendet den Job, löscht ihn aber nicht aus der Liste im OptimisationJobHandler.
    *
    * @return ob alle GAMSHandler erfolgreich angehalten wurden
    */
   public boolean hold() {
      interrupted = true;
      LOG.info("Job wird angehalten, Thread: {}", jobThread.getName());
      final boolean interrupted = true;
      for (final OptimisationYear year : currentYears) {
         if (year != null) {
            year.kill();
         }
      }

      File dest = new File(workspaceFolder.getParent(), "job_" + jobid + "_held_0");
      final int i = 1;
      while (dest.exists()) {
         dest = new File(workspaceFolder.getParent(), dest.getName().substring(0, dest.getName().length() - 2) + "_" + i);
      }
      workspaceFolder.renameTo(dest);
      try {
         Thread.sleep(10); // / Warte, so dass Job die Möglichkeit hat, auf Interrupt zu reagieren
      } catch (final InterruptedException e) {
         e.printStackTrace();
      }
      LOG.debug("interrupt auf jobThread {} ausgeführt.", jobThread.getName());
      return interrupted;
   }

   /**
    * Beendet den Job.
    *
    * @return Wahrheitswert welcher Auskunft über Beendigung des Simulaionsjobs gibt.
    */
   public synchronized boolean kill() {
      LOG.info("Kill empfangen, Thread: {} Job: {}", jobThread.getName(), jobid);
      setState(State.ABORTED);
      interrupted = true;
      jobThread.interrupt();
      for (final OptimisationYear year : currentYears) {
         if (year != null) {
            year.kill();
         }
      }
      jobThread.interrupt();
      File dest = new File(workspaceFolder.getParent(), "job_" + jobid + "_killed");
      while (dest.exists()) {
         dest = new File(workspaceFolder.getParent(), dest.getName() + "_killerror");
      }

      OptimisationJobHandler.getInstance().jobKilled(this);
      LOG.debug("Kill beendet {}", jobid);
      return interrupted;
   }

   /**
    * Gibt die Ergebnisdatei des Laufs aus, wenn der Lauf beendet ist, andernfalls null.
    *
    * @param yearIndex Index des Jahres, für das die CSV-Datei ausgegeben werden soll
    * @return Die Ergebnisdatei des Laufs.
    */
   public File getCSVFile(int modelindex, final int yearIndex) {
      final File currentWorkspace = PersistenceFolderUtil.getWorkspaceFolder(jobid, modelindex, yearIndex);
      if (!currentWorkspace.exists()) {
         return null;
      }
      final List<ResultFileInfo> fileinfos = OptimisationJobUtils.getInstance().getResultFiles(currentWorkspace);
      LOG.info("Erstellte Gesamt-CSV-Datei für Jahr {} Workspace: {}", yearIndex, currentWorkspace);
      final File returnFile = new CompleteCSVReader().readCompleteCSV(currentWorkspace, fileinfos);
      return returnFile;
   }

   public synchronized void addError(final String message) {
      hasError = true;
      state = State.ERROR;
      LOG.info("Setze Fehler: {}", message);
      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
         final OptimisationJobPersistent dbJob = em.find(OptimisationJobPersistent.class, jobid);
         dbJob.setError(true);
         dbJob.setErrorMessage(message);
         dbJob.setState(State.ERROR);
         final EntityTransaction et = em.getTransaction();
         et.begin();
         em.persist(dbJob);
         LOG.info(jobid + " State: " + dbJob.getState() + " " + state);
         et.commit();
      }
   }
}
