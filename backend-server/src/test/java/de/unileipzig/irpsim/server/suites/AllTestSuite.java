package de.unileipzig.irpsim.server.suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.unileipzig.irpsim.server.endpoints.DeleteOldJobTest;
import de.unileipzig.irpsim.server.endpoints.OptimisationParametersTest;
import de.unileipzig.irpsim.server.endpoints.ScenarioReferencesCreationTest;
import de.unileipzig.irpsim.server.optimisation.IRPoptReferencedTest;
import de.unileipzig.irpsim.server.optimisation.IRPoptServerTest;
import de.unileipzig.irpsim.server.optimisation.OptimisationFileTest;
import de.unileipzig.irpsim.server.optimisation.TestIntermediaryResults;
import de.unileipzig.irpsim.server.optimisation.persistence.IRPoptPersistenceTest;
import de.unileipzig.irpsim.server.optimisation.postprocessing.BasismodellInterpolationTest;
import de.unileipzig.irpsim.server.optimisation.postprocessing.InterpolationHandlerTest;
import de.unileipzig.irpsim.server.performance.GetLoadServerPerformanceTest;

/**
 * Testsuite für die Sicherstellung aller Funktionalitäten. Wird für eine nachgelagerte Prüfung bzw. für das Deployment eines neuen Produktivsystems genutzt.
 * 
 * @author reichelt
 */
@RunWith(Suite.class)
@SuiteClasses({
		ScenarioReferencesCreationTest.class,
		IRPoptReferencedTest.class,
		DeleteOldJobTest.class,
		InterpolationHandlerTest.class,
		BasismodellInterpolationTest.class,
		OptimisationParametersTest.class,
		GetLoadServerPerformanceTest.class,
		IRPoptPersistenceTest.class,
		IRPoptServerTest.class,
		OptimisationFileTest.class,
		TestIntermediaryResults.class })
public class AllTestSuite {

}
