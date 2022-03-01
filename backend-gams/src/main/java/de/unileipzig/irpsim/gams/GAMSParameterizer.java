package de.unileipzig.irpsim.gams;

import java.io.File;
import java.io.IOException;

import javax.persistence.EntityTransaction;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.gams.api.GAMSExecutionException;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.data.timeseries.DatabaseConnectionHandler;
import de.unileipzig.irpsim.core.simulation.data.BackendParameters;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersMultiModel;
import de.unileipzig.irpsim.core.simulation.data.BackendParametersYearData;
import de.unileipzig.irpsim.core.simulation.data.TimeseriesTooShortException;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersMultimodel;
import de.unileipzig.irpsim.core.simulation.data.json.JSONParametersSingleModel;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManager;
import de.unileipzig.irpsim.core.simulation.data.persistence.ClosableEntityManagerProxy;
import de.unileipzig.irpsim.core.simulation.data.persistence.OptimisationJobPersistent;
import de.unileipzig.irpsim.core.simulation.data.persistence.State;
import de.unileipzig.irpsim.core.utils.PersistenceFolderUtil;
import de.unileipzig.irpsim.core.utils.StreamGobbler;

public class GAMSParameterizer {

	private static final Logger LOG = LogManager.getLogger(GAMSParameterizer.class);

	public static void main(final String[] args) throws JsonParseException, JsonMappingException, IOException, TimeseriesTooShortException {
		if (args.length < 3) {
			LOG.error("Job-Id, Modellindex und Jahr müssen als Parameter übergeben werden.");
			System.exit(1);
		}
		
		final long id = Long.parseLong(args[0]);
		final int modelIndex = Integer.parseInt(args[1]);
		try {
			final int yearIndex = Integer.parseInt(args[2]);

			initDatabase(id);

			LOG.debug("Parametrisiere Job: {} Jahr: {}", id, yearIndex);

			final File concreteWorkspaceFolder = PersistenceFolderUtil.getWorkspaceFolder(id, modelIndex, yearIndex);

			final GAMSHandler handler = new GAMSHandler(concreteWorkspaceFolder);
			final BackendParametersYearData parametersBackend = getParameters(id, modelIndex, yearIndex);

			final GAMSModelParametrisationUtil parameterizer = new GAMSModelParametrisationUtil(handler, parametersBackend, yearIndex);
			parameterizer.loadParameters();
			parameterizer.parameterizeModel();

			final Process p = Runtime.getRuntime().exec("ls -lah", new String[0], concreteWorkspaceFolder);
			StreamGobbler.showFullProcess(p);

			handler.expose();

			final Process p2 = Runtime.getRuntime().exec("ls -lah", new String[0], concreteWorkspaceFolder);
			StreamGobbler.showFullProcess(p2);

			// handler.startBlocking();

			LOG.info("Parametrisierung {} (Jahr {})beendet", id, yearIndex);
		} catch (IOException | TimeseriesTooShortException e) {
			LOG.info("Persistiere " + id);
			persistError(id, e);
			e.printStackTrace();
			System.exit(3);
		} catch (final GAMSExecutionException e) {
			LOG.info("Persistiere " + id);
			persistError(id, e);
			e.printStackTrace();
			System.exit(2);
		} catch (final Throwable t) {
			LOG.info("Persistiere " + id);
			persistError(id, t);
			t.printStackTrace();
			System.exit(4);
		}
	}

   private static BackendParametersYearData getParameters(final long id, int modelIndex, int yearIndex) throws JsonProcessingException, JsonMappingException {
      OptimisationJobPersistent job;
      try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
      	job = em.find(OptimisationJobPersistent.class, id);
      }

      final String parameters = job.getJsonParameter();

      try {
         final JSONParametersSingleModel parametersJSON = new ObjectMapper().readValue(parameters, JSONParametersSingleModel.class);
         final BackendParameters parametersBackend = new BackendParameters(parametersJSON);
         BackendParametersYearData yearBackend = parametersBackend.getYeardata()[yearIndex];
         return yearBackend;
      } catch (UnrecognizedPropertyException e) {
         final JSONParametersMultimodel parametersJSON = new ObjectMapper().readValue(parameters, JSONParametersMultimodel.class);
         final BackendParametersMultiModel parametersBackend = new BackendParametersMultiModel(parametersJSON);
         final BackendParametersYearData yearBackend = parametersBackend.getModels()[modelIndex].getYeardata()[yearIndex];
         return yearBackend;
      }
      
   }

   public static void initDatabase(final long id) {
      final String dburl = System.getenv(Constants.IRPSIM_DATABASE_URL), user = System.getenv(Constants.IRPSIM_DATABASE_USER), pw = System.getenv(Constants.IRPSIM_DATABASE_PASSWORD);

      boolean fail = false;
      if (user == null) {
      	LOG.error("Datenbankbenutzer nicht gesetzt.");
      	fail = true;
      }
      if (dburl == null) {
      	LOG.error("Datenbank-URL nicht gesetzt.");
      	fail = true;
      }
      if (fail) {
      	persistError(id, "Interner Fehler: Nutzer/URL der Datenbank nicht gesetzt");
      	System.exit(1);
      }

      DatabaseConnectionHandler.getInstance().setUser(user);
      DatabaseConnectionHandler.getInstance().setPassword(pw);
      DatabaseConnectionHandler.getInstance().setUrl(dburl);
   }

	public static void persistError(final long jobid, final Throwable e) {
		persistError(jobid, e.getMessage() + "\n" + e.getStackTrace());
	}

	public static void persistError(final long jobid, final String message) {
		try (ClosableEntityManager em = ClosableEntityManagerProxy.newInstance()) {
			final OptimisationJobPersistent job = em.find(OptimisationJobPersistent.class, jobid);
			final EntityTransaction et = em.getTransaction();
			et.begin();
			job.setState(State.ERROR);
			job.setError(true);
			job.setErrorMessage(message);
			LOG.info("Setze Nachricht: {}", message);
			em.persist(job);
			et.commit();
		}
	}
}
