package de.unileipzig.irpsim.server.optimisation.queue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.hibernate.Session;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationYearPersistent;
import de.unileipzig.irpsim.core.simulation.data.persistence.State;
import de.unileipzig.irpsim.server.endpoints.CleanupEndpoint;
import de.unileipzig.irpsim.server.optimisation.Job;
import de.unileipzig.irpsim.server.optimisation.MultiModelJob;
import de.unileipzig.irpsim.server.optimisation.OptimisationYearHandler;
import de.unileipzig.irpsim.server.optimisation.SingleModelJob;

/**
 * Verwaltet die Optimierungsjobs. Dabei werden sowohl noch laufende Optimierungsjobs (OptimisationJob) als auch persistierte, eventuell beendete Optimierungsjobs (SimulationJobPersistent) gespeichert
 * und verwaltet.
 *
 * @author reichelt
 */
public final class OptimisationJobHandler {

	private static final Logger LOG = LogManager.getLogger(OptimisationJobHandler.class);
	public static int maxParallelJobs = 1;

	private static OptimisationJobHandler instance;

	final Map<Long, Job> runningJobs = new HashMap<>();
	private Queue<Job> waitingJobs = new LinkedList<>();

	private final boolean useInterpolation;
	private final boolean interpolateTimeseries;

	/**
	 * Erzeugt neue SimulationJobHandlerinstaz falls diese null ist.
	 *
	 * @return Die erzeugte Instanz.
	 */
	public static synchronized OptimisationJobHandler getInstance() {
		if (null == instance) {
			instance = new OptimisationJobHandler();
		}
		return instance;
	}

	/**
	 * Privater Konstruktor erstellt neue SimulationJobHandler-Instanz nach Singletonarchitektur.
	 */
	private OptimisationJobHandler() {
		final int check = checkInterpolationSetting();
		useInterpolation = check > 0;
		interpolateTimeseries = check > 1;
	}

	/**
	 * Überprüft die Interpolationseinstellungen. Über Umgebungsvariable IRPSIM_INTERPOLATION geregelt. Mögliche Werte: ALL, POSTPROCESSING, OFF.
	 *
	 * @return Der Rückgabewert ist 0, 1 oder 2 wenn nicht, nur die Nachberechnung oder alles interpoliert werden soll.
	 */
	private static int checkInterpolationSetting() {
		int check = 0;
		final String interpolation = System.getenv("IRPSIM_INTERPOLATION");
		if (interpolation == null) {
			LOG.debug("Verhalten bzgl. Interpolation nicht definiert, default ohne Interpolation. \n Nutze Umgebungsvariable \"IRPSIM_INTERPOLATION\"!");
		} else {
			switch (interpolation.toUpperCase()) {
			case "ALL":
				check++;
			case "POSTPROCESSING":
				check++;
			case "OFF":
			case "NONE":
				LOG.info("Interpolation: {}, d.h. Postprocessing: {}, Zeitreihen: {}", interpolation, check > 0, check > 1);
				break;
			default:
				LOG.error("Verhalten bzgl. Interpolation falsch definiert, default ohne Interpolation. \n " + "Nutze Werte \"ALL\", \"POSTPROCESSING\" oder \"OFF\"!");
			}
		}
		return check;
	}

	public static int getMaxParallelJobs() {
		return maxParallelJobs;
	}

	public static void setMaxParallelJobs(final int maxParallelJobs) {
		OptimisationJobHandler.maxParallelJobs = maxParallelJobs;
	}

