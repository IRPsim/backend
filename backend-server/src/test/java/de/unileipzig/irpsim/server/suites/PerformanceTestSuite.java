package de.unileipzig.irpsim.server.suites;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import de.unileipzig.irpsim.server.performance.GetLoadServerPerformanceTest;
import de.unileipzig.irpsim.server.performance.ParametersetReferencesCreationPerformanceTest;
import de.unileipzig.irpsim.server.performance.ThroughputTest;

/**
 * TestSuite aller Performance Tests mit dem KoPeMe framework.
 *
 * @author krauss
 */
@RunWith(Suite.class)
@SuiteClasses({
		GetLoadServerPerformanceTest.class,
		ParametersetReferencesCreationPerformanceTest.class,
		ThroughputTest.class })
public class PerformanceTestSuite {

}
