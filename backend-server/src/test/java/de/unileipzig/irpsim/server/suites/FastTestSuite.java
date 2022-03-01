package de.unileipzig.irpsim.server.suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.unileipzig.irpsim.core.ParameterBaseDependenciesUtilTest;
import de.unileipzig.irpsim.core.SchemaTest;
import de.unileipzig.irpsim.server.data.stammdaten.TestStammdaten;
import de.unileipzig.irpsim.server.data.stammdaten.TestStammdatenParent;
import de.unileipzig.irpsim.server.data.stammdaten.TestStammdatumDelete;
import de.unileipzig.irpsim.server.data.stammdaten.TestStammdatumImport;
import de.unileipzig.irpsim.server.data.stammdaten.TestWrongJSON;
import de.unileipzig.irpsim.server.data.stammdaten.datensatz.AlgebraicDataTest;
import de.unileipzig.irpsim.server.endpoints.DeleteOldJobTest;
import de.unileipzig.irpsim.server.endpoints.OptimisationParametersTest;
import de.unileipzig.irpsim.server.endpoints.ScenarioReferencesCreationTest;
import de.unileipzig.irpsim.server.javamodel.TestModelStart;
import de.unileipzig.irpsim.server.optimisation.IRPoptMultipleYearTest;
import de.unileipzig.irpsim.server.optimisation.JobStateTest;
import de.unileipzig.irpsim.server.optimisation.OptimisationFileTest;
import de.unileipzig.irpsim.server.optimisation.comparators.SyncableParameterHandlerTest;
import de.unileipzig.irpsim.server.optimisation.endpoints.GdxExportEndpointTest;
import de.unileipzig.irpsim.server.optimisation.json2sqlite.TestJSONTransformation;
import de.unileipzig.irpsim.server.optimisation.persistence.IRPoptPersistenceTest;
import de.unileipzig.irpsim.server.optimisation.persistence.ImportResultHandlerTest;
import de.unileipzig.irpsim.server.optimisation.persistence.JobPersistenceManagerTest;
import de.unileipzig.irpsim.server.optimisation.postprocessing.InterpolationHandlerTest;
import de.unileipzig.irpsim.server.optimisation.postprocessing.SelectiveInterestTest;

/**
 * Testsuite für die schnelle Testausführung zur grundlegenden Sicherstellung der Funktionalität. Wird für das Testen nach einem Commit benutzt.
 *
 * @author reichelt
 */
@RunWith(Suite.class)
@SuiteClasses({
		SchemaTest.class,
		ParameterBaseDependenciesUtilTest.class,
		OptimisationParametersTest.class,
		SelectiveInterestTest.class,
		ScenarioReferencesCreationTest.class,
		ImportResultHandlerTest.class,
		IRPoptMultipleYearTest.class,
		IRPoptPersistenceTest.class,
		// BasismodellMultipleYearPersistenceTest.class,
		JobStateTest.class,
		// TestNonReferencedDeletion.class,
		// BasismodellReferencedTest.class,
		DeleteOldJobTest.class,
		InterpolationHandlerTest.class,
		JobPersistenceManagerTest.class,
		OptimisationFileTest.class,
		// NoDeleteAtServerStartTest.class,
		AlgebraicDataTest.class,
		TestWrongJSON.class,
		TestStammdatumImport.class,
		TestStammdaten.class,
		TestStammdatenParent.class,
		TestStammdatumDelete.class,
		GdxExportEndpointTest.class,
		TestJSONTransformation.class,
		TestModelStart.class,
		SyncableParameterHandlerTest.class
})
public class FastTestSuite {

}
