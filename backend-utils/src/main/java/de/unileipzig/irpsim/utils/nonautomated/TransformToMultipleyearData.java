package de.unileipzig.irpsim.utils.nonautomated;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.json.JSONArray;
import org.json.JSONObject;

import de.unileipzig.irpsim.core.Constants;
import de.unileipzig.irpsim.core.data.simulationparameters.OptimisationScenario;
import de.unileipzig.irpsim.core.data.timeseries.DatabaseConnectionHandler;

/**
 * Transformiert die Einjahresdaten in der angegebenen Datenbank in Mehrjahresdaten.
 *
 * @author reichelt
 */
public final class TransformToMultipleyearData {

	private static final Logger LOG = LogManager.getLogger(TransformToMultipleyearData.class);

	/**
	 * Hilfsklassenkonstruktor.
	 */
	private TransformToMultipleyearData() {
	}

	/**
	 * @param args
	 *            TODO
	 */
	public static void main(final String[] args) throws ParseException {
		final Options options = new Options();

		final Option nameOption = Option.builder(Constants.DBURL).required(false).hasArg().desc("Gibt an, unter welcher URL die Datenbank erreichbar ist").build();
		options.addOption(nameOption);

		final Option userOption = Option.builder(Constants.DBUSER).required(false).hasArg().desc("Gibt den Datenbankbenutzer für den Datenbankimport an").build();
		options.addOption(userOption);

		final Option passwordOption = Option.builder(Constants.DBPW).required(false).hasArg().desc("Gibt das Datenbankpasswort für den Datenbankimport an").build();
		options.addOption(passwordOption);

		final CommandLineParser parser = new DefaultParser();
		CommandLine line;
		line = parser.parse(options, args);

		String dburl = null, user = null, pw = null;
		if (line.hasOption(Constants.DBURL)) {
			dburl = line.getOptionValue(Constants.DBURL);
		} else {
			dburl = System.getenv(Constants.IRPSIM_DATABASE_URL);
		}
		if (line.hasOption(Constants.DBUSER)) {
			user = line.getOptionValue(Constants.DBUSER);
		} else {
			user = System.getenv(Constants.IRPSIM_DATABASE_USER);
		}
		if (line.hasOption(Constants.DBPW)) {
			pw = line.getOptionValue(Constants.DBPW);
		} else {
			pw = System.getenv(Constants.IRPSIM_DATABASE_PASSWORD);
		}

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
			final HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("backend.jar", options);
			LOG.error("Weiterhin ist es möglich, mittels der Umgebungsvariablen " + Constants.IRPSIM_DATABASE_URL + ", " + Constants.IRPSIM_DATABASE_PASSWORD + " und "
					+ Constants.IRPSIM_DATABASE_URL + " die Datenbankzugangsdaten zu setzen");
			System.exit(1);
		}

		DatabaseConnectionHandler.getInstance().setUser(user);
		DatabaseConnectionHandler.getInstance().setPassword(pw);
		DatabaseConnectionHandler.getInstance().setUrl(dburl);

		DatabaseConnectionHandler.getInstance().getConnection();

		final EntityManager em = DatabaseConnectionHandler.getInstance().getEntityManager();
		final Session session = (Session) em.getDelegate();
		@SuppressWarnings("unchecked")

		CriteriaBuilder builder = session.getCriteriaBuilder();
		CriteriaQuery<OptimisationScenario> criteria = builder.createQuery(OptimisationScenario.class);
		Root<OptimisationScenario> queryRoot = criteria.from(OptimisationScenario.class);

		criteria.select(queryRoot);

		final List<OptimisationScenario> list = session.createQuery(criteria).list();

		for (final OptimisationScenario metadata : list) {
			final String content = metadata.getData();
			final JSONObject object = new JSONObject();
			object.put("years", new JSONArray("[" + content + "]"));
			final EntityTransaction et = em.getTransaction();
			et.begin();
			metadata.setData(object.toString());
			session.saveOrUpdate(metadata);
			et.commit();
		}
	}
}
