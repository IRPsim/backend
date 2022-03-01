package de.unileipzig.irpsim.core;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Sammlung allgemein genutzter Konstanzen.
 *
 * @author krauss
 */
public final class Constants {

	public static final String DBURL = "dburl";
	public static final String DBUSER = "dbuser";
	public static final String DBPW = "dbpassword";
	public static final String HELP = "help";
	public static final String IRPSIM_DATABASE_URL = "IRPSIM_MYSQL_URL";
	public static final String IRPSIM_DATABASE_USER = "IRPSIM_MYSQL_USER";
	public static final String IRPSIM_PORT = "IRPSIM_PORT";
	public static final String IRPSIM_DATABASE_PASSWORD = "IRPSIM_MYSQL_PASSWORD";
	public static final String IRPSIM_PERSISTENCEFOLDER = "IRPSIM_PERSISTENCEFOLDER";
	public static final String IRPSIM_PARALLEL_JOBS = "IRPSIM_PARALLEL_JOBS";
	public static final String IRPSIM = "IRPsim";
	public static final String IRPSIM_MYSQL_JAVAPATH = "IRPSIM_MYSQL_JAVAPATH";
	public static final String IRPSIM_MYSQL_PATH = "IRPSIM_MYSQL_PATH";

	public static final double DOUBLE_DELTA = 0.001;
	public static final double MEGA = 1E7;
	public static final int ZERO_TIMESERIES_NAME = 0;
	public static final List<Number> ZERO_TIMESERIES_VALUES = makeZeroTimeseries();
	public static final double QUARTER = 0.25;

	public static final int YEAR_VALUES = 35040;
	public static final String GAMS_MODULE_PATH = "../backend-gams/";
	public static final String SERVER_MODULE_PATH = "../backend-server/";
	public static final String CORE_MODULE_PATH = "../backend-core/";

	public static final Pattern SEMICOLONPATTERN = Pattern.compile(";");
	
	public static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

	public static final int IRPACT_MODELDEFINITION = 3;
	/**
	 * Legt die Liste für die Nullzeitreihe an.
	 *
	 * @return Die erstellte Liste.
	 */
	private static List<Number> makeZeroTimeseries() {
		final List<Number> zeroTimes = new ArrayList<>();
		for (int i = 0; i < YEAR_VALUES; i++) {
			zeroTimes.add(0d);
		}
		return zeroTimes;
	}

	public static final File SCENARIO_FOLDER_JSON = new File(SERVER_MODULE_PATH + "src/main/resources/scenarios/");
//	public static final File SCENARIO_FILE_JSON = new File(SERVER_MODULE_PATH + "src/main/resources/scenarios/Basismodell.json");
//	public static final File FULL_YEAR_SCENARIO_FILE_JSON = new File(SERVER_MODULE_PATH + "src/main/resources/scenarios/Basismodell_full_year.json");
	public static final String TIMESET = "set_ii";
	public static final String LD_GAMS_PATH = "LD_GAMS_PATH";

	/**
	 * Hilfsklassenkonstruktor.
	 */
	private Constants() {
	}
}
