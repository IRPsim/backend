package de.unileipzig.irpsim.core.utils;

import java.io.File;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import de.unileipzig.irpsim.core.Constants;

/**
 * Enthält alle Testszenarien.
 *
 * @author krauss
 */
public enum TestFiles {
	TEST("Basismodell_test"), 
	   ERROR("Basismodell_error"), 
	   MULTIPLE_YEAR("Basismodell_multiple_years"), 
	   INTERPOLATION("Basismodell_interpolation"), 
	   MULTI_THREE_DAYS("Basismodell_multiple_years_three_days"), 
	   DAYS_21("Basismodell_days_21"), 
	   DAYS_3("Basismodell_days_3"), 
	   FULL_YEAR("Basismodell_full_year_test"),
	   IRPACT("IRPact"),
		IRPACT_MULTIPLE_YEAR("IRPact_multiple_years");

	private final String name;
	public static final File ALTERNATIVE_DEPENDENCY = new File(Constants.CORE_MODULE_PATH, "src/test/resources/alternative_dependencies.json");
	public static final File TEST_PERSISTENCE_FOLDER = new File(Constants.SERVER_MODULE_PATH, "target/persistence-tests");

	/**
	 * Enumkonstruktor.
	 *
	 * @param name
	 *            Dateispezifikation des Szenarios
	 */
	TestFiles(final String name) {
		this.name = name;
	}

	private static final File TEST_RES = new File(Constants.SERVER_MODULE_PATH, "src/test/resources/");
	public static final File TEST_PARAMS = new File(TEST_RES, "scenarios");
	public static final File TEST_INTERPOLATION = new File(TEST_PARAMS, "Test_Interpolation.json");

	/**
	 * @return Die zugehörige Testdatei
	 */
	public File make() {
		return new File(TEST_PARAMS, name + ".json");
	}

	/**
	 * Gibt alle Testdateien für das Modell zurück die existieren.
	 *
	 * @param model
	 *            Das Modell
	 * @return alle existierenden Testdateien
	 */
	public static Set<File> makeAll() {
		final Set<File> allFiles = new HashSet<>();
		EnumSet.allOf(TestFiles.class).forEach(tf -> {
			final File file = tf.make();
			if (file.exists()) {
				allFiles.add(file);
			}
		});
		return allFiles;
	}
}