	/**
	 * Gibt den persistierten Simulationsjob mit der angefragten Id zurück.
	 *
	 * @param id Die angefragte id des simulationjobs.
	 * @return Der Persistierte Simulationsjob an der bestimmten Id.
	 */
	public OptimisationJobPersistent getJob(final long id) {
		try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final Session session = em.unwrap(Session.class);
			final OptimisationJobPersistent job = session.get(OptimisationJobPersistent.class, id);
			if (job != null) {
				Hibernate.initialize(job);
				for (final OptimisationYearPersistent year : job.getYears()) {
					Hibernate.initialize(year);
				}
			}
			return job;
		}
	}

	/**
	 * Gibt den noch laufenden Optimierungsjob mit der Angefragten Id zurück, bzw. null wenn kein laufender Simulationsjob mit der angefragten Id vorhanden ist.
	 *
	 * @param id Die angefragte Id des runningJobs.
	 * @return Der über die Id refferenzierte runningJob.
	 */
	public Job getRunningJob(final long id) {
		Job job;
		synchronized (runningJobs) {
			job = runningJobs.get(id);
			if (job == null) {
				final Optional<Job> findFirst = waitingJobs.stream().filter(current -> current.getId() == id).findFirst();
				if (findFirst.isPresent()) {
					job = findFirst.get();
				}
			}
		}
		return job;
	}

	/**
	 * Gibt alle persistierten Simulationsjobs zurück.
	 *
	 * @return Liste der persistiertern Jobs.
	 */
	public List<OptimisationJobPersistent> getPersistedJobs() {
		try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
		   final CriteriaBuilder cBuilder = em.getCriteriaBuilder();
		   final CriteriaQuery<OptimisationJobPersistent> ojpQuery = cBuilder.createQuery(OptimisationJobPersistent.class);
		   ojpQuery.from(OptimisationJobPersistent.class);

			return em.createQuery(ojpQuery).getResultList();
		}
	}

	/**
	 * Startet einen neuen SimulationsJob mit dem übergebenen Parametersatz, dem übergebenen Modellquelltext-Ordner und dem übergebenen Ordner, in dem die Berechnungne ausgeführt werden.
	 *
	 * @param gp Der übergebene GAMSParameter
	 * @param sourceFolder Ordner, in dem der GAMS-Modellquelltext liegt
	 * @return Die Id des erstellten SimulationJobs
	 * @throws IOException Wird geworfen falls Fehler beim lesen oder schreiben der Datei auftreten
	 */
	public long newJob(final JSONParametersMultimodel gp) throws IOException {
	   LOG.debug("New job received");
	   Job job;
	   if (gp.getModels().size() > 1) {
	      job = new MultiModelJob(gp, useInterpolation, interpolateTimeseries);
	   } else {
	      job = new SingleModelJob(gp, useInterpolation, interpolateTimeseries);
	   }
      startJob(job);
      return job.getId();
   }

	private void startJob(final Job job) {
		synchronized (runningJobs) {
		   LOG.debug("Running jobs: {}", runningJobs.keySet());
		   LOG.debug("Waiting jobs: {}", waitingJobs);
		   LOG.debug("Cleaning: {}", CleanupEndpoint.isCleaning());
			if (runningJobs.size() < maxParallelJobs && !CleanupEndpoint.isCleaning()
					&& OptimisationYearHandler.canTakeMoreJobs()) {
				job.start();
				runningJobs.put(job.getId(), job);
			} else {
				waitingJobs.add(job);
			}
		}
	}

	/**
	 * Erstellt einen neuen Arbeitsordner, in dem der folgende GAMS-Lauf ausgeführt werden soll.
	 *
	 * @return Arbeitsordner, in dem GAMS ausgeführt werden kann
	 * @throws IOException Fehler, falls die Erstellung fehlschlägt
	 */
	public File getWorkingFolderName() throws IOException {
		final File target = new File("target");
		target.mkdir();
		final File workspaceDirectory = File.createTempFile("run", ".tmp", target);
		workspaceDirectory.delete(); // delete created temporary file
		if (workspaceDirectory.exists()) {
			throw new RuntimeException("Dieser Ordner darf hier noch nicht existieren!");
		}
		workspaceDirectory.mkdir();

		return workspaceDirectory;
	}

	/**
	 * Beendet alle laufenden SimulationJobs.
	 */
	public void killAllJobs() {
		LOG.debug("Beende alle Jobs");
		for (final Map.Entry<Long, Job> entry : new HashMap<>(runningJobs).entrySet()) {
			if (entry.getValue().isRunning()) {
				entry.getValue().kill();
			}
		}
	}

	/**
	 * Entfernt den Simulationsjob mit der laufenden Id aus der liste laufender Simulationsjob; sollte nach dem Ende jedes Jobs aufgerufen werden. *
	 *
	 * @param id Die laufende Id des Simulationjobs.
	 */
	public void jobStoppedRunning(final long id) {
		synchronized (runningJobs) {
			runningJobs.remove(id);
			LOG.debug("Wartende Jobs: {}", waitingJobs.size());
			startNextJob();
		}
	}

	void startNextJob() {
		if (runningJobs.size() < maxParallelJobs && waitingJobs.size() > 0) {
			final Job nextRunJob = waitingJobs.poll();
			LOG.debug("Nächster Job: {}", nextRunJob);
			nextRunJob.start();
			runningJobs.put(nextRunJob.getId(), nextRunJob);
		}
	}

	/**
	 * Entfernt den extern beendeten Optimierungsjob aus der Liste laufender Optimierungsjobs bzw. aus der Warteschlange noch auszuführender Jobs.
	 *
	 * @param optimisationJob Der extern beendete Job.
	 */
	public void jobKilled(final Job optimisationJob) {
		LOG.debug("Entferne Job: {} Running: {} Waiting: {}", optimisationJob.getId(), runningJobs.containsKey(optimisationJob.getId()), waitingJobs.contains(optimisationJob));
		if (runningJobs.containsKey(optimisationJob.getId())) {
			jobStoppedRunning(optimisationJob.getId());
		} else if (waitingJobs.contains(optimisationJob)) {
			LOG.debug("Entferne wartenden Job: {}", optimisationJob.getId());
			waitingJobs.remove(optimisationJob);
		}
	}

	/**
	 * Gibt alle Jobs, d.h. sowohl aktuell laufende als auch in der Warteschlange befindliche Jobs, aus. Persistierte Jobs werden nicht ausgegeben.
	 *
	 * @return Alle Optimierungsjobs
	 */
	public Collection<Job> getActiveJobs() {
		final Collection<Job> jobs = new LinkedList<>();
		jobs.addAll(runningJobs.values());
		jobs.addAll(waitingJobs);
		return jobs;
	}

	/**
	 * Gibt die Liste der aktuell laufenden Jobs zurück.
	 *
	 * @return Liste der aktuell laufenden Jobs
	 */
	public Collection<Job> getRunningJobs() {
		return runningJobs.values();
	}

	/**
	 * Gibt die Liste der aktuell wartenden Jobs zurück.
	 *
	 * @return Liste der aktuell wartenden Jobs
	 */
	public Collection<Job> getWaitingJobs() {
		LOG.debug("Wartende Jobs: {}", waitingJobs);
		return waitingJobs;
	}

	/**
	 * Ordnet die Ausführungsreihenfolge neu.
	 *
	 * @param ids Neue Reihenfolge
	 * @return Gibt entweder eine Erfolgsnachricht zurück oder eine Auflistung aller JobIds, die nicht in der neuen Reihenfolge vorhanden sind.
	 */
	public String reschedule(final Long[] ids) {
		String missing = "";
		for (final Job ojob : waitingJobs) {
			if (!Arrays.asList(ids).contains(ojob.getId())) {
				missing += ojob.getId() + ",";
			}
		}
		if (missing.length() > 0) {
			return missing;
		}

		final Queue<Job> reorderedQueue = new LinkedList<>();

		for (final long id : ids) {
			for (final Job job : waitingJobs) {
				if (job.getId() == id) {
					reorderedQueue.add(job);
				}
			}
		}
		waitingJobs = reorderedQueue;
		return "Reihenfolgenänderung erfolgreich";
	}

	/**
	 * Weist allen Jobs den Status INTERRUPTED zu, stoppt und persistiert sie und speichert in JobOrder, welche Jobs neu gestartet werden müssen..
	 */
	public void holdAllJobs() {
		LOG.info("Setze alle Jobs auf interrupted.");
		try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			em.getTransaction().begin();
			em.createNativeQuery("TRUNCATE TABLE " + JobOrder.class.getSimpleName()).executeUpdate();
			em.getTransaction().commit();
		}
		int position = 0;
		final List<JobOrder> jobs = new ArrayList<>();
		for (final Job job : runningJobs.values()) {
			job.setState(State.INTERRUPTED);
			job.hold();
			jobs.add(new JobOrder(position, job.getId()));
			position++;
			LOG.info("Job {} angehalten ", job.getId());
		}
		runningJobs.clear();
		for (final Job job : waitingJobs) {
			job.setState(State.INTERRUPTED);
			job.hold();
			jobs.add(new JobOrder(position, job.getId()));
			position++;
			LOG.info("Job {} angehalten ", job.getId());
		}
		waitingJobs.clear();
		try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			em.getTransaction().begin();
			for (final JobOrder job : jobs) {
				em.persist(job);
			}
			em.getTransaction().commit();
		}
	}

	/**
	 * Started die Jobs neu, die in der JobOrder Tabelle hinterlegt wurden.
	 */
	public void restartJobs() {

		final Map<Long, JSONParametersMultimodel> parameterSets = fetchInterruptedJobs();

		LOG.info("Starte Jobs neu: {}", parameterSets.keySet());

		restartJobList(parameterSets);
	}

   private void restartJobList(final Map<Long, JSONParametersMultimodel> parameterSets) {
      for (final Entry<Long, JSONParametersMultimodel> parameterSet : parameterSets.entrySet()) {
			final JSONParametersMultimodel parametersJSON = parameterSet.getValue();
			parametersJSON.getDescription().setCreator(parametersJSON.getDescription().getCreator() + " (neugestartet vom System von job " + parameterSet.getKey() + ")");
			try {
				newJob(parametersJSON);
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
   }

   private Map<Long, JSONParametersMultimodel> fetchInterruptedJobs() {
      final Map<Long, JSONParametersMultimodel> parameterSets = new LinkedHashMap<>();

		try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final TypedQuery<JobOrder> allJobsQuery = em.createQuery("SELECT jo from JobOrder jo", JobOrder.class);
			final List<JobOrder> resultList = allJobsQuery.getResultList();
			Collections.sort(resultList, (jo1, jo2) -> Integer.compare(jo1.getPosition(), jo2.getPosition()));
			for (final JobOrder job : resultList) {
				final OptimisationJobPersistent foundJob = em.find(OptimisationJobPersistent.class, job.getId());
				final String jsonParameter = foundJob.getJsonParameter();
				JSONParametersMultimodel restartedJobParameters = new ObjectMapper().readValue(jsonParameter, JSONParametersMultimodel.class);
            parameterSets.put(job.getId(), restartedJobParameters);
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
      return parameterSets;
   }

	/**
	 *
	 */
	public void handleUndefinedJobs() {

		final Set<State> undefinedStates = new HashSet<>();

		for (final State state : State.values()) {
			if (state.compareTo(State.INTERRUPTED) < 0) {
				undefinedStates.add(state);
			}
		}

		final Map<Long, JSONParametersMultimodel> parameterSets = fetchUndefinedJobs(undefinedStates);

		LOG.info("Starte nicht wohl definierte Jobs neu: {}", parameterSets.keySet());

		restartJobList(parameterSets);
	}

   private Map<Long, JSONParametersMultimodel> fetchUndefinedJobs(final Set<State> undefinedStates) {
      final Map<Long, JSONParametersMultimodel> parameterSets = new LinkedHashMap<>();

		try (final ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final TypedQuery<OptimisationJobPersistent> query = em.createQuery("SELECT ojp from OptimisationJobPersistent ojp WHERE ojp.state IN :states", OptimisationJobPersistent.class);
			query.setParameter("states", undefinedStates);
			final List<OptimisationJobPersistent> undefinedJobs = query.getResultList();

			em.getTransaction().begin();
			for (final OptimisationJobPersistent undefinedJob : undefinedJobs) {
				LOG.info("Nicht wohldefinierter Job: {} {}", undefinedJob.getId(), undefinedJob.getState());
				undefinedJob.setState(State.INTERRUPTED);
				em.unwrap(Session.class).saveOrUpdate(undefinedJob);
				final String jsonParameter = undefinedJob.getJsonParameter();
				parameterSets.put(undefinedJob.getId(), new ObjectMapper().readValue(jsonParameter, JSONParametersMultimodel.class));
			}
			em.getTransaction().commit();

		} catch (final IOException e) {
			e.printStackTrace();
		}
      return parameterSets;
   }
}
