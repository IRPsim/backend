package de.unileipzig.irpsim.server.suites;

import org.junit.experimental.categories.Categories;
import org.junit.experimental.categories.Categories.ExcludeCategory;
import org.junit.experimental.categories.Categories.IncludeCategory;
import org.junit.runner.RunWith;
import org.junit.runners.Suite.SuiteClasses;

import de.unileipzig.irpsim.server.endpoints.OptimisationParametersTest;
import de.unileipzig.irpsim.server.marker.GAMSTest;
import de.unileipzig.irpsim.server.marker.PerformanceTest;
import de.unileipzig.irpsim.server.marker.RESTTest;
import de.unileipzig.irpsim.server.optimisation.persistence.IRPoptPersistenceTest;

/**
 * @author reichelt
 */
@RunWith(Categories.class)
@IncludeCategory({ RESTTest.class })
@ExcludeCategory({ PerformanceTest.class, GAMSTest.class })
@SuiteClasses({
		IRPoptPersistenceTest.class,
		OptimisationParametersTest.class })
public class RESTSuite {
	// run all REST specific tests, exclude slow performance tests
}
